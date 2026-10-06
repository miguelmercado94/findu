package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetPayoutQuery;
import com.findu.transaction.domain.model.payout.Payout;

import java.util.List;
import java.util.Optional;

public interface GetPayoutUseCase {
    Optional<Payout> execute(GetPayoutQuery query);
    List<Payout> getPendingPayouts();
}
