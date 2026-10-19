package com.findu.help.v2.infrastructure.adapter.input.web.dto.response;

import com.findu.help.v2.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpCategoryResponse {
    private String id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private UserRole targetRole;
    private Integer order;
    private Boolean isActive;
    private List<HelpSubcategoryResponse> subcategories;
}
