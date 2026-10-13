package com.findu.help.v2.infrastructure.adapter.out.persistence.repository;

import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpCategoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataHelpCategoryRepository extends MongoRepository<HelpCategoryDocument, String> {
    List<HelpCategoryDocument> findByIsActiveTrue();
    List<HelpCategoryDocument> findByIsActiveTrueAndTargetRoleIn(List<UserRole> targetRoles);
}
