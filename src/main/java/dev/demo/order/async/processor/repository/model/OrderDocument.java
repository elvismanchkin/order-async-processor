package dev.demo.order.async.processor.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_documents")
public class OrderDocument {

    @Id
    @Column("id")
    private UUID id;

    @Column("order_id")
    private UUID orderId;

    @Column("type")
    private String type;

    @Column("name")
    private String name;

    @Column("number")
    private String number;

    @Column("issue_date")
    private LocalDate issueDate;

    @Column("expiry_date")
    private LocalDate expiryDate;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("status")
    private String status;

    @Column("storage_path")
    private String storagePath;

    @Column("storage_id")
    private String storageId;

    @Column("mime_type")
    private String mimeType;

    @Column("size_bytes")
    private Long sizeBytes;

    @Column("uploaded_by")
    private String uploadedBy;

    @Column("uploaded_at")
    private LocalDateTime uploadedAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("description")
    private String description;

    @Column("metadata")
    private String metadata;
}