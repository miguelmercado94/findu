package com.findu.help.v2.application.port.out.persistence;

import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.category.HelpCategory;

import java.util.List;
import java.util.Optional;

public interface HelpCategoryRepository {
    HelpCategory save(HelpCategory category);
    List<HelpCategory> findActiveByRole(UserRole role);
    List<HelpCategory> findAllActive();
    Optional<HelpCategory> findById(String id);
}
