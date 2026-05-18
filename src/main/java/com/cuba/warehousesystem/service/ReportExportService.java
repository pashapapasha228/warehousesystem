package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.ABCAnalysisReport;
import com.cuba.warehousesystem.dto.AuditReport;
import com.cuba.warehousesystem.dto.CellUtilizationReport;
import com.cuba.warehousesystem.dto.DashboardReport;
import com.cuba.warehousesystem.dto.EdiStatisticsReport;
import com.cuba.warehousesystem.dto.MovementReport;
import com.cuba.warehousesystem.dto.StockBalanceReport;
import com.cuba.warehousesystem.dto.SupplierStatsReport;
import com.cuba.warehousesystem.dto.TopProductReport;
import com.cuba.warehousesystem.dto.TurnoverReport;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final ReportService reportService;

    public byte[] exportExcel(String report, LocalDateTime start, LocalDateTime end) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeReportSheet(workbook, report, start, end);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export Excel report.", ex);
        }
    }

    public byte[] exportPdf(String report, LocalDateTime start, LocalDateTime end) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputStream));
            Document document = new Document(pdfDocument);
            document.add(new Paragraph("Warehouse ERP Report: " + report));
            writePdfReport(document, report, start, end);
            document.close();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export PDF report.", ex);
        }
    }

    private void writeReportSheet(Workbook workbook, String report, LocalDateTime start, LocalDateTime end) {
        Sheet sheet = workbook.createSheet(normalizeReportName(report));
        switch (normalizeReportName(report)) {
            case "turnover" -> writeMapSheet(sheet, reportService.calculateTurnover(start, end).summary());
            case "stock-balance" -> writeStockBalance(sheet, reportService.getStockBalanceReport());
            case "movement" -> writeMovement(sheet, reportService.getMovementReport(start, end));
            case "top-products" -> writeTopProducts(sheet, reportService.getTopProductsReport(50, start, end));
            case "supplier-stats" -> writeSupplierStats(sheet, reportService.getSupplierStatsReport(start, end));
            case "cell-utilization" -> writeCellUtilization(sheet, reportService.getCellUtilizationReport());
            case "abc-analysis" -> writeAbc(sheet, reportService.getABCAnalysisReport(start, end));
            case "edi-statistics" -> writeEdiStats(sheet, reportService.getEdiStatisticsReport());
            case "audit" -> writeAudit(sheet, reportService.getAuditReport(start, end, PageRequest.of(0, 500)));
            case "dashboard" -> writeDashboard(sheet, reportService.getDashboardReport());
            default -> throw new IllegalArgumentException("Unsupported report for export: " + report);
        }
        for (int i = 0; i < 12; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writePdfReport(Document document, String report, LocalDateTime start, LocalDateTime end) {
        WorkbookBackedRows rows = buildRows(report, start, end);
        Table table = new Table(rows.headers().size());
        rows.headers().forEach(header -> table.addHeaderCell(new Cell().add(new Paragraph(header))));
        rows.rows().forEach(row -> row.forEach(value -> table.addCell(new Cell().add(new Paragraph(value)))));
        document.add(table);
    }

    private WorkbookBackedRows buildRows(String report, LocalDateTime start, LocalDateTime end) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("report");
            writeReportSheet(workbook, report, start, end);
            Sheet actualSheet = workbook.getSheetAt(1);
            List<String> headers = readRow(actualSheet.getRow(0));
            List<List<String>> rows = new java.util.ArrayList<>();
            for (int i = 1; i <= actualSheet.getLastRowNum(); i++) {
                rows.add(readRow(actualSheet.getRow(i)));
            }
            return new WorkbookBackedRows(headers, rows);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare PDF rows.", ex);
        }
    }

    private List<String> readRow(Row row) {
        if (row == null) {
            return List.of();
        }
        List<String> values = new java.util.ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(row.getCell(i) == null ? "" : row.getCell(i).toString());
        }
        return values;
    }

    private void writeMapSheet(Sheet sheet, Map<String, Object> values) {
        writeHeader(sheet, "metric", "value");
        int rowIndex = 1;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            writeRow(sheet, rowIndex++, entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private void writeStockBalance(Sheet sheet, StockBalanceReport report) {
        writeHeader(sheet, "sku", "product", "quantity", "cell");
        int rowIndex = 1;
        for (StockBalanceReport.BalanceItem item : report.balances()) {
            writeRow(sheet, rowIndex++, item.productSku(), item.productName(), item.quantity(), item.cellCode());
        }
    }

    private void writeMovement(Sheet sheet, MovementReport report) {
        writeHeader(sheet, "operation", "type", "sku", "product", "quantity", "from_cell", "to_cell", "completed_at");
        int rowIndex = 1;
        for (MovementReport.MovementItem item : report.movements()) {
            writeRow(sheet, rowIndex++, item.operationNumber(), item.operationType(), item.productSku(), item.productName(),
                    item.quantity(), item.fromCellCode(), item.toCellCode(), item.completedAt());
        }
    }

    private void writeTopProducts(Sheet sheet, TopProductReport report) {
        writeHeader(sheet, "sku", "product", "total_quantity", "movement_count");
        int rowIndex = 1;
        for (TopProductReport.TopProductItem item : report.topProducts()) {
            writeRow(sheet, rowIndex++, item.productSku(), item.productName(), item.totalQuantity(), item.turnoverCount());
        }
    }

    private void writeSupplierStats(Sheet sheet, SupplierStatsReport report) {
        writeHeader(sheet, "supplier", "incoming_operations", "incoming_quantity");
        int rowIndex = 1;
        for (SupplierStatsReport.SupplierStatItem item : report.supplierStats()) {
            writeRow(sheet, rowIndex++, item.supplierName(), item.totalIncomingOps(), item.totalIncomingQty());
        }
    }

    private void writeCellUtilization(Sheet sheet, CellUtilizationReport report) {
        writeHeader(sheet, "cell", "current_volume", "max_volume", "current_weight", "max_weight");
        int rowIndex = 1;
        for (CellUtilizationReport.CellUtilizationItem item : report.utilizations()) {
            writeRow(sheet, rowIndex++, item.cellCode(), item.currentVolume(), item.maxVolume(), item.currentWeight(), item.maxWeight());
        }
    }

    private void writeAbc(Sheet sheet, ABCAnalysisReport report) {
        writeHeader(sheet, "sku", "product", "total_quantity", "cumulative_percentage", "category");
        int rowIndex = 1;
        for (ABCAnalysisReport.ABCItem item : report.analysis()) {
            writeRow(sheet, rowIndex++, item.productSku(), item.productName(), item.totalQuantity(), item.percentage(), item.category());
        }
    }

    private void writeEdiStats(Sheet sheet, EdiStatisticsReport report) {
        writeHeader(sheet, "metric", "value");
        int rowIndex = 1;
        writeRow(sheet, rowIndex++, "total_messages", report.totalMessages());
        writeRow(sheet, rowIndex++, "pending_queue_items", report.pendingQueueItems());
        writeRow(sheet, rowIndex++, "failed_queue_items", report.failedQueueItems());
        for (Map.Entry<String, Long> entry : report.messagesByStatus().entrySet()) {
            writeRow(sheet, rowIndex++, "status_" + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Long> entry : report.messagesByType().entrySet()) {
            writeRow(sheet, rowIndex++, "type_" + entry.getKey(), entry.getValue());
        }
    }

    private void writeAudit(Sheet sheet, AuditReport report) {
        writeHeader(sheet, "id", "entity", "entity_id", "action", "username", "occurred_at", "details");
        int rowIndex = 1;
        for (AuditReport.AuditItem item : report.items()) {
            writeRow(sheet, rowIndex++, item.id(), item.entityName(), item.entityId(), item.action(),
                    item.username(), item.occurredAt(), item.detailsJson());
        }
    }

    private void writeDashboard(Sheet sheet, DashboardReport report) {
        writeHeader(sheet, "metric", "value");
        writeRow(sheet, 1, "expected_receiving", report.kpi().expectedReceiving());
        writeRow(sheet, 2, "ready_to_ship", report.kpi().readyToShip());
        writeRow(sheet, 3, "pending_edi_messages", report.kpi().pendingEdi());
        writeRow(sheet, 4, "failed_edi_messages", report.kpi().failedEdi());
        writeRow(sheet, 5, "zero_stock_products", report.kpi().zeroStockProducts());
        writeRow(sheet, 6, "below_min_products", report.kpi().belowMinProducts());
        writeRow(sheet, 7, "average_volume_utilization", report.kpi().averageVolumeUtilization());
        writeRow(sheet, 8, "average_weight_utilization", report.kpi().averageWeightUtilization());
        writeRow(sheet, 9, "completed_operations", report.kpi().completedOperations());
        writeRow(sheet, 10, "draft_operations", report.kpi().draftOperations());
    }

    private void writeHeader(Sheet sheet, String... values) {
        writeRow(sheet, 0, (Object[]) values);
    }

    private void writeRow(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i] == null ? "" : String.valueOf(values[i]));
        }
    }

    private String normalizeReportName(String report) {
        return report == null ? "" : report.trim().toLowerCase();
    }

    private record WorkbookBackedRows(List<String> headers, List<List<String>> rows) {
    }
}
