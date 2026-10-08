package com.rapido.customer.service;

import com.rapido.customer.dto.CustomerRequest;
import com.rapido.customer.dto.CustomerResponse;

import java.util.List;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse getCustomerById(Long customerId);
    CustomerResponse updateCustomer(Long customerId, CustomerRequest request);
    void deleteCustomer(Long customerId);
    List<CustomerResponse> getAllCustomers();
    boolean checkCustomerExists(Long customerId);
}
