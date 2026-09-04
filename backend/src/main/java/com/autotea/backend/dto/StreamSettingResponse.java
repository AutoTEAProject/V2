package com.autotea.backend.dto;

import com.autotea.backend.domain.ProjectStreamSetting;
import com.autotea.backend.domain.StreamDirection;

public record StreamSettingResponse(
        String streamName,
        StreamDirection direction,
        Double cost
) {
    public static StreamSettingResponse from(ProjectStreamSetting setting) {
        return new StreamSettingResponse(setting.getStreamName(), setting.getDirection(), setting.getCost());
    }
}
