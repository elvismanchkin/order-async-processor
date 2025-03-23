package dev.demo.order.async.processor;

import dev.demo.order.async.processor.repository.OrderDocumentRepository;
import dev.demo.order.async.processor.repository.model.OrderDocument;
import dev.demo.order.async.processor.service.DocumentService;
import dev.demo.order.async.processor.service.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class DocumentServiceIntegrationTest {

    @Mock
    private OrderDocumentRepository documentRepository;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        documentService = new DocumentServiceImpl(documentRepository);

        // Set the necessary fields using reflection
        org.springframework.test.util.ReflectionTestUtils.setField(documentService, "pendingStatus", "PENDING");
        org.springframework.test.util.ReflectionTestUtils.setField(documentService, "processingStatus", "PROCESSING");
        org.springframework.test.util.ReflectionTestUtils.setField(documentService, "completedStatus", "COMPLETED");
        org.springframework.test.util.ReflectionTestUtils.setField(documentService, "errorStatus", "ERROR");
    }

    @Test
    void findDocumentsForProcessing_ShouldReturnMatchingDocuments() {
        // Arrange
        OrderDocument doc1 = new OrderDocument();
        doc1.setId(UUID.randomUUID());
        doc1.setOrderId(UUID.randomUUID());
        doc1.setType("INVOICE");
        doc1.setStatus("PENDING");

        OrderDocument doc2 = new OrderDocument();
        doc2.setId(UUID.randomUUID());
        doc2.setOrderId(UUID.randomUUID());
        doc2.setType("CONTRACT");
        doc2.setStatus("PENDING");

        List<String> types = Arrays.asList("INVOICE", "CONTRACT");
        List<String> statuses = List.of("PENDING");

        when(documentRepository.findDocumentsForProcessing(eq(types), eq(statuses), anyInt()))
                .thenReturn(Flux.just(doc1, doc2));

        // Act & Assert
        StepVerifier.create(documentService.findDocumentsForProcessing(types, statuses, 10))
                .expectNext(doc1)
                .expectNext(doc2)
                .verifyComplete();
    }

    @Test
    void findDocumentsExpiringBetween_ShouldReturnMatchingDocuments() {
        // Arrange
        OrderDocument doc1 = new OrderDocument();
        doc1.setId(UUID.randomUUID());
        doc1.setOrderId(UUID.randomUUID());
        doc1.setType("CONTRACT");
        doc1.setExpiryDate(LocalDate.now().plusDays(15));

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);

        when(documentRepository.findByExpiryDateBetween(eq(startDate), eq(endDate)))
                .thenReturn(Flux.just(doc1));

        // Act & Assert
        StepVerifier.create(documentService.findDocumentsExpiringBetween(startDate, endDate))
                .expectNext(doc1)
                .verifyComplete();
    }

    @Test
    void processDocument_ShouldUpdateStatusAndReturnProcessedDocument() {
        // Arrange
        UUID docId = UUID.randomUUID();

        OrderDocument document = new OrderDocument();
        document.setId(docId);
        document.setOrderId(UUID.randomUUID());
        document.setType("INVOICE");
        document.setStatus("PENDING");
        document.setAmount(new BigDecimal("100.00"));

        OrderDocument processingDocument = new OrderDocument();
        processingDocument.setId(docId);
        processingDocument.setOrderId(document.getOrderId());
        processingDocument.setType("INVOICE");
        processingDocument.setStatus("PROCESSING");
        processingDocument.setAmount(new BigDecimal("100.00"));

        OrderDocument completedDocument = new OrderDocument();
        completedDocument.setId(docId);
        completedDocument.setOrderId(document.getOrderId());
        completedDocument.setType("INVOICE");
        completedDocument.setStatus("COMPLETED");
        completedDocument.setAmount(new BigDecimal("100.00"));

        // Mock the status updates
        when(documentRepository.updateDocumentStatus(eq(docId), eq("PROCESSING")))
                .thenReturn(Mono.just(1));
        when(documentRepository.updateDocumentStatus(eq(docId), eq("COMPLETED")))
                .thenReturn(Mono.just(1));

        // Mock the findById sequence with a clearer pattern
        when(documentRepository.findById(docId))
                .thenReturn(Mono.just(processingDocument))  // First call returns PROCESSING state
                .thenReturn(Mono.just(completedDocument));  // Second call returns COMPLETED state

        // Act & Assert
        StepVerifier.create(documentService.processDocument(document))
                .expectNextMatches(doc -> "COMPLETED".equals(doc.getStatus()))
                .verifyComplete();
    }

    @Test
    void createDocument_ShouldSetDefaultValuesAndSave() {
        // Arrange
        OrderDocument document = new OrderDocument();
        document.setOrderId(UUID.randomUUID());
        document.setType("RECEIPT");
        document.setName("Test Receipt");
        document.setUploadedBy("test-user");

        when(documentRepository.save(any(OrderDocument.class)))
                .thenAnswer(invocation -> {
                    OrderDocument savedDoc = invocation.getArgument(0);
                    savedDoc.setId(UUID.randomUUID()); // Simulate DB id generation
                    return Mono.just(savedDoc);
                });

        // Act & Assert
        StepVerifier.create(documentService.createDocument(document))
                .expectNextMatches(doc ->
                        doc.getId() != null &&
                                doc.getUploadedAt() != null &&
                                doc.getStatus() != null &&
                                "PENDING".equals(doc.getStatus()))
                .verifyComplete();
    }
}