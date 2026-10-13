package com.findu.help.v2.infrastructure.adapter.out.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchNotificationRequestDto {
    private String type;
    private String recipient;
    private String templateCode;
    private String language;
    private Map<String, Object> params;
}
