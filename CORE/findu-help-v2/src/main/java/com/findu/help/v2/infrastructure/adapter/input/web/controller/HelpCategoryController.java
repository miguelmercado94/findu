package com.findu.help.v2.infrastructure.adapter.input.web.controller;

import com.findu.help.v2.application.port.in.GetCategoriesUseCase;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.category.HelpCategory;
import com.findu.help.v2.infrastructure.adapter.input.web.dto.response.HelpCategoryResponse;
import com.findu.help.v2.infrastructure.adapter.input.web.mapper.HelpTicketWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/help/categories", "/api/v1/categories"})
@RequiredArgsConstructor
public class HelpCategoryController {

    private final GetCategoriesUseCase getCategoriesUseCase;
    private final HelpTicketWebMapper webMapper;

    @GetMapping
    public ResponseEntity<List<HelpCategoryResponse>> getCategories(
            @RequestParam(required = false) UserRole role) {
        log.info("Consultando categorías de soporte. Filtro role={}", role);
        List<HelpCategory> categories = getCategoriesUseCase.execute(role);
        return ResponseEntity.ok(webMapper.toCategoryResponseList(categories));
    }
}
