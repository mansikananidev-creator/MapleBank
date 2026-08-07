package com.unibank.bankingSystem.repository;

import com.unibank.bankingSystem.model.Account;
import com.unibank.bankingSystem.model.Transaction;
import com.unibank.bankingSystem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountOrderByCreatedAtDesc(Account account, Pageable pageable);

    Page<Transaction> findByAccountOwnerOrderByCreatedAtDesc(User owner, Pageable pageable);

    List<Transaction> findByAccountOwnerAndCreatedAtAfter(User owner, LocalDateTime after);
}
