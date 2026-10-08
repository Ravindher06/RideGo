package com.rapido.customer.service;

import com.rapido.customer.dto.CustomerRequest;
import com.rapido.customer.dto.CustomerResponse;
import com.rapido.customer.entity.Customer;
import com.rapido.customer.exception.CustomerNotFoundException;
import com.rapido.customer.exception.DuplicateResourceException;
import com.rapido.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;

    public CustomerServiceImpl(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (repository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Customer with phone number " + request.getPhone() + " already exists");
        }
        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer with email " + request.getEmail() + " already exists");
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setWalletBalance(request.getWalletBalance() != null ? request.getWalletBalance() : BigDecimal.ZERO);

        Customer saved = repository.save(customer);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long customerId) {
        Customer customer = repository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID " + customerId));
        return mapToResponse(customer);
    }

    @Override
    public CustomerResponse updateCustomer(Long customerId, CustomerRequest request) {
        Customer customer = repository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID " + customerId));

        repository.findByPhone(request.getPhone()).ifPresent(existing -> {
            if (!existing.getCustomerId().equals(customerId)) {
                throw new DuplicateResourceException("Phone number " + request.getPhone() + " is already taken");
            }
        });

        repository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getCustomerId().equals(customerId)) {
                throw new DuplicateResourceException("Email " + request.getEmail() + " is already taken");
            }
        });

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        if (request.getWalletBalance() != null) {
            customer.setWalletBalance(request.getWalletBalance());
        }

        Customer updated = repository.save(customer);
        return mapToResponse(updated);
    }

    @Override
    public void deleteCustomer(Long customerId) {
        if (!repository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found with ID " + customerId);
        }
        repository.deleteById(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkCustomerExists(Long customerId) {
        return repository.existsById(customerId);
    }

    private CustomerResponse mapToResponse(Customer entity) {
        CustomerResponse response = new CustomerResponse();
        response.setCustomerId(entity.getCustomerId());
        response.setName(entity.getName());
        response.setPhone(entity.getPhone());
        response.setEmail(entity.getEmail());
        response.setWalletBalance(entity.getWalletBalance());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
