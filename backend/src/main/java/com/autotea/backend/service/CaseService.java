package com.autotea.backend.service;

import com.autotea.backend.domain.TeaCase;
import com.autotea.backend.dto.CaseRequest;
import com.autotea.backend.exception.ResourceNotFoundException;
import com.autotea.backend.repository.TeaCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final TeaCaseRepository teaCaseRepository;

    public TeaCase create(CaseRequest request) {
        return teaCaseRepository.save(new TeaCase(request.name(), request.description()));
    }

    public List<TeaCase> findAll() {
        return teaCaseRepository.findAll();
    }

    public TeaCase getOrThrow(Long id) {
        return teaCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + id));
    }
}
