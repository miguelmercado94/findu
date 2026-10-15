package com.findu.validacion.domain.model;

import java.util.Collections;
import java.util.Map;

public record Evidence(Map<String, Object> data) {
    public Evidence {
        data = data != null ? Collections.unmodifiableMap(data) : Collections.emptyMap();
    }

    public static Evidence empty() {
        return new Evidence(Collections.emptyMap());
    }

    public static Evidence of(Map<String, Object> data) {
        return new Evidence(data);
    }
}
