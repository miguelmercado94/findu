package com.findu.core.infrastructure.repository;

import com.findu.core.infrastructure.entity.MunicipioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MunicipioRepository extends JpaRepository<MunicipioEntity, Long> {

    Optional<MunicipioEntity> findByCodigoDane(String codigoDane);

    List<MunicipioEntity> findAllByActiveTrue();
}
