package dev.demo.order.async.processor.controller;

import dev.demo.order.async.processor.repository.model.OrderCommunication;
import dev.demo.order.async.processor.service.CommunicationService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API endpoints for communication management
 */
@RestController
@RequestMapping("/api/communications")
@RequiredArgsConstructor
@Slf4j
public class CommunicationController {

    private final CommunicationService communicationService;

    /**
     * Get a communication by ID
     */
    @GetMapping("/{id}")
    @Observed(name = "api.communication.get", contextualName = "apiGetCommunicationById")
    public Mono<ResponseEntity<OrderCommunication>> getCommunicationById(@PathVariable UUID id) {
        return communicationService.getCommunicationById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error("Error retrieving communication {}: {}", id, error.getMessage(), error));
    }

    /**
     * Get communications for an order
     */
    @GetMapping("/order/{orderId}")
    @Observed(name = "api.communication.find.by.order", contextualName = "apiFindCommunicationsByOrder")
    public Flux<OrderCommunication> getCommunicationsByOrder(@PathVariable UUID orderId) {
        return communicationService.findCommunicationsByOrder(orderId)
                .doOnComplete(() -> log.info("Retrieved communications for order: {}", orderId))
                .doOnError(error -> log.error("Error retrieving communications for order {}: {}",
                        orderId, error.getMessage(), error));
    }

    /**
     * Get communications for a customer
     */
    @GetMapping("/customer/{customerId}")
    @Observed(name = "api.communication.find.by.customer", contextualName = "apiFindCommunicationsByCustomer")
    public Flux<OrderCommunication> getCommunicationsByCustomer(@PathVariable UUID customerId) {
        return communicationService.findCommunicationsByCustomer(customerId)
                .doOnComplete(() -> log.info("Retrieved communications for customer: {}", customerId))
                .doOnError(error -> log.error("Error retrieving communications for customer {}: {}",
                        customerId, error.getMessage(), error));
    }

    /**
     * Create a new communication
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Observed(name = "api.communication.create", contextualName = "apiCreateCommunication")
    public Mono<OrderCommunication> createCommunication(@RequestBody OrderCommunication communication) {
        return communicationService.createCommunication(communication)
                .doOnSuccess(createdComm -> log.info("Created communication: {} for order: {}",
                        createdComm.getId(), createdComm.getOrderId()))
                .doOnError(error -> log.error("Error creating communication: {}", error.getMessage(), error));
    }

    /**
     * Update communication status
     */
    @PatchMapping("/{id}/status")
    @Observed(name = "api.communication.update.status", contextualName = "apiUpdateCommunicationStatus")
    public Mono<ResponseEntity<Object>> updateCommunicationStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentAt) {

        return communicationService.updateCommunicationStatus(id, status, sentAt)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok().build();
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                })
                .doOnSuccess(result -> log.info("Updated communication {} status to {}", id, status))
                .doOnError(error -> log.error("Error updating communication {} status: {}",
                        id, error.getMessage(), error));
    }

    /**
     * Process a communication manually
     */
    @PostMapping("/{id}/process")
    @Observed(name = "api.communication.process", contextualName = "apiProcessCommunication")
    public Mono<OrderCommunication> processCommunication(@PathVariable UUID id) {
        return communicationService.getCommunicationById(id)
                .flatMap(communicationService::processCommunication)
                .doOnSuccess(processedComm -> log.info("Manually processed communication: {}", id))
                .doOnError(error -> log.error("Error processing communication {}: {}", id, error.getMessage(), error));
    }

    /**
     * Mark communication as delivered
     */
    @PatchMapping("/{id}/delivered")
    @Observed(name = "api.communication.mark.delivered", contextualName = "apiMarkCommunicationDelivered")
    public Mono<ResponseEntity<Object>> markCommunicationDelivered(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deliveredAt) {

        LocalDateTime effectiveDeliveredTime = deliveredAt != null ? deliveredAt : LocalDateTime.now();

        return communicationService.markCommunicationDelivered(id, effectiveDeliveredTime)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok().build();
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                })
                .doOnSuccess(result -> log.info("Marked communication {} as delivered at {}", id, effectiveDeliveredTime))
                .doOnError(error -> log.error("Error marking communication {} as delivered: {}",
                        id, error.getMessage(), error));
    }
}