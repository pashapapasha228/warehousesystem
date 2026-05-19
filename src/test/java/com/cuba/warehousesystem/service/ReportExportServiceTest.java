package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.StockBalanceReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportExportServiceTest {

    @Test
    void exportsStockBalanceReportAsExcelAndPdf() {
        ReportService reportService = mock(ReportService.class);
        ReportExportService exportService = new ReportExportService(reportService);
        when(reportService.getStockBalanceReport()).thenReturn(new StockBalanceReport(List.of(
                new StockBalanceReport.BalanceItem("SKU-1", "Product", 7, "A-01")
        )));

        byte[] excel = exportService.exportExcel("stock-balance", LocalDateTime.now().minusDays(1), LocalDateTime.now());
        byte[] pdf = exportService.exportPdf("stock-balance", LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(excel).startsWith(new byte[]{'P', 'K'});
        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
    }

    @Test
    void unsupportedReportNameFailsFast() {
        ReportExportService exportService = new ReportExportService(mock(ReportService.class));

        assertThatThrownBy(() -> exportService.exportExcel("unknown", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported report");
    }
}
