package com.findu.validacion.application.port.in;

import com.findu.validacion.application.dto.response.ActiveAlertDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetActiveAlertsUseCase {
    Mono<List<ActiveAlertDto>> getActiveAlerts(String entityType, String entityId);
}
