package com.autotea.backend.web;

import com.autotea.backend.domain.CalculationRun;
import com.autotea.backend.dto.RunResponse;
import com.autotea.backend.service.RunService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RunController {

    private final RunService runService;

    /** 1단계: input.xlsx/input.rep 업로드 + 장치 목록 파싱(계산은 아직 안 함). */
    @PostMapping(value = "/api/cases/{caseId}/runs/draft", consumes = "multipart/form-data")
    public RunResponse submitDraft(
            @PathVariable Long caseId,
            @RequestParam("xlsxFile") MultipartFile xlsxFile,
            @RequestParam("repFile") MultipartFile repFile
    ) {
        CalculationRun run = runService.submitDraft(caseId, xlsxFile, repFile);
        return RunResponse.from(run);
    }

    /** 2단계: 케이스에 저장된 장치비/utility 설정을 반영해 실제 계산을 실행. */
    @PostMapping("/api/cases/{caseId}/runs/{runId}/execute")
    public RunResponse execute(@PathVariable Long caseId, @PathVariable Long runId) {
        CalculationRun run = runService.execute(caseId, runId);
        return RunResponse.from(run);
    }

    @GetMapping("/api/cases/{caseId}/runs")
    public List<RunResponse> findByCase(@PathVariable Long caseId) {
        return runService.findByCaseId(caseId).stream().map(RunResponse::from).toList();
    }

    @GetMapping("/api/runs/{id}")
    public RunResponse findOne(@PathVariable Long id) {
        return RunResponse.from(runService.getOrThrow(id));
    }

    @GetMapping("/api/runs/{id}/result")
    public ResponseEntity<Resource> downloadResult(@PathVariable Long id) {
        CalculationRun run = runService.getOrThrow(id);
        Path resultFile = runService.resultFile(run);
        Resource resource = new FileSystemResource(resultFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"output.xlsx\"")
                .body(resource);
    }
}
