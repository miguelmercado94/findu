package com.findu.security.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityProvider {
    private Long id;
    private Long userId;
    private String providerName;
    private String providerUserId;
    private LocalDateTime createdAt;
}
