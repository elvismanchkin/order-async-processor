package dev.demo.order.async.processor;

import dev.demo.order.async.processor.repository.OrderDocumentRepository;
import dev.demo.order.async.processor.repository.model.OrderDocument;
import dev.demo.order.async.processor.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentServiceIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @MockitoBean
    private OrderDocumentRepository documentRepository;

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
        List<String> statuses = Arrays.asList("PENDING");

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

        OrderDocument processedDocument = new OrderDocument();
        processedDocument.setId(docId);
        processedDocument.setOrderId(document.getOrderId());
        processedDocument.setType("INVOICE");
        processedDocument.setStatus("COMPLETED");
        processedDocument.setAmount(new BigDecimal("100.00"));

        when(documentRepository.updateDocumentStatus(eq(docId), anyString()))
                .thenReturn(Mono.just(1));

        when(documentRepository.findById(docId))
                .thenReturn(Mono.just(document))
                .thenReturn(Mono.just(processedDocument));

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
                                doc.getStatus() != null)
                .verifyComplete();
    }
}