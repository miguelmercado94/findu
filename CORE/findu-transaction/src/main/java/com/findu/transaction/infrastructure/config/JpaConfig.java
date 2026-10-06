package com.findu.transaction.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.findu.transaction.infrastructure.adapter.out.persistence.repository")
public class JpaConfig {
}
