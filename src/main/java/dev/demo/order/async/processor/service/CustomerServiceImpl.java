package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.CustomerRepository;
import dev.demo.order.async.processor.repository.model.Customer;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Observed(name = "customer.service.get", contextualName = "getCustomerById")
    public Mono<Customer> getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .doOnNext(customer -> log.debug("Retrieved customer: {}", customer.getId()))
                .doOnError(error -> log.error("Error retrieving customer {}: {}", id, error.getMessage()));
    }

    @Override
    @Observed(name = "customer.service.find.external", contextualName = "findCustomerByExternalId")
    public Mono<Customer> findCustomerByExternalId(String externalId) {
        return customerRepository.findByExternalIdAndDeletedFalse(externalId)
                .doOnNext(customer -> log.debug("Found customer by external ID {}: {}", externalId, customer.getId()))
                .doOnError(error -> log.error("Error finding customer by external ID {}: {}", externalId, error.getMessage()));
    }

    @Override
    @Observed(name = "customer.service.find.tax", contextualName = "findCustomerByTaxId")
    public Mono<Customer> findCustomerByTaxId(String taxId) {
        return customerRepository.findByTaxIdAndDeletedFalse(taxId)
                .doOnNext(customer -> log.debug("Found customer by tax ID {}: {}", taxId, customer.getId()))
                .doOnError(error -> log.error("Error finding customer by tax ID {}: {}", taxId, error.getMessage()));
    }

    @Override
    @Transactional
    @Observed(name = "customer.service.create", contextualName = "createCustomer")
    public Mono<Customer> createCustomer(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID());
        }

        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(LocalDateTime.now());
        }

        return customerRepository.save(customer)
                .doOnNext(savedCustomer -> log.info("Created new customer: {}", savedCustomer.getId()));
    }

    @Override
    @Transactional
    @Observed(name = "customer.service.update", contextualName = "updateCustomer")
    public Mono<Customer> updateCustomer(Customer customer) {
        return customerRepository.findById(customer.getId())
                .flatMap(existing -> {
                    customer.setUpdatedAt(LocalDateTime.now());
                    customer.setVersion(existing.getVersion() + 1);
                    return customerRepository.save(customer);
                })
                .doOnNext(updatedCustomer -> log.info("Updated customer: {}", updatedCustomer.getId()))
                .doOnError(error -> log.error("Error updating customer {}: {}", customer.getId(), error.getMessage()));
    }

    @Override
    @Transactional
    @Observed(name = "customer.service.delete", contextualName = "deleteCustomer")
    public Mono<Boolean> deleteCustomer(UUID id) {
        return customerRepository.softDeleteCustomer(id)
                .map(result -> result > 0)
                .doOnNext(success -> {
                    if (success) {
                        log.info("Deleted customer: {}", id);
                    } else {
                        log.warn("Failed to delete customer: {}", id);
                    }
                });
    }

    @Override
    @Observed(name = "customer.service.find.segment", contextualName = "findCustomersBySegment")
    public Flux<Customer> findCustomersBySegment(String segment) {
        return customerRepository.findBySegmentAndStatusAndDeletedFalse(segment, "ACTIVE")
                .doOnComplete(() -> log.debug("Found customers for segment: {}", segment));
    }
}