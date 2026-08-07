package com.unibank.bankingSystem.controller;

import com.unibank.bankingSystem.dto.ComplianceFlagResponse;
import com.unibank.bankingSystem.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @GetMapping("/flags")
    public List<ComplianceFlagResponse> getFlags() {
        return complianceService.getFlags();
    }

    @PutMapping("/flags/{id}/review")
    public ComplianceFlagResponse reviewFlag(@PathVariable Long id) {
        return complianceService.reviewFlag(id);
    }
}
