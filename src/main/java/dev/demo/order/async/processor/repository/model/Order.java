package dev.demo.order.async.processor.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    @Column("id")
    private UUID id;

    @Column("reference_number")
    private String referenceNumber;

    @Column("type")
    private String type;

    @Column("status")
    private String status;

    @Column("customer_id")
    private UUID customerId;

    @Column("created_by")
    private String createdBy;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_by")
    private String updatedBy;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("priority")
    private Integer priority;

    @Column("due_date")
    private LocalDateTime dueDate;

    @Column("description")
    private String description;

    @Column("metadata")
    private String metadata;

    @Version
    private Long version;

    @Builder.Default
    private boolean deleted = false;
}