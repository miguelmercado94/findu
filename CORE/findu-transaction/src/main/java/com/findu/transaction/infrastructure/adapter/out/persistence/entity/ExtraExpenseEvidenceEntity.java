package com.findu.transaction.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "extra_expense_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraExpenseEvidenceEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "extra_expense_request_id", length = 36, nullable = false)
    private String extraExpenseRequestId;

    @Column(name = "type", length = 32, nullable = false)
    private String type;

    @Column(name = "file_id", length = 128)
    private String fileId;

    @Column(name = "object_key", length = 255, nullable = false)
    private String objectKey;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "uploaded_by", length = 128)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
}
