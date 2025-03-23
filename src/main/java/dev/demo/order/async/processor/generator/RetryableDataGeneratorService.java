package dev.demo.order.async.processor.generator;

import dev.demo.order.async.processor.repository.CustomerRepository;
import dev.demo.order.async.processor.repository.model.Customer;
import dev.demo.order.async.processor.repository.model.Order;
import dev.demo.order.async.processor.repository.model.OrderCommunication;
import dev.demo.order.async.processor.repository.model.OrderDocument;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryableDataGeneratorService {

    private final DataGeneratorService dataGeneratorService;
    private final MeterRegistry meterRegistry;
    private final CustomerRepository customerRepository;

    private final Retry customerRetry = Retry.of("customerCreation", RetryConfig.custom()
            .maxAttempts(5)
            .waitDuration(Duration.ofMillis(50))
            .retryOnException(e -> e instanceof OptimisticLockingFailureException)
            .build());

    public Mono<Void> generateBatchData() {
        log.info("Generating batch with retry logic");
        return Flux.range(0, dataGeneratorService.getBatchSize())
                .flatMap(i -> generateCompleteDataSetWithRetry())
                .then();
    }

    private Mono<Void> generateCompleteDataSetWithRetry() {
        // Generate a completely new customer each time instead of updating existing
        return Mono.defer(() -> {
            Customer customer = createNewCustomer();
            return customerRepository.save(customer)
                    .transform(RetryOperator.of(customerRetry))
                    .doOnError(OptimisticLockingFailureException.class, e ->
                            log.warn("Customer creation failed after retries: {}", e.getMessage()))
                    .onErrorResume(OptimisticLockingFailureException.class, e -> Mono.empty())
                    .flatMap(dataGeneratorService::generateOrder)
                    .flatMap(order -> {
                        Flux<OrderDocument> documents = generateDocuments(order);
                        Flux<OrderCommunication> communications =
                                generateCommunications(order, customer);

                        return Flux.merge(documents, communications).then(Mono.just(order));
                    })
                    .then();
        });
    }

    private Customer createNewCustomer() {
        Customer customer = new Customer();
        customer.setExternalId("EXT-" + dataGeneratorService.getFaker().number().numberBetween(1000, 9999));
        customer.setTaxId(dataGeneratorService.getFaker().number().digits(10));
        customer.setName(dataGeneratorService.getFaker().company().name());
        customer.setEmail(dataGeneratorService.getFaker().internet().emailAddress());
        customer.setPhone(dataGeneratorService.getFaker().phoneNumber().phoneNumber());
        customer.setSegment(this.getRandomItem(Arrays.asList("REGULAR", "VIP", "ENTERPRISE", "GOVERNMENT")));
        customer.setCreatedAt(LocalDateTime.now());
        customer.setStatus("ACTIVE");
        customer.setAccountManager(dataGeneratorService.getFaker().name().fullName());
        customer.setDeleted(false);
        return customer;
    }

    public <T> T getRandomItem(List<T> items) {
        return items.get(ThreadLocalRandom.current().nextInt(items.size()));
    }

    private Flux<OrderDocument> generateDocuments(Order order) {
        int count = 1 + (int)(Math.random() * 3);
        return Flux.range(0, count)
                .flatMap(i -> dataGeneratorService.generateDocument(order));
    }

    private Flux<OrderCommunication> generateCommunications(Order order, Customer customer) {
        int count = 1 + (int)(Math.random() * 2);
        return Flux.range(0, count)
                .flatMap(i -> dataGeneratorService.generateCommunication(order, customer));
    }
}