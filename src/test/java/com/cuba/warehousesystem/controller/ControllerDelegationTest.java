package com.cuba.warehousesystem.controller;

import com.cuba.warehousesystem.dto.CounterpartyRequest;
import com.cuba.warehousesystem.dto.EdiCustomerReceiptRequest;
import com.cuba.warehousesystem.dto.EdiMappingConfigRequest;
import com.cuba.warehousesystem.dto.EdiMessageReceiveRequest;
import com.cuba.warehousesystem.dto.EdiProcessRequest;
import com.cuba.warehousesystem.dto.EdiSimulationRequest;
import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.dto.OperationVerificationRequest;
import com.cuba.warehousesystem.dto.ProductRequest;
import com.cuba.warehousesystem.dto.ProductWarehouseMinStockRequest;
import com.cuba.warehousesystem.dto.StorageCellRequest;
import com.cuba.warehousesystem.dto.UserCreateRequest;
import com.cuba.warehousesystem.dto.UserUpdateRequest;
import com.cuba.warehousesystem.dto.WarehouseRequest;
import com.cuba.warehousesystem.model.CounterpartyType;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.UserRole;
import com.cuba.warehousesystem.service.CounterpartyService;
import com.cuba.warehousesystem.service.EdiMappingConfigService;
import com.cuba.warehousesystem.service.EdiPartnerService;
import com.cuba.warehousesystem.service.EdiProcessingService;
import com.cuba.warehousesystem.service.OperationService;
import com.cuba.warehousesystem.service.ProductService;
import com.cuba.warehousesystem.service.ReportExportService;
import com.cuba.warehousesystem.service.ReportService;
import com.cuba.warehousesystem.service.StorageCellService;
import com.cuba.warehousesystem.service.UserService;
import com.cuba.warehousesystem.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerDelegationTest {

    @Test
    void catalogControllersReturnExpectedStatusesAndDelegate() {
        var pageable = PageRequest.of(0, 5);

        WarehouseService warehouseService = mock(WarehouseService.class);
        WarehouseController warehouseController = new WarehouseController(warehouseService);
        WarehouseRequest warehouseRequest = new WarehouseRequest("WH-1", "Warehouse", null, true);
        assertThat(warehouseController.create(warehouseRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        warehouseController.getById(1L);
        warehouseController.getAll("wh", pageable);
        warehouseController.update(1L, warehouseRequest);
        assertThat(warehouseController.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(warehouseService).getAll("wh", pageable);

        StorageCellService storageCellService = mock(StorageCellService.class);
        StorageCellController storageCellController = new StorageCellController(storageCellService);
        StorageCellRequest cellRequest = new StorageCellRequest(1L, "A-01", null, null, null, null, 1,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, true);
        assertThat(storageCellController.create(cellRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        storageCellController.getById(10L);
        storageCellController.getAll(1L, "A", pageable);
        storageCellController.update(10L, cellRequest);
        assertThat(storageCellController.delete(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(storageCellService).getAll(1L, "A", pageable);

        CounterpartyService counterpartyService = mock(CounterpartyService.class);
        CounterpartyController counterpartyController = new CounterpartyController(counterpartyService);
        CounterpartyRequest counterpartyRequest = new CounterpartyRequest("SUP-1", "Supplier", CounterpartyType.SUPPLIER,
                null, null, null, null, null, null, true);
        assertThat(counterpartyController.create(counterpartyRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        counterpartyController.getById(1L);
        counterpartyController.getAll("sup", pageable);
        counterpartyController.update(1L, counterpartyRequest);
        assertThat(counterpartyController.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(counterpartyService).getAll("sup", pageable);
    }

    @Test
    void productAndUserControllersDelegateAllEndpoints() {
        var pageable = PageRequest.of(0, 5);

        ProductService productService = mock(ProductService.class);
        ProductController productController = new ProductController(productService);
        ProductRequest productRequest = new ProductRequest("SKU-1", null, "Product", null,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, true);
        ProductWarehouseMinStockRequest minStockRequest = new ProductWarehouseMinStockRequest(1L, 5);
        assertThat(productController.create(productRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        productController.getById(1L);
        productController.getCategories();
        productController.getCard(1L, 2L);
        productController.getAll("sku", pageable);
        productController.update(1L, productRequest);
        productController.setWarehouseMinStock(1L, minStockRequest);
        assertThat(productController.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productService).getCard(1L, 2L);

        UserService userService = mock(UserService.class);
        UserController userController = new UserController(userService);
        UserCreateRequest createRequest = new UserCreateRequest("admin", "secret", "Admin", "a@example.com", UserRole.ADMIN, true);
        UserUpdateRequest updateRequest = new UserUpdateRequest("new", "Admin", "a@example.com", UserRole.MANAGER, true);
        assertThat(userController.create(createRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        userController.getById(1L);
        userController.getAll("adm", pageable);
        userController.update(1L, updateRequest);
        assertThat(userController.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).update(1L, updateRequest);
    }

    @Test
    void ediControllersDelegateIncludingDefaultQueueRequestAndSystemUserFallbacks() {
        var pageable = PageRequest.of(0, 5);

        EdiPartnerService partnerService = mock(EdiPartnerService.class);
        EdiPartnerController partnerController = new EdiPartnerController(partnerService);
        var partnerRequest = new com.cuba.warehousesystem.dto.EdiPartnerRequest("EDI-1", "Partner", null, null, List.of(1L), true, false, true);
        assertThat(partnerController.create(partnerRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        partnerController.getById(1L);
        partnerController.getAll("edi", pageable);
        partnerController.update(1L, partnerRequest);
        assertThat(partnerController.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        EdiMappingConfigService mappingService = mock(EdiMappingConfigService.class);
        EdiMappingConfigController mappingController = new EdiMappingConfigController(mappingService);
        EdiMappingConfigRequest mappingRequest = new EdiMappingConfigRequest(1L, "EXT-1", 2L, true);
        assertThat(mappingController.create(mappingRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        mappingController.getById(1L);
        mappingController.getAll("ext", 1L, pageable);
        mappingController.update(1L, mappingRequest);
        assertThat(mappingController.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        EdiProcessingService processingService = mock(EdiProcessingService.class);
        EdiController ediController = new EdiController(processingService);
        EdiMessageReceiveRequest receiveRequest = new EdiMessageReceiveRequest(null, "EDI-1", EdiMessageType.DESADV, "INT", "MSG", "DOC", "raw", null);
        EdiSimulationRequest simulationRequest = new EdiSimulationRequest(1L, 2L, "DOC", List.of(new EdiSimulationRequest.Item(null, 3L, "EXT-1", 1, null)));
        EdiCustomerReceiptRequest receiptRequest = new EdiCustomerReceiptRequest(1L, 2L, "DOC");

        assertThat(ediController.receiveInbound(receiveRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(ediController.simulateSupplier(simulationRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(ediController.simulateCustomer(simulationRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(ediController.simulateCustomerReceipt(receiptRequest, null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ediController.getMessage(1L);
        ediController.getMessages(EdiMessageType.DESADV, EdiMessageStatus.PROCESSED, EdiMessageStatus.RECEIVED, pageable);
        ediController.getQueue(EdiMessageStatus.FAILED, pageable);
        ediController.processQueueItem(1L, null, null);
        ediController.getAudit(1L, pageable);

        verify(processingService).simulateCustomerReceipt(receiptRequest, "system");
        verify(processingService).getMessages(EdiMessageType.DESADV, EdiMessageStatus.PROCESSED, pageable);
        verify(processingService).processQueueItem(eq(1L), any(EdiProcessRequest.class), eq("system"));
    }

    @Test
    void operationAndReportControllersDelegateAuthenticationAndExportDefaults() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("manager", null);
        var pageable = PageRequest.of(0, 5);

        OperationService operationService = mock(OperationService.class);
        OperationController operationController = new OperationController(operationService);
        OperationRequest operationRequest = new OperationRequest(OperationType.INCOME, 1L, null, null, null, null, null, List.of());
        OperationVerificationRequest verificationRequest = new OperationVerificationRequest(null, null, List.of());

        assertThat(operationController.createDraft(operationRequest, authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        operationController.completeOperation(1L, authentication);
        operationController.shipOperation(1L, authentication);
        operationController.cancelOperation(1L);
        operationController.getById(1L);
        operationController.getAllOperations(OperationType.INCOME, OperationStatus.DRAFT, 1L, pageable);
        operationController.getStockBalance(1L, pageable);
        assertThat(operationController.verifyOperation(1L, verificationRequest, authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        operationController.getVerifications(1L);
        operationController.getExecutionChain(1L);
        verify(operationService).createDraftOperation(operationRequest, "manager");

        ReportService reportService = mock(ReportService.class);
        ReportExportService exportService = mock(ReportExportService.class);
        ReportController reportController = new ReportController(reportService, exportService);
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(exportService.exportExcel(eq("stock-balance"), any(), any())).thenReturn(new byte[]{1, 2});
        when(exportService.exportPdf(eq("stock-balance"), any(), any())).thenReturn(new byte[]{3, 4});

        reportController.getTurnoverReport(start, end);
        reportController.getStockBalanceReport();
        reportController.getMovementReport(start, end);
        reportController.getTopProductsReport(5, start, end);
        reportController.getSupplierStatsReport(start, end);
        reportController.getCellUtilizationReport();
        reportController.getABCAnalysisReport(start, end);
        reportController.getEdiStatisticsReport();
        reportController.getAuditReport(start, end, pageable);
        reportController.getDashboardReport(1L, 7);
        reportController.getLowStockAlerts();
        assertThat(reportController.exportExcel("stock-balance", null, null).getHeaders().getFirst("Content-Disposition"))
                .contains("stock-balance.xlsx");
        assertThat(reportController.exportPdf("stock-balance", null, null).getHeaders().getContentType().toString())
                .isEqualTo("application/pdf");
    }
}
