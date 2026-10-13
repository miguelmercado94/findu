package com.findu.help.v2.infrastructure.adapter.out.persistence;

import com.findu.help.v2.application.port.out.persistence.HelpCategoryRepository;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.category.HelpCategory;
import com.findu.help.v2.infrastructure.adapter.out.persistence.document.HelpCategoryDocument;
import com.findu.help.v2.infrastructure.adapter.out.persistence.mapper.HelpCategoryMongoMapper;
import com.findu.help.v2.infrastructure.adapter.out.persistence.repository.SpringDataHelpCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MongoHelpCategoryRepository implements HelpCategoryRepository {

    private final SpringDataHelpCategoryRepository springDataHelpCategoryRepository;
    private final HelpCategoryMongoMapper mapper;

    @Override
    public HelpCategory save(HelpCategory category) {
        HelpCategoryDocument doc = mapper.toDocument(category);
        HelpCategoryDocument saved = springDataHelpCategoryRepository.save(doc);
        return mapper.toDomain(saved);
    }

    @Override
    public List<HelpCategory> findActiveByRole(UserRole role) {
        List<UserRole> targetRoles = (role == null || role == UserRole.AMBOS)
                ? List.of(UserRole.CLIENTE, UserRole.PROVEEDOR, UserRole.AMBOS)
                : List.of(role, UserRole.AMBOS);
        List<HelpCategoryDocument> docs = springDataHelpCategoryRepository.findByIsActiveTrueAndTargetRoleIn(targetRoles);
        return mapper.toDomainList(docs);
    }

    @Override
    public List<HelpCategory> findAllActive() {
        List<HelpCategoryDocument> docs = springDataHelpCategoryRepository.findByIsActiveTrue();
        return mapper.toDomainList(docs);
    }

    @Override
    public Optional<HelpCategory> findById(String id) {
        return springDataHelpCategoryRepository.findById(id)
                .map(mapper::toDomain);
    }
}
