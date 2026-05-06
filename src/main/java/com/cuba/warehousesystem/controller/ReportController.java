package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.ABCAnalysisReport;
import com.cuba.warehousesystem.dto.AuditReport;
import com.cuba.warehousesystem.dto.CellUtilizationReport;
import com.cuba.warehousesystem.dto.DashboardReport;
import com.cuba.warehousesystem.dto.EdiStatisticsReport;
import com.cuba.warehousesystem.dto.MovementReport;
import com.cuba.warehousesystem.dto.ProductAlert;
import com.cuba.warehousesystem.dto.StockBalanceReport;
import com.cuba.warehousesystem.dto.SupplierStatsReport;
import com.cuba.warehousesystem.dto.TopProductReport;
import com.cuba.warehousesystem.dto.TurnoverReport;
import com.cuba.warehousesystem.service.ReportExportService;
import com.cuba.warehousesystem.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    @GetMapping("/turnover")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TurnoverReport> getTurnoverReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(reportService.calculateTurnover(start, end));
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(reportService.getMovementReport(start, end));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TopProductReport> getTopProductsReport(
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(reportService.getTopProductsReport(topN, start, end));
    }

    @GetMapping("/supplier-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SupplierStatsReport> getSupplierStatsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(reportService.getSupplierStatsReport(start, end));
    }

    @GetMapping("/cell-utilization")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CellUtilizationReport> getCellUtilizationReport() {
        return ResponseEntity.ok(reportService.getCellUtilizationReport());
    }

    @GetMapping("/abc-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ABCAnalysisReport> getABCAnalysisReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(reportService.getABCAnalysisReport(start, end));
    }

    @GetMapping("/edi-statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EdiStatisticsReport> getEdiStatisticsReport() {
        return ResponseEntity.ok(reportService.getEdiStatisticsReport());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AuditReport> getAuditReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reportService.getAuditReport(start, end, pageable));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<DashboardReport> getDashboardReport() {
        return ResponseEntity.ok(reportService.getDashboardReport());
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<List<ProductAlert>> getLowStockAlerts() {
        return ResponseEntity.ok(reportService.findLowStockAlerts());
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam String report,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        LocalDateTime actualEnd = end == null ? LocalDateTime.now() : end;
        LocalDateTime actualStart = start == null ? actualEnd.minusDays(30) : start;
        byte[] content = reportExportService.exportExcel(report, actualStart, actualEnd);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam String report,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        LocalDateTime actualEnd = end == null ? LocalDateTime.now() : end;
        LocalDateTime actualStart = start == null ? actualEnd.minusDays(30) : start;
        byte[] content = reportExportService.exportPdf(report, actualStart, actualEnd);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
