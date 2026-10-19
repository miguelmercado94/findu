package com.findu.help.v2.infrastructure.adapter.input.web.dto.response;

import com.findu.help.v2.domain.enums.PriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpSubcategoryResponse {
    private String subcategoryId;
    private String name;
    private String description;
    private Boolean requiresSolicitudId;
    private PriorityLevel basePriority;
    private Integer basePriorityScore;
    private Boolean isActive;
}
