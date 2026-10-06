package com.findu.transaction.application.port.in;

import com.findu.transaction.application.port.in.command.CreateSettlementCommand;
import com.findu.transaction.domain.model.settlement.Settlement;

public interface CreateSettlementUseCase {
    Settlement execute(CreateSettlementCommand command);
}
