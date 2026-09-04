package com.autotea.backend.service;

import com.autotea.backend.client.PythonEngineClient;
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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * backend와 python-engine은 디스크를 공유하지 않는다(배포 환경에서 서비스 간 볼륨 공유가 불가능하다는 전제).
 * 업로드된 input 파일과 계산 결과(xlsx, 장치별 계산 원가)는 모두 이 서비스가 DB에 들고 있다가
 * 매 요청마다 python-engine에 통째로 보내고, 응답으로 결과를 통째로 돌려받는다.
 *
 * 흐름: draft(업로드+장치 파싱) -> (프론트에서 장치비/utility 설정) -> execute(설정 반영 후 실제 계산)
 * execute는 같은 run에 대해 몇 번이든 다시 부를 수 있고(설정만 바꿔 재실행), 그때마다 새 run이 생기지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunService {

    private final CalculationRunRepository calculationRunRepository;
    private final CaseService caseService;
    private final EquipmentSettingService equipmentSettingService;
    private final StreamSettingService streamSettingService;
    private final UtilityPriceService utilityPriceService;
    private final PythonEngineClient pythonEngineClient;
    private final ObjectMapper objectMapper;

    public CalculationRun submitDraft(Long caseId, String name, MultipartFile xlsxFile, MultipartFile repFile) {
        TeaCase teaCase = caseService.getOrThrow(caseId);
        String resolvedName = (name == null || name.isBlank()) ? xlsxFile.getOriginalFilename() : name.trim();

        byte[] xlsxBytes;
        byte[] repBytes;
        try {
            xlsxBytes = xlsxFile.getBytes();
            repBytes = repFile.getBytes();
        } catch (IOException e) {
            CalculationRun failed = new CalculationRun(teaCase, resolvedName, xlsxFile.getOriginalFilename(), repFile.getOriginalFilename());
            failed.setStatus(RunStatus.FAILED);
            failed.setErrorMessage("입력 파일 읽기 실패: " + e.getMessage());
            return calculationRunRepository.save(failed);
        }

        CalculationRun run = new CalculationRun(teaCase, resolvedName, xlsxFile.getOriginalFilename(), repFile.getOriginalFilename());
        run.setInputXlsxData(xlsxBytes);
        run.setInputRepData(repBytes);
        run.setStatus(RunStatus.PARSING);
        run = calculationRunRepository.save(run);

        try {
            PythonEngineClient.ParseResponse response = pythonEngineClient.parse(xlsxBytes, repBytes);
            run.setLogs(response.logs());
            if (response.isSuccess()) {
                run.setEquipmentSnapshot(objectMapper.writeValueAsString(response.equipment()));
                run.setStreamSnapshot(objectMapper.writeValueAsString(response.streams()));
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
        TeaCase teaCase = caseService.getOrThrow(caseId);
        CalculationRun run = getOrThrow(runId);
        if (run.getEquipmentSnapshot() == null || run.getInputXlsxData() == null || run.getInputRepData() == null) {
            throw new IllegalStateException("이 run은 아직 장치 파싱이 끝나지 않았습니다. draft 단계를 먼저 완료하세요.");
        }

        String equipmentConfigJson;
        try {
            List<EquipmentInstanceResponse> instances = List.of(
                    objectMapper.readValue(run.getEquipmentSnapshot(), EquipmentInstanceResponse[].class));
            Map<String, Object> equipmentConfig = equipmentSettingService.buildEngineConfig(caseId, instances);
            equipmentConfig.put("streams", streamSettingService.buildStreamConfig(caseId));
            equipmentConfig.put("utilityPrices", utilityPriceService.buildConfig());
            equipmentConfig.put("plantParameters", Map.of(
                    "plantOperationHours", teaCase.getPlantOperationHours(),
                    "depreciationLifetime", teaCase.getDepreciationLifetime()
            ));
            equipmentConfigJson = objectMapper.writeValueAsString(equipmentConfig);
        } catch (JacksonException e) {
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage("장치 설정 직렬화 실패: " + e.getMessage());
            return calculationRunRepository.save(run);
        }

        run.setStatus(RunStatus.RUNNING);
        calculationRunRepository.save(run);

        try {
            PythonEngineClient.CalculateResponse response =
                    pythonEngineClient.calculate(run.getInputXlsxData(), run.getInputRepData(), equipmentConfigJson);
            run.setLogs(response.logs());
            if (response.isSuccess()) {
                run.setResultData(Base64.getDecoder().decode(response.outputXlsxBase64()));
                run.setCostResult(objectMapper.writeValueAsString(response.costResult()));
                run.setStatus(RunStatus.SUCCESS);
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

    public byte[] resultData(CalculationRun run) {
        if (run.getStatus() != RunStatus.SUCCESS || run.getResultData() == null) {
            throw new ResourceNotFoundException("결과 파일이 없습니다. run=" + run.getId());
        }
        return run.getResultData();
    }

    /**
     * 장치 이름 -> (수식 이름 -> 실제 계산된 EQUIPMENT COST[USD])를 돌려준다.
     * 장치비 설정 화면에서 "이 수식을 쓰면 이 장치는 실제로 얼마가 나온다"를 보여주기 위한 용도.
     */
    public Map<String, Map<String, Double>> equipmentCosts(CalculationRun run) {
        if (run.getStatus() != RunStatus.SUCCESS || run.getCostResult() == null) {
            throw new ResourceNotFoundException("계산이 완료된 run이 아닙니다. run=" + run.getId());
        }
        Map<String, Map<String, Map<String, Object>>> raw;
        try {
            raw = objectMapper.readValue(run.getCostResult(),
                    new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {
                    });
        } catch (JacksonException e) {
            throw new IllegalStateException("장치비 계산 결과 읽기 실패: " + e.getMessage(), e);
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
}
