package com.findu.transaction.application.port.out.security;

import java.util.Optional;
import java.util.Set;

public interface SecurityContextPort {
    Optional<Long> getCurrentUserId();
    Set<String> getCurrentUserRoles();
    boolean hasRole(String role);
}
