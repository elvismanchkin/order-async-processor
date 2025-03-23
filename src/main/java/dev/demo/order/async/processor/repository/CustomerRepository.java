package dev.demo.order.async.processor.repository;

import dev.demo.order.async.processor.repository.model.Customer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CustomerRepository extends R2dbcRepository<Customer, UUID> {

    /**
     * Find customer by tax ID
     *
     * @param taxId Tax identification number
     * @return Customer if found
     */
    Mono<Customer> findByTaxIdAndDeletedFalse(String taxId);

    /**
     * Find customer by external ID
     *
     * @param externalId External system identifier
     * @return Customer if found
     */
    Mono<Customer> findByExternalIdAndDeletedFalse(String externalId);

    /**
     * Find active customers by segment
     *
     * @param segment Customer segment
     * @param status Customer status
     * @return Active customers in the specified segment
     */
    Flux<Customer> findBySegmentAndStatusAndDeletedFalse(String segment, String status);

    /**
     * Count customers by segment
     *
     * @param segment Customer segment
     * @return Count of customers in the segment
     */
    @Query("SELECT COUNT(*) FROM customers WHERE segment = :segment AND deleted = false")
    Mono<Long> countBySegment(String segment);

    /**
     * Soft delete a customer
     *
     * @param id Customer ID
     * @return Number of rows affected
     */
    @Query("UPDATE customers SET deleted = true WHERE id = :id")
    Mono<Integer> softDeleteCustomer(UUID id);
}