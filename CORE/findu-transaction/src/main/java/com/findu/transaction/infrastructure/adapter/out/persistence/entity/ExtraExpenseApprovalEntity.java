package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "extra_expense_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraExpenseApprovalEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "extra_expense_request_id", length = 36, nullable = false)
    private String extraExpenseRequestId;

    @Column(name = "approved_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal approvedAmount;

    @Column(name = "approved_by", length = 128, nullable = false)
    private String approvedBy;

    @Column(name = "approval_status", length = 32, nullable = false)
    private String approvalStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;
}
