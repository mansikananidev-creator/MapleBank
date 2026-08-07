package com.unibank.bankingSystem.repository;

import com.unibank.bankingSystem.model.RecurringPayment;
import com.unibank.bankingSystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {

    List<RecurringPayment> findByFromAccount_Owner(User owner);

    List<RecurringPayment> findByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
}
