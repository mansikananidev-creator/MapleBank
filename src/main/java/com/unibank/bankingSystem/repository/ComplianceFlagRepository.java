package com.unibank.bankingSystem.repository;

import com.unibank.bankingSystem.model.ComplianceFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceFlagRepository extends JpaRepository<ComplianceFlag, Long> {

    List<ComplianceFlag> findAllByOrderByCreatedAtDesc();
}
