package com.findu.help.v2.domain.model.category;

import com.findu.help.v2.domain.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class HelpCategory {
    private String id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private UserRole targetRole;
    private Integer order;
    private Boolean isActive;
    private final List<HelpSubcategory> subcategories = new ArrayList<>();

    @Builder
    public HelpCategory(String id, String name, String code, String description, String icon, UserRole targetRole, Integer order, Boolean isActive, List<HelpSubcategory> subcategories) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de la categoría es obligatorio");
        }
        this.id = id;
        this.name = name.trim();
        this.code = code.trim();
        this.description = description != null ? description.trim() : "";
        this.icon = icon != null ? icon.trim() : "";
        this.targetRole = targetRole != null ? targetRole : UserRole.AMBOS;
        this.order = order != null ? order : 0;
        this.isActive = isActive != null ? isActive : true;
        if (subcategories != null) {
            this.subcategories.addAll(subcategories);
        }
    }

    public List<HelpSubcategory> getSubcategories() {
        return Collections.unmodifiableList(subcategories);
    }
}
