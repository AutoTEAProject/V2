package com.autotea.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** 프로젝트(케이스)가 원료(IN)/제품(OUT)으로 지정한 stream 하나. IN만 cost를 쓴다($/kg). */
@Entity
@Table(name = "project_stream_setting")
@Getter
@Setter
@NoArgsConstructor
public class ProjectStreamSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private TeaCase teaCase;

    @Column(name = "stream_name", nullable = false, length = 200)
    private String streamName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StreamDirection direction;

    /** USD/kg. direction=IN일 때만 의미 있음. */
    private Double cost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ProjectStreamSetting(TeaCase teaCase, String streamName, StreamDirection direction) {
        this.teaCase = teaCase;
        this.streamName = streamName;
        this.direction = direction;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
