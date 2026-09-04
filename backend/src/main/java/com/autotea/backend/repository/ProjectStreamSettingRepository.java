package com.autotea.backend.repository;

import com.autotea.backend.domain.ProjectStreamSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectStreamSettingRepository extends JpaRepository<ProjectStreamSetting, Long> {
    List<ProjectStreamSetting> findAllByTeaCaseId(Long caseId);

    Optional<ProjectStreamSetting> findByTeaCaseIdAndStreamName(Long caseId, String streamName);

    void deleteByTeaCaseIdAndStreamName(Long caseId, String streamName);
}
