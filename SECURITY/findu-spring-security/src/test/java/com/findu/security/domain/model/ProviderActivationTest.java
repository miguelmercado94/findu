package com.findu.security.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderActivationTest {

    @Test
    void validacionesPreviasCompletas_allThree_true() {
        ProviderActivation a = new ProviderActivation();
        a.setOtpOk(true);
        a.setEmailOk(true);
        a.setBiometriaOk(true);
        assertTrue(a.validacionesPreviasCompletas());
    }

    @Test
    void validacionesPreviasCompletas_missingOne_false() {
        ProviderActivation a = new ProviderActivation();
        a.setOtpOk(true);
        a.setEmailOk(true);
        a.setBiometriaOk(false);
        assertFalse(a.validacionesPreviasCompletas());
    }
}
