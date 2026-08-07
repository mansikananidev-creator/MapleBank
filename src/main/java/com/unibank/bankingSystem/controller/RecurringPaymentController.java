package com.unibank.bankingSystem.controller;

import com.unibank.bankingSystem.dto.RecurringPaymentRequest;
import com.unibank.bankingSystem.dto.RecurringPaymentResponse;
import com.unibank.bankingSystem.service.RecurringPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-payments")
@RequiredArgsConstructor
public class RecurringPaymentController {

    private final RecurringPaymentService recurringPaymentService;

    @PostMapping
    public RecurringPaymentResponse create(@RequestBody RecurringPaymentRequest request) {
        return recurringPaymentService.createRecurringPayment(request);
    }

    @GetMapping
    public List<RecurringPaymentResponse> getAll() {
        return recurringPaymentService.getUserRecurringPayments();
    }

    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Long id) {
        recurringPaymentService.cancelRecurringPayment(id);
    }
}
