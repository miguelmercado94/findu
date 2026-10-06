package com.findu.help.v2.domain.model.category;

import com.findu.help.v2.domain.enums.PriorityLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class HelpSubcategory {
    private final String subcategoryId;
    private final String name;
    private final String description;
    private final Boolean requiresSolicitudId;
    private final PriorityLevel basePriority;
    private final Integer basePriorityScore;
    private final Boolean isActive;

    public HelpSubcategory(String subcategoryId, String name, String description, Boolean requiresSolicitudId, PriorityLevel basePriority, Integer basePriorityScore, Boolean isActive) {
        if (subcategoryId == null || subcategoryId.isBlank()) {
            throw new IllegalArgumentException("El subcategoryId es obligatorio");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la subcategoría es obligatorio");
        }
        this.subcategoryId = subcategoryId.trim();
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.requiresSolicitudId = requiresSolicitudId != null ? requiresSolicitudId : false;
        this.basePriority = basePriority != null ? basePriority : PriorityLevel.MEDIA;
        this.basePriorityScore = basePriorityScore != null ? basePriorityScore : this.basePriority.getDefaultScore();
        this.isActive = isActive != null ? isActive : true;
    }
}
