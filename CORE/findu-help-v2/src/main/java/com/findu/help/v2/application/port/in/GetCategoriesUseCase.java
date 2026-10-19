package com.findu.help.v2.application.port.in;

import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.category.HelpCategory;

import java.util.List;

public interface GetCategoriesUseCase {
    List<HelpCategory> execute(UserRole targetRole);
}
