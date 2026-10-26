package com.findu.transaction.application.port.in.finance;

import com.findu.transaction.infrastructure.adapter.input.web.dto.response.ServiceFinancialDetailResponse;

public interface GetServiceFinancialDetailUseCase {
    ServiceFinancialDetailResponse getServiceFinancialDetail(Long providerId, Long serviceRequestId);
}
