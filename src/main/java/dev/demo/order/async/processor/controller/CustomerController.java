package dev.demo.order.async.processor.controller;

import dev.demo.order.async.processor.repository.model.Customer;
import dev.demo.order.async.processor.service.CustomerService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * API endpoints for customer management
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/all")
    @Observed(name = "api.customer.find.all", contextualName = "apiFindAllCustomers")
    public Flux<Customer> findAllCustomers() {
        return customerService.findAllCustomers();
    }

    /**
     * Get a customer by ID
     */
    @GetMapping("/{id}")
    @Observed(name = "api.customer.get", contextualName = "apiGetCustomerById")
    public Mono<ResponseEntity<Customer>> getCustomerById(@PathVariable UUID id) {
        return customerService
                .getCustomerById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error("Error retrieving customer {}: {}", id, error.getMessage(), error));
    }

    /**
     * Find customer by tax ID
     */
    @GetMapping("/tax/{taxId}")
    @Observed(name = "api.customer.find.by.tax", contextualName = "apiFindCustomerByTaxId")
    public Mono<ResponseEntity<Customer>> findCustomerByTaxId(@PathVariable String taxId) {
        return customerService
                .findCustomerByTaxId(taxId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error ->
                        log.error("Error finding customer by tax ID {}: {}", taxId, error.getMessage(), error));
    }

    /**
     * Find customer by external ID
     */
    @GetMapping("/external/{externalId}")
    @Observed(name = "api.customer.find.by.external", contextualName = "apiFindCustomerByExternalId")
    public Mono<ResponseEntity<Customer>> findCustomerByExternalId(@PathVariable String externalId) {
        return customerService
                .findCustomerByExternalId(externalId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error(
                        "Error finding customer by external ID {}: {}", externalId, error.getMessage(), error));
    }

    /**
     * Create a new customer
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Observed(name = "api.customer.create", contextualName = "apiCreateCustomer")
    public Mono<Customer> createCustomer(@RequestBody Customer customer) {
        return customerService
                .createCustomer(customer)
                .doOnSuccess(createdCustomer -> log.info("Created customer: {}", createdCustomer.getId()))
                .doOnError(error -> log.error("Error creating customer: {}", error.getMessage(), error));
    }

    /**
     * Update an existing customer
     */
    @PutMapping("/{id}")
    @Observed(name = "api.customer.update", contextualName = "apiUpdateCustomer")
    public Mono<Customer> updateCustomer(@PathVariable UUID id, @RequestBody Customer customer) {
        if (!id.equals(customer.getId())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID in path must match ID in body"));
        }

        return customerService
                .updateCustomer(customer)
                .doOnSuccess(updatedCustomer -> log.info("Updated customer: {}", updatedCustomer.getId()))
                .doOnError(error -> log.error("Error updating customer {}: {}", id, error.getMessage(), error));
    }

    /**
     * Delete a customer
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Observed(name = "api.customer.delete", contextualName = "apiDeleteCustomer")
    public Mono<Object> deleteCustomer(@PathVariable UUID id) {
        return customerService
                .deleteCustomer(id)
                .flatMap(success -> {
                    if (success) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
                    }
                })
                .doOnSuccess(v -> log.info("Deleted customer: {}", id))
                .doOnError(error -> log.error("Error deleting customer {}: {}", id, error.getMessage(), error));
    }

    /**
     * Find customers by segment
     */
    @GetMapping("/segment/{segment}")
    @Observed(name = "api.customer.find.by.segment", contextualName = "apiFindCustomersBySegment")
    public Flux<Customer> findCustomersBySegment(@PathVariable String segment) {
        return customerService
                .findCustomersBySegment(segment)
                .doOnComplete(() -> log.info("Retrieved customers for segment: {}", segment))
                .doOnError(error ->
                        log.error("Error retrieving customers for segment {}: {}", segment, error.getMessage(), error));
    }
}