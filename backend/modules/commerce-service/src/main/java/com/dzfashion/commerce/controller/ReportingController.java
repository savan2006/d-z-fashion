package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.response.ManagerOperationalReportResponse;
import com.dzfashion.commerce.dto.response.OwnerAnalyticsReportResponse;
import com.dzfashion.commerce.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/operational")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ManagerOperationalReportResponse> getOperationalReport() {
        ManagerOperationalReportResponse response = reportingService.getManagerOperationalReport();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerAnalyticsReportResponse> getAnalyticsReport() {
        OwnerAnalyticsReportResponse response = reportingService.getOwnerAnalyticsReport();
        return ResponseEntity.ok(response);
    }
}
