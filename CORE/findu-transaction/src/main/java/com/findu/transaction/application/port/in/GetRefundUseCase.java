package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.query.GetRefundQuery;
import com.findu.transaction.domain.model.refund.Refund;

import java.util.List;
import java.util.Optional;

public interface GetRefundUseCase {
    Optional<Refund> execute(GetRefundQuery query);
    List<Refund> getPendingRefunds();
}
