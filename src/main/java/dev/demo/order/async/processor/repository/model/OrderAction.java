package dev.demo.order.async.processor.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_actions")
public class OrderAction {

    @Id
    @Column("id")
    private UUID id;

    @Column("order_id")
    private UUID orderId;

    @Column("type")
    private String type;

    @Column("status")
    private String status;

    @Column("performed_by")
    private String performedBy;

    @Column("performed_at")
    private LocalDateTime performedAt;

    @Column("description")
    private String description;

    @Column("result")
    private String result;

    @Column("error_code")
    private String errorCode;

    @Column("error_message")
    private String errorMessage;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("duration_ms")
    private Long durationMs;

    @Column("source_ip")
    private String sourceIp;

    @Column("metadata")
    private String metadata;
}