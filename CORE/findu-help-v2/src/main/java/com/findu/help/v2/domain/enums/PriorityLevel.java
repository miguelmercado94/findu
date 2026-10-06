package com.findu.help.v2.domain.enums;

import lombok.Getter;

@Getter
public enum PriorityLevel {
    CRITICA(100),
    ALTA(80),
    MEDIA(60),
    NORMAL(40),
    BAJA(20);

    private final int defaultScore;

    PriorityLevel(int defaultScore) {
        this.defaultScore = defaultScore;
    }
}
