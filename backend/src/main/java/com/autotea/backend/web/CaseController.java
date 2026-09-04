package com.autotea.backend.web;

import com.autotea.backend.dto.CaseRequest;
import com.autotea.backend.dto.CaseResponse;
import com.autotea.backend.dto.PlantParameterRequest;
import com.autotea.backend.service.CaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    public ResponseEntity<CaseResponse> create(@Valid @RequestBody CaseRequest request) {
        CaseResponse response = CaseResponse.from(caseService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<CaseResponse> findAll() {
        return caseService.findAll().stream().map(CaseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CaseResponse findOne(@PathVariable Long id) {
        return CaseResponse.from(caseService.getOrThrow(id));
    }

    @PutMapping("/{id}/plant-parameters")
    public CaseResponse updatePlantParameters(@PathVariable Long id, @Valid @RequestBody PlantParameterRequest request) {
        return CaseResponse.from(caseService.updatePlantParameters(id, request));
    }
}
