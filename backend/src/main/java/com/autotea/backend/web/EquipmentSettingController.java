package com.autotea.backend.web;

import com.autotea.backend.dto.EquipmentSettingRequest;
import com.autotea.backend.dto.EquipmentSettingResponse;
import com.autotea.backend.service.EquipmentSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cases/{caseId}/equipment-settings")
@RequiredArgsConstructor
public class EquipmentSettingController {

    private final EquipmentSettingService equipmentSettingService;

    @GetMapping
    public List<EquipmentSettingResponse> findAll(@PathVariable Long caseId) {
        return equipmentSettingService.findAllForCase(caseId);
    }

    @PutMapping
    public List<EquipmentSettingResponse> upsert(@PathVariable Long caseId, @Valid @RequestBody EquipmentSettingRequest request) {
        equipmentSettingService.upsert(caseId, request);
        return equipmentSettingService.findAllForCase(caseId);
    }
}
