package com.autotea.backend.service;

import com.autotea.backend.client.PythonEngineClient;
import com.autotea.backend.config.StorageProperties;
import com.autotea.backend.domain.CalculationRun;
import com.autotea.backend.domain.RunStatus;
import com.autotea.backend.domain.TeaCase;
import com.autotea.backend.dto.EquipmentInstanceResponse;
import com.autotea.backend.exception.ResourceNotFoundException;
import com.autotea.backend.repository.CalculationRunRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * runId 기준으로 {app.storage.run-dir}/{runId}/input/ 아래 input.xlsx, input.rep를
 * 배치해두면, python-engine이 같은 공유 볼륨의 같은 경로에서 이를 읽는 구조를 전제로 한다.
 *
 * 흐름: draft(업로드+장치 파싱) -> (프론트에서 장치비/utility 설정) -> execute(설정 반영 후 실제 계산)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunService {

    private final CalculationRunRepository calculationRunRepository;
    private final CaseService caseService;
    private final EquipmentSettingService equipmentSettingService;
    private final StorageProperties storageProperties;
    private final PythonEngineClient pythonEngineClient;
    private final ObjectMapper objectMapper;

    public CalculationRun submitDraft(Long caseId, String name, MultipartFile xlsxFile, MultipartFile repFile) {
        TeaCase teaCase = caseService.getOrThrow(caseId);

        String resolvedName = (name == null || name.isBlank()) ? xlsxFile.getOriginalFilename() : name.trim();
        CalculationRun run = calculationRunRepository.save(
                new CalculationRun(teaCase, resolvedName, xlsxFile.getOriginalFilename(), repFile.getOriginalFilename()));

        Path inputDir = runDir(run.getId()).resolve("input");
        try {
            Files.createDirectories(inputDir);
            xlsxFile.transferTo(inputDir.resolve("input.xlsx"));
            repFile.transferTo(inputDir.resolve("input.rep"));
        } catch (IOException e) {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage("입력 파일 저장 실패: " + e.getMessage());
            return calculationRunRepository.save(run);
        }

        run.setStatus(RunStatus.PARSING);
        calculationRunRepository.save(run);

        try {
            PythonEngineClient.ParseResponse response = pythonEngineClient.parse(String.valueOf(run.getId()));
            run.setLogs(response.logs());
            if (response.isSuccess()) {
                run.setEquipmentSnapshot(objectMapper.writeValueAsString(response.equipment()));
                run.setStatus(RunStatus.PARSED);
            } else {
                run.setStatus(RunStatus.FAILED);
                run.setErrorMessage(response.errorMessage());
            }
        } catch (Exception e) {
            log.error("Parsing failed for run {}", run.getId(), e);
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
        }

        return calculationRunRepository.save(run);
    }

    public CalculationRun execute(Long caseId, Long runId) {
        caseService.getOrThrow(caseId);
        CalculationRun run = getOrThrow(runId);
        if (run.getEquipmentSnapshot() == null) {
            throw new IllegalStateException("이 run은 아직 장치 파싱이 끝나지 않았습니다. draft 단계를 먼저 완료하세요.");
        }

        Path runDir = runDir(run.getId());
        Path inputDir = runDir.resolve("input");
        try {
            List<EquipmentInstanceResponse> instances = List.of(
                    objectMapper.readValue(run.getEquipmentSnapshot(), EquipmentInstanceResponse[].class));
            var equipmentConfig = equipmentSettingService.buildEngineConfig(caseId, instances);
            Files.writeString(inputDir.resolve("equipment_config.json"), objectMapper.writeValueAsString(equipmentConfig));
        } catch (JacksonException e) {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage("장치 설정 직렬화 실패: " + e.getMessage());
            return calculationRunRepository.save(run);
        } catch (IOException e) {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage("장치 설정 파일 저장 실패: " + e.getMessage());
            return calculationRunRepository.save(run);
        }

        run.setStatus(RunStatus.RUNNING);
        calculationRunRepository.save(run);

        try {
            PythonEngineClient.CalculateResponse response = pythonEngineClient.calculate(String.valueOf(run.getId()));
            run.setLogs(response.logs());
            if (response.isSuccess()) {
                run.setStatus(RunStatus.SUCCESS);
                run.setResultPath(runDir.resolve("output.xlsx").toString());
                run.setErrorMessage(null);
            } else {
                run.setStatus(RunStatus.FAILED);
                run.setErrorMessage(response.errorMessage());
            }
        } catch (Exception e) {
            log.error("Calculation failed for run {}", run.getId(), e);
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage(e.getMessage());
        }

        return calculationRunRepository.save(run);
    }

    public CalculationRun getOrThrow(Long id) {
        return calculationRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Run not found: " + id));
    }

    public List<CalculationRun> findByCaseId(Long caseId) {
        return calculationRunRepository.findByTeaCaseIdOrderByCreatedAtDesc(caseId);
    }

    public Path resultFile(CalculationRun run) {
        if (run.getStatus() != RunStatus.SUCCESS || run.getResultPath() == null) {
            throw new ResourceNotFoundException("결과 파일이 없습니다. run=" + run.getId());
        }
        return Path.of(run.getResultPath());
    }

    /**
     * 장치 이름 -> (수식 이름 -> 실제 계산된 EQUIPMENT COST[USD])를 돌려준다.
     * 장치비 설정 화면에서 "이 수식을 쓰면 이 장치는 실제로 얼마가 나온다"를 보여주기 위한 용도.
     */
    public Map<String, Map<String, Double>> equipmentCosts(CalculationRun run) {
        if (run.getStatus() != RunStatus.SUCCESS) {
            throw new ResourceNotFoundException("계산이 완료된 run이 아닙니다. run=" + run.getId());
        }
        Path file = runDir(run.getId()).resolve("cost_result.json");
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("장치비 계산 결과 파일이 없습니다. run=" + run.getId());
        }
        Map<String, Map<String, Map<String, Object>>> raw;
        try {
            raw = objectMapper.readValue(file.toFile(),
                    new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {
                    });
        } catch (JacksonException e) {
            throw new IllegalStateException("장치비 계산 결과 파일 읽기 실패: " + e.getMessage(), e);
        }

        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        raw.forEach((equipmentName, byFormula) -> {
            Map<String, Double> costs = new LinkedHashMap<>();
            byFormula.forEach((formulaName, fields) -> {
                if (fields.get("EQUIPMENT COST") instanceof Number number) {
                    costs.put(formulaName, number.doubleValue());
                }
            });
            result.put(equipmentName, costs);
        });
        return result;
    }

    private Path runDir(Long runId) {
        return Path.of(storageProperties.runDir(), String.valueOf(runId));
    }
}
