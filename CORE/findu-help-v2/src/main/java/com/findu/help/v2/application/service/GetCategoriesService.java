package com.findu.help.v2.application.service;

import com.findu.help.v2.application.port.in.GetCategoriesUseCase;
import com.findu.help.v2.application.port.out.persistence.HelpCategoryRepository;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.category.HelpCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCategoriesService implements GetCategoriesUseCase {

    private final HelpCategoryRepository helpCategoryRepository;

    @Override
    public List<HelpCategory> execute(UserRole targetRole) {
        if (targetRole != null) {
            return helpCategoryRepository.findActiveByRole(targetRole);
        }
        return helpCategoryRepository.findAllActive();
    }
}
