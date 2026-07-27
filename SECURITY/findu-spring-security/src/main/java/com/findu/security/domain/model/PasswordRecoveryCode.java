package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordRecoveryCode {
    private Long id;
    private Long userId;
    private String code;
    private Long emittedAt;
    private Long expiresAt;
    private boolean used;
}
