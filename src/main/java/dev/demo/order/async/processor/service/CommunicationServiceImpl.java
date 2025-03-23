package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.OrderCommunicationRepository;
import dev.demo.order.async.processor.repository.model.OrderCommunication;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationServiceImpl implements CommunicationService {

    private final OrderCommunicationRepository communicationRepository;

    @Value("${communication.processing.status.pending:PENDING}")
    private String pendingStatus;

    @Value("${communication.processing.status.sending:SENDING}")
    private String sendingStatus;

    @Value("${communication.processing.status.sent:SENT}")
    private String sentStatus;

    @Value("${communication.processing.status.delivered:DELIVERED}")
    private String deliveredStatus;

    @Value("${communication.processing.status.error:ERROR}")
    private String errorStatus;

    @Override
    @Observed(name = "communication.service.get", contextualName = "getCommunicationById")
    public Mono<OrderCommunication> getCommunicationById(UUID id) {
        return communicationRepository.findById(id)
                .doOnNext(communication -> log.debug("Retrieved communication: {}", communication.getId()))
                .doOnError(error -> log.error("Error retrieving communication {}: {}", id, error.getMessage(), error));
    }

    @Override
    @Observed(name = "communication.service.find.by.order", contextualName = "findCommunicationsByOrder")
    public Flux<OrderCommunication> findCommunicationsByOrder(UUID orderId) {
        return communicationRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .doOnComplete(() -> log.debug("Found communications for order: {}", orderId));
    }

    @Override
    @Observed(name = "communication.service.find.by.customer", contextualName = "findCommunicationsByCustomer")
    public Flux<OrderCommunication> findCommunicationsByCustomer(UUID customerId) {
        return communicationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .doOnComplete(() -> log.debug("Found communications for customer: {}", customerId));
    }

    @Override
    @Transactional
    @Observed(name = "communication.service.create", contextualName = "createCommunication")
    public Mono<OrderCommunication> createCommunication(OrderCommunication communication) {
        if (communication.getId() == null) {
            communication.setId(UUID.randomUUID());
        }

        if (communication.getCreatedAt() == null) {
            communication.setCreatedAt(LocalDateTime.now());
        }

        if (communication.getStatus() == null) {
            communication.setStatus(pendingStatus);
        }

        return communicationRepository.save(communication)
                .doOnNext(savedCommunication -> log.info("Created new communication: {} for order: {}",
                        savedCommunication.getId(), savedCommunication.getOrderId()));
    }

    @Override
    @Transactional
    @Observed(name = "communication.service.update.status", contextualName = "updateCommunicationStatus")
    public Mono<Boolean> updateCommunicationStatus(UUID id, String status, LocalDateTime sentAt) {
        return communicationRepository.updateCommunicationStatus(id, status, sentAt)
                .map(result -> result > 0)
                .doOnNext(success -> {
                    if (success) {
                        log.info("Updated communication {} status to {}", id, status);
                    } else {
                        log.warn("Failed to update communication {} status", id);
                    }
                });
    }

    @Override
    @Observed(name = "communication.service.find.unsent", contextualName = "findUnsentCommunications")
    public Flux<OrderCommunication> findUnsentCommunications(List<String> statuses, int batchSize) {
        return communicationRepository.findUnsentCommunications(statuses, batchSize)
                .doOnNext(communication -> log.debug("Found unsent communication: {}", communication.getId()))
                .doOnComplete(() -> log.debug("Found unsent communications, statuses: {}", statuses));
    }

    @Override
    @Transactional
    @Observed(name = "communication.service.process", contextualName = "processCommunication")
    public Mono<OrderCommunication> processCommunication(OrderCommunication communication) {
        log.info("Processing communication: {}", communication.getId());

        // First update status to sending
        LocalDateTime now = LocalDateTime.now();
        return updateCommunicationStatus(communication.getId(), sendingStatus, null)
                .flatMap(success -> {
                    if (!success) {
                        return Mono.error(new RuntimeException("Failed to update communication status to sending"));
                    }

                    // Retrieve the latest communication
                    return communicationRepository.findById(communication.getId());
                })
                .flatMap(processedCommunication -> {
                    // Simulated communication sending logic
                    log.info("Sending communication: {}", processedCommunication.getId());

                    // Update status to sent after processing
                    return updateCommunicationStatus(processedCommunication.getId(), sentStatus, now)
                            .flatMap(success -> {
                                if (!success) {
                                    return Mono.error(new RuntimeException("Failed to update communication status to sent"));
                                }
                                return communicationRepository.findById(processedCommunication.getId());
                            });
                })
                .onErrorResume(error -> {
                    log.error("Error processing communication {}: {}", communication.getId(), error.getMessage(), error);
                    return updateCommunicationStatus(communication.getId(), errorStatus, null)
                            .then(communicationRepository.findById(communication.getId()));
                });
    }

    @Override
    @Transactional
    @Observed(name = "communication.service.mark.delivered", contextualName = "markCommunicationDelivered")
    public Mono<Boolean> markCommunicationDelivered(UUID id, LocalDateTime deliveredAt) {
        // Custom query implementation for setting delivered status
        return getCommunicationById(id)
                .flatMap(comm -> {
                    comm.setStatus(deliveredStatus);
                    comm.setDeliveredAt(deliveredAt);
                    comm.setUpdatedAt(LocalDateTime.now());
                    return communicationRepository.save(comm);
                })
                .map(result -> true)
                .onErrorReturn(false)
                .doOnNext(success -> {
                    if (success) {
                        log.info("Marked communication {} as delivered at {}", id, deliveredAt);
                    } else {
                        log.warn("Failed to mark communication {} as delivered", id);
                    }
                });
    }
}