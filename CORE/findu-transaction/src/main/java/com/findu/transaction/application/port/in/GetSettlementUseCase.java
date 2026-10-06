package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetSettlementQuery;
import com.findu.transaction.domain.model.settlement.Settlement;

import java.util.Optional;

public interface GetSettlementUseCase {
    Optional<Settlement> execute(GetSettlementQuery query);
}
