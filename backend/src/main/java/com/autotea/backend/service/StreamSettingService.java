package com.autotea.backend.service;

import com.autotea.backend.domain.ProjectStreamSetting;
import com.autotea.backend.domain.StreamDirection;
import com.autotea.backend.domain.TeaCase;
import com.autotea.backend.dto.StreamSettingItem;
import com.autotea.backend.dto.StreamSettingResponse;
import com.autotea.backend.repository.ProjectStreamSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 프로젝트별로 어떤 stream을 원료(IN)/제품(OUT)으로 쓸지 설정. 예전엔 전역 MaterialData.xlsx에 고정되어 있었다. */
@Service
@RequiredArgsConstructor
public class StreamSettingService {

    private final ProjectStreamSettingRepository streamSettingRepository;
    private final CaseService caseService;

    public List<StreamSettingResponse> findAllForCase(Long caseId) {
        caseService.getOrThrow(caseId);
        return streamSettingRepository.findAllByTeaCaseId(caseId).stream()
                .map(StreamSettingResponse::from)
                .toList();
    }

    /** 케이스의 stream 설정을 통째로 새 목록으로 교체한다(체크 해제된 stream은 자연히 사라짐). */
    @Transactional
    public void replaceAll(Long caseId, List<StreamSettingItem> items) {
        TeaCase teaCase = caseService.getOrThrow(caseId);
        streamSettingRepository.deleteAll(streamSettingRepository.findAllByTeaCaseId(caseId));
        for (StreamSettingItem item : items) {
            ProjectStreamSetting setting = new ProjectStreamSetting(teaCase, item.streamName(), item.direction());
            setting.setCost(item.cost());
            streamSettingRepository.save(setting);
        }
    }

    /** run 실행 시 python-engine에 넘길 equipment_config.json의 "streams" 값. */
    public Map<String, Object> buildStreamConfig(Long caseId) {
        Map<String, Object> input = new LinkedHashMap<>();
        List<String> output = new java.util.ArrayList<>();

        for (ProjectStreamSetting setting : streamSettingRepository.findAllByTeaCaseId(caseId)) {
            if (setting.getDirection() == StreamDirection.IN) {
                input.put(setting.getStreamName(), Map.of("cost", setting.getCost() == null ? 0.0 : setting.getCost()));
            } else {
                output.add(setting.getStreamName());
            }
        }

        Map<String, Object> streams = new LinkedHashMap<>();
        streams.put("input", input);
        streams.put("output", output);
        return streams;
    }
}
