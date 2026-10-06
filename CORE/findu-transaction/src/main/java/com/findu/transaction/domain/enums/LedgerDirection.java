package com.findu.transaction.domain.enums;

public enum LedgerDirection {
    CREDIT, // Aumenta lo que FindU debe al proveedor
    DEBIT   // Aumenta lo que el proveedor debe a FindU (o disminuye el saldo a favor)
}
