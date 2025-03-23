package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerService {

    /**
     * Get customer by ID
     *
     * @param id Customer ID
     * @return Customer if found
     */
    Mono<Customer> getCustomerById(UUID id);

    /**
     * Find customer by external ID
     *
     * @param externalId External system ID
     * @return Customer if found
     */
    Mono<Customer> findCustomerByExternalId(String externalId);

    /**
     * Find customer by tax ID
     *
     * @param taxId Tax identification number
     * @return Customer if found
     */
    Mono<Customer> findCustomerByTaxId(String taxId);

    /**
     * Create new customer
     *
     * @param customer Customer to create
     * @return Created customer
     */
    Mono<Customer> createCustomer(Customer customer);

    /**
     * Update customer
     *
     * @param customer Customer with updates
     * @return Updated customer
     */
    Mono<Customer> updateCustomer(Customer customer);

    /**
     * Delete customer
     *
     * @param id Customer ID
     * @return Success indicator
     */
    Mono<Boolean> deleteCustomer(UUID id);

    /**
     * Find customers by segment
     *
     * @param segment Customer segment
     * @return Customers in the segment
     */
    Flux<Customer> findCustomersBySegment(String segment);
}