package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.*;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TurnoverReport> getTurnoverReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(reportService.calculateTurnover(start, end));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<List<ProductAlert>> getLowStockAlerts() {
        return ResponseEntity.ok(reportService.findLowStockAlerts());
    }

    @GetMapping("/stock-balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<StockBalanceReport> getStockBalanceReport() {
        return ResponseEntity.ok(reportService.getStockBalanceReport());
    }

    @GetMapping("/movement")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MovementReport> getMovementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(reportService.getMovementReport(start, end));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TopProductReport> getTopProductsReport(
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(reportService.getTopProductsReport(topN, start, end));
    }

    @GetMapping("/supplier-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SupplierStatsReport> getSupplierStatsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(reportService.getSupplierStatsReport(start, end));
    }

    // --- НОВЫЙ эндпоинт: ABC-анализ ---
    @GetMapping("/abc-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ABCAnalysisReport> getABCAnalysisReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodEnd) {
        return ResponseEntity.ok(reportService.getABCAnalysisReport(periodStart, periodEnd));
    }
    // ------------------------------------

    // --- НОВЫЙ эндпоинт: Загруженность ячеек ---
    @GetMapping("/cell-utilization")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<CellUtilizationReport.CellUtilizationItem>> getCellUtilizationReport() {
        return ResponseEntity.ok(reportService.getCellUtilizationReport());
    }
    // -------------------------------------------
}
