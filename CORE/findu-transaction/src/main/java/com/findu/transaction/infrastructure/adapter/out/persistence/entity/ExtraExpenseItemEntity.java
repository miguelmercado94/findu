package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "extra_expense_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraExpenseItemEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "extra_expense_request_id", length = 36, nullable = false)
    private String extraExpenseRequestId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal unitAmount;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;
}
