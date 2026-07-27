package com.findu.security.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("user_identity_provider")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityProviderEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("provider_name")
    private String providerName;

    @Column("provider_user_id")
    private String providerUserId;

    @Column("created_at")
    private LocalDateTime createdAt;
}
