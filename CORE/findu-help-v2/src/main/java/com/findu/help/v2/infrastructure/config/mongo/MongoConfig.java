package com.findu.help.v2.infrastructure.config.mongo;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.findu.help.v2.infrastructure.adapter.out.persistence.repository")
public class MongoConfig {
}
