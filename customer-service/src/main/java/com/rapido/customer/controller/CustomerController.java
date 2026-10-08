package com.rapido.customer.controller;

import com.rapido.customer.dto.ApiResponse;
import com.rapido.customer.dto.CustomerRequest;
import com.rapido.customer.dto.CustomerResponse;
import com.rapido.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.createCustomer(request);
        return new ResponseEntity<>(
                new ApiResponse<>(true, "Customer registered successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        CustomerResponse response = service.getCustomerById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer profile retrieved", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.updateCustomer(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer profile updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.deleteCustomer(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer profile deleted", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAll() {
        List<CustomerResponse> response = service.getAllCustomers();
        return ResponseEntity.ok(new ApiResponse<>(true, "Customers listed successfully", response));
    }

    @GetMapping("/internal/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable Long id) {
        boolean exists = service.checkCustomerExists(id);
        return ResponseEntity.ok(exists);
    }
}
