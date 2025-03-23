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
@Table("order_communications")
public class OrderCommunication {

    @Id
    @Column("id")
    private UUID id;

    @Column("order_id")
    private UUID orderId;

    @Column("customer_id")
    private UUID customerId;

    @Column("channel")
    private String channel;

    @Column("type")
    private String type;

    @Column("direction")
    private String direction;

    @Column("sender")
    private String sender;

    @Column("recipient")
    private String recipient;

    @Column("subject")
    private String subject;

    @Column("message")
    private String message;

    @Column("status")
    private String status;

    @Column("sent_at")
    private LocalDateTime sentAt;

    @Column("delivered_at")
    private LocalDateTime deliveredAt;

    @Column("created_by")
    private String createdBy;

    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("error_message")
    private String errorMessage;

    @Column("external_reference")
    private String externalReference;

    @Column("metadata")
    private String metadata;
}