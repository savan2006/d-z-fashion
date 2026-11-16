package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.response.FinancialSummaryReportResponse;
import com.dzfashion.payment.service.FinancialReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/financial-reports")
public class FinancialReportController {

    private final FinancialReportingService financialReportingService;

    public FinancialReportController(FinancialReportingService financialReportingService) {
        this.financialReportingService = financialReportingService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<FinancialSummaryReportResponse> getFinancialSummaryReport() {
        FinancialSummaryReportResponse response = financialReportingService.getFinancialSummaryReport();
        return ResponseEntity.ok(response);
    }
}
