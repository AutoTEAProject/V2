package com.autotea.backend.web;

import com.autotea.backend.dto.StreamSettingRequest;
import com.autotea.backend.dto.StreamSettingResponse;
import com.autotea.backend.service.StreamSettingService;
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
@RequestMapping("/api/cases/{caseId}/stream-settings")
@RequiredArgsConstructor
public class StreamSettingController {

    private final StreamSettingService streamSettingService;

    @GetMapping
    public List<StreamSettingResponse> findAll(@PathVariable Long caseId) {
        return streamSettingService.findAllForCase(caseId);
    }

    @PutMapping
    public List<StreamSettingResponse> replaceAll(@PathVariable Long caseId, @Valid @RequestBody StreamSettingRequest request) {
        streamSettingService.replaceAll(caseId, request.items() == null ? List.of() : request.items());
        return streamSettingService.findAllForCase(caseId);
    }
}
