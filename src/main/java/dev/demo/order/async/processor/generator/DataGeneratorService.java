package dev.demo.order.async.processor.generator;

import com.github.javafaker.Faker;
import dev.demo.order.async.processor.repository.CustomerRepository;
import dev.demo.order.async.processor.repository.OrderCommunicationRepository;
import dev.demo.order.async.processor.repository.OrderDocumentRepository;
import dev.demo.order.async.processor.repository.OrderRepository;
import dev.demo.order.async.processor.repository.model.Customer;
import dev.demo.order.async.processor.repository.model.Order;
import dev.demo.order.async.processor.repository.model.OrderCommunication;
import dev.demo.order.async.processor.repository.model.OrderDocument;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Service for generating fake data for development and testing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataGeneratorService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderDocumentRepository documentRepository;
    private final OrderCommunicationRepository communicationRepository;
    private final MeterRegistry meterRegistry;

    private final Faker faker = new Faker();
    private final Random random = new Random();

    @Value("${data.generator.batch-size:5}")
    private int batchSize;

    private static final List<String> ORDER_TYPES = Arrays.asList("STANDARD", "PRIORITY", "URGENT", "RETURN");
    private static final List<String> ORDER_STATUSES = Arrays.asList("PENDING", "PROCESSING", "COMPLETED", "ERROR");
    private static final List<String> CUSTOMER_SEGMENTS = Arrays.asList("REGULAR", "VIP", "ENTERPRISE", "GOVERNMENT");
    private static final List<String> DOCUMENT_TYPES = Arrays.asList("INVOICE", "CONTRACT", "RECEIPT", "REPORT", "CERTIFICATE");
    private static final List<String> COMMUNICATION_CHANNELS = Arrays.asList("EMAIL", "SMS", "PUSH", "LETTER");
    private static final List<String> COMMUNICATION_TYPES = Arrays.asList("NOTIFICATION", "REMINDER", "CONFIRMATION", "MARKETING");

    /**
     * Generate a random customer
     */
    public Mono<Customer> generateCustomer() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setExternalId("EXT-" + faker.number().numberBetween(1000, 9999));
        customer.setTaxId(faker.number().digits(10));
        customer.setName(faker.company().name());
        customer.setEmail(faker.internet().emailAddress());
        customer.setPhone(faker.phoneNumber().phoneNumber());
        customer.setSegment(getRandomItem(CUSTOMER_SEGMENTS));
        customer.setCreatedAt(LocalDateTime.now());
        customer.setStatus("ACTIVE");
        customer.setAccountManager(faker.name().fullName());
        customer.setVersion(0L);
        customer.setDeleted(false);

        log.debug("Generated customer: {}", customer.getName());
        meterRegistry.counter("data.generator.customer").increment();

        return customerRepository.save(customer);
    }

    /**
     * Generate a random order for a customer
     */
    public Mono<Order> generateOrder(Customer customer) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setReferenceNumber("ORD-" + faker.number().digits(5));
        order.setType(getRandomItem(ORDER_TYPES));
        order.setStatus(getRandomWeightedOrderStatus());
        order.setCustomerId(customer.getId());
        order.setCreatedBy(faker.name().username());
        order.setCreatedAt(LocalDateTime.now().minusHours(random.nextInt(48)));
        order.setPriority(random.nextInt(20));
        order.setDueDate(LocalDateTime.now().plusDays(random.nextInt(14) + 1));
        order.setDescription(faker.lorem().paragraph());
        order.setVersion(0L);
        order.setDeleted(false);

        log.debug("Generated order: {} for customer: {}", order.getReferenceNumber(), customer.getName());
        meterRegistry.counter("data.generator.order").increment();

        return orderRepository.save(order);
    }

    /**
     * Generate a random document for an order
     */
    public Mono<OrderDocument> generateDocument(Order order) {
        OrderDocument document = new OrderDocument();
        document.setId(UUID.randomUUID());
        document.setOrderId(order.getId());
        document.setType(getRandomItem(DOCUMENT_TYPES));
        document.setName(faker.file().fileName());
        document.setNumber(faker.idNumber().validSvSeSsn());
        document.setIssueDate(LocalDate.now().minusDays(random.nextInt(30)));
        document.setExpiryDate(LocalDate.now().plusDays(random.nextInt(365) + 30));
        document.setAmount(BigDecimal.valueOf(random.nextDouble() * 10000));
        document.setCurrency("USD");
        document.setStatus(getRandomItem(Arrays.asList("PENDING", "VERIFIED", "REJECTED")));
        document.setStoragePath("/storage/documents/" + document.getId());
        document.setStorageId(document.getId().toString());
        document.setMimeType("application/pdf");
        document.setSizeBytes((long)random.nextInt(1000000) + 1000);
        document.setUploadedBy(faker.name().username());
        document.setUploadedAt(LocalDateTime.now().minusHours(random.nextInt(24)));
        document.setDescription(faker.lorem().paragraph(1));

        log.debug("Generated document: {} for order: {}", document.getName(), order.getReferenceNumber());
        meterRegistry.counter("data.generator.document").increment();

        return documentRepository.save(document);
    }

    /**
     * Generate a random communication for an order and customer
     */
    public Mono<OrderCommunication> generateCommunication(Order order, Customer customer) {
        OrderCommunication communication = new OrderCommunication();
        communication.setId(UUID.randomUUID());
        communication.setOrderId(order.getId());
        communication.setCustomerId(customer.getId());
        communication.setChannel(getRandomItem(COMMUNICATION_CHANNELS));
        communication.setType(getRandomItem(COMMUNICATION_TYPES));
        communication.setDirection(getRandomItem(Arrays.asList("OUTBOUND", "INBOUND")));
        communication.setSender("system@example.com");
        communication.setRecipient(customer.getEmail());
        communication.setSubject(faker.commerce().productName());
        communication.setMessage(faker.lorem().paragraph(3));
        communication.setStatus(getRandomItem(Arrays.asList("PENDING", "SENT", "DELIVERED", "FAILED")));

        if ("SENT".equals(communication.getStatus()) || "DELIVERED".equals(communication.getStatus())) {
            communication.setSentAt(LocalDateTime.now().minusMinutes(random.nextInt(60)));

            if ("DELIVERED".equals(communication.getStatus())) {
                communication.setDeliveredAt(communication.getSentAt().plusMinutes(random.nextInt(10)));
            }
        }

        communication.setCreatedBy(faker.name().username());
        communication.setCreatedAt(LocalDateTime.now().minusHours(random.nextInt(24)));
        communication.setExternalReference(UUID.randomUUID().toString());

        log.debug("Generated communication for order: {}", order.getReferenceNumber());
        meterRegistry.counter("data.generator.communication").increment();

        return communicationRepository.save(communication);
    }

    /**
     * Generate a complete set of data: customer, order, documents, communications
     */
    public Mono<Void> generateCompleteDataSet() {
        return generateCustomer()
                .flatMap(customer ->
                        generateOrder(customer)
                                .flatMap(order -> {
                                    // Generate 1-3 documents
                                    int docCount = random.nextInt(3) + 1;
                                    Flux<OrderDocument> documents = Flux.range(0, docCount)
                                            .flatMap(i -> generateDocument(order));

                                    // Generate 1-2 communications
                                    int commCount = random.nextInt(2) + 1;
                                    Flux<OrderCommunication> communications = Flux.range(0, commCount)
                                            .flatMap(i -> generateCommunication(order, customer));

                                    return Flux.merge(documents, communications).then(Mono.just(order));
                                })
                )
                .then();
    }

    /**
     * Generate multiple data sets
     */
    public Mono<Void> generateBatchData() {
        log.info("Generating batch of {} data sets", batchSize);

        return Flux.range(0, batchSize)
                .flatMap(i -> generateCompleteDataSet())
                .then();
    }

    /**
     * Get a random item from a list
     */
    private <T> T getRandomItem(List<T> items) {
        return items.get(random.nextInt(items.size()));
    }

    /**
     * Get a random order status with weighting (more PENDING and COMPLETED, fewer PROCESSING and ERROR)
     */
    private String getRandomWeightedOrderStatus() {
        int rand = random.nextInt(100);
        if (rand < 40) {
            return "PENDING";
        } else if (rand < 55) {
            return "PROCESSING";
        } else if (rand < 95) {
            return "COMPLETED";
        } else {
            return "ERROR";
        }
    }
}