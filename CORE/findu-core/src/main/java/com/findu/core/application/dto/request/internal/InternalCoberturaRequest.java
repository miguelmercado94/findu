package com.findu.core.application.dto.request.internal;

import java.util.List;

public record InternalCoberturaRequest(
        List<Long> municipioIds
) {
}
