package com.autotea.backend.web;

import com.autotea.backend.domain.EquipmentType;
import com.autotea.backend.dto.FormulaTemplateRequest;
import com.autotea.backend.dto.FormulaTemplateResponse;
import com.autotea.backend.service.FormulaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 장치비 수식 전역 라이브러리: 프로젝트에 관계없이 공유되며 자유롭게 추가/수정/삭제 가능. */
@RestController
@RequestMapping("/api/formulas")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaService formulaService;

    @GetMapping
    public List<FormulaTemplateResponse> findAll(@RequestParam(required = false) EquipmentType equipmentType) {
        return formulaService.findAll(equipmentType).stream().map(FormulaTemplateResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<FormulaTemplateResponse> create(@Valid @RequestBody FormulaTemplateRequest request) {
        FormulaTemplateResponse response = FormulaTemplateResponse.from(formulaService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public FormulaTemplateResponse update(@PathVariable Long id, @Valid @RequestBody FormulaTemplateRequest request) {
        return FormulaTemplateResponse.from(formulaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        formulaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
