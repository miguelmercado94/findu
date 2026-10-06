package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.CreateRefundCommand;
import com.findu.transaction.domain.enums.TransferSimulationOutcome;
import com.findu.transaction.domain.model.refund.Refund;

import java.util.List;

public interface ManageRefundUseCase {
    Refund createRefund(CreateRefundCommand command);
    Refund executeRefund(String refundId, TransferSimulationOutcome outcome);
    List<Refund> getPendingRefunds();
}
