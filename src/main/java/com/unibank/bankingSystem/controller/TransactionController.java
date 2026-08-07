package com.unibank.bankingSystem.controller;

import com.unibank.bankingSystem.dto.EmailTransferRequest;
import com.unibank.bankingSystem.dto.MonthlySummaryResponse;
import com.unibank.bankingSystem.dto.TransactionRequest;
import com.unibank.bankingSystem.dto.TransactionResponse;
import com.unibank.bankingSystem.dto.TransferRequest;
import com.unibank.bankingSystem.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public TransactionResponse deposit(@RequestBody TransactionRequest request) {
        return transactionService.deposit(request);
    }

    @PostMapping("/withdraw")
    public TransactionResponse withdraw(@RequestBody TransactionRequest request) {
        return transactionService.withdraw(request);
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(@RequestBody TransferRequest request) {
        return transactionService.transfer(request);
    }

    @PostMapping("/transfer/email")
    public TransactionResponse transferByEmail(@RequestBody EmailTransferRequest request) {
        return transactionService.transferByEmail(request);
    }

    @GetMapping("/account/{accountId}")
    public Page<TransactionResponse> getHistory(@PathVariable Long accountId, Pageable pageable) {
        return transactionService.getTransactionHistory(accountId, pageable);
    }

    @GetMapping("/recent")
    public List<TransactionResponse> getRecent() {
        return transactionService.getRecentTransactions();
    }

    @GetMapping("/summary")
    public List<MonthlySummaryResponse> getSummary() {
        return transactionService.getMonthlySummary();
    }
}
