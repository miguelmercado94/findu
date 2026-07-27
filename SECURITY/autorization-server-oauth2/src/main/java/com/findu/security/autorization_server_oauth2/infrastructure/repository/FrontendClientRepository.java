package com.findu.security.autorization_server_oauth2.infrastructure.repository;

import com.findu.security.autorization_server_oauth2.infrastructure.entity.FrontendClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrontendClientRepository extends JpaRepository<FrontendClient, String> {
    Optional<FrontendClient> findByClientId(String clientId);
}
