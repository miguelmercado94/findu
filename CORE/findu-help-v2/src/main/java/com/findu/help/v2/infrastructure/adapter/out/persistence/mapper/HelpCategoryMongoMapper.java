package com.findu.help.v2.infrastructure.adapter.out.persistence.mapper;

import com.findu.help.v2.domain.model.category.HelpCategory;
import com.findu.help.v2.domain.model.category.HelpSubcategory;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpCategoryDocument;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpSubcategoryDocument;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HelpCategoryMongoMapper {

    HelpCategoryDocument toDocument(HelpCategory domain);

    HelpCategory toDomain(HelpCategoryDocument document);

    HelpSubcategoryDocument toSubcategoryDocument(HelpSubcategory domain);

    HelpSubcategory toSubcategoryDomain(HelpSubcategoryDocument document);

    List<HelpCategoryDocument> toDocumentList(List<HelpCategory> domainList);

    List<HelpCategory> toDomainList(List<HelpCategoryDocument> documentList);
}
