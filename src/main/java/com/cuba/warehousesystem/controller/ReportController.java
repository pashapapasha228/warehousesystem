package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.ProductAlert;
import com.cuba.warehousesystem.dto.TurnoverReport;
import com.cuba.warehousesystem.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/turnover")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<TurnoverReport> getTurnoverReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(reportService.calculateTurnover(start, end));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STOREKEEPER')")
    public ResponseEntity<List<ProductAlert>> getLowStockAlerts() {
        return ResponseEntity.ok(reportService.findLowStockAlerts());
    }
}
