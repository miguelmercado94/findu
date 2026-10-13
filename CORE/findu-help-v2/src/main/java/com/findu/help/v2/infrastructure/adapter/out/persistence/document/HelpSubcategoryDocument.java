package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

import com.findu.help.v2.domain.enums.PriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpSubcategoryDocument {
    private String subcategoryId;
    private String name;
    private String description;
    private Boolean requiresSolicitudId;
    private PriorityLevel basePriority;
    private Integer basePriorityScore;
    private Boolean isActive;
}
