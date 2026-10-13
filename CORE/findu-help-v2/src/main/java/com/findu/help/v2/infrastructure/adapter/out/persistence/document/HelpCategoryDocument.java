package com.findu.help.v2.infrastructure.adapter.out.persistence.document;

import com.findu.help.v2.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "help_categories")
public class HelpCategoryDocument {

    @Id
    private String id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private UserRole targetRole;
    private Integer order;
    private Boolean isActive;
    private List<HelpSubcategoryDocument> subcategories;
}
