package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.ABCAnalysisReport;
import com.cuba.warehousesystem.dto.StockBalanceReport;
import com.cuba.warehousesystem.model.AuditLog;
import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.CounterpartyType;
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiPartner;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.ProductWarehouseMinStock;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.Warehouse;
import org.springframework.data.domain.PageImpl;
import com.cuba.warehousesystem.repository.AuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.ProductWarehouseMinStockRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OperationRepository operationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductWarehouseMinStockRepository productWarehouseMinStockRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockBalanceRepository stockBalanceRepository;
    @Mock
    private StorageCellRepository storageCellRepository;
    @Mock
    private EdiMessageRepository ediMessageRepository;
    @Mock
    private EdiProcessingQueueRepository ediProcessingQueueRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void stockBalanceReportUsesStockBalances() {
        Product product = product(1L, "SKU-1", "Test product");
        StorageCell cell = cell(10L, "A-01");
        StockBalance balance = new StockBalance(product, cell, 12);

        when(stockBalanceRepository.findAll()).thenReturn(List.of(balance));

        StockBalanceReport report = reportService.getStockBalanceReport();

        assertThat(report.balances()).hasSize(1);
        assertThat(report.balances().get(0).productSku()).isEqualTo("SKU-1");
        assertThat(report.balances().get(0).quantity()).isEqualTo(12);
        assertThat(report.balances().get(0).cellCode()).isEqualTo("A-01");
    }

    @Test
    void abcAnalysisUsesCumulativeShare() {
        Product first = product(1L, "A-SKU", "A product");
        Product second = product(2L, "B-SKU", "B product");
        Product third = product(3L, "C-SKU", "C product");

        Operation operation = new Operation();
        operation.setStatus(OperationStatus.COMPLETED);
        operation.setType(OperationType.OUTCOME);
        operation.getItems().add(item(operation, first, 80));
        operation.getItems().add(item(operation, second, 15));
        operation.getItems().add(item(operation, third, 5));

        when(operationRepository.findCompletedOperationsByTypeAndPeriod(any(), any(), eq(OperationType.OUTCOME)))
                .thenReturn(List.of(operation));

        ABCAnalysisReport report = reportService.getABCAnalysisReport(LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(report.analysis()).extracting(ABCAnalysisReport.ABCItem::category)
                .containsExactly("A", "B", "C");
        assertThat(report.analysis()).extracting(ABCAnalysisReport.ABCItem::percentage)
                .containsExactly(80.0, 95.0, 100.0);
    }

    @Test
    void turnoverMovementTopProductsSupplierStatsAndCellUtilizationUseRepositoryData() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now();
        Product first = product(1L, "A-SKU", "A product");
        Product second = product(2L, "B-SKU", "B product");
        Warehouse warehouse = cell(10L, "A-01").getWarehouse();
        Counterparty supplier = counterparty(7L, "Supplier");

        Operation income = operation("IN-1", OperationType.INCOME, OperationStatus.COMPLETED, warehouse, supplier);
        income.getItems().add(item(income, first, 10));
        Operation outcome = operation("OUT-1", OperationType.OUTCOME, OperationStatus.COMPLETED, warehouse, null);
        outcome.getItems().add(item(outcome, first, 4));
        outcome.getItems().add(item(outcome, second, 6));
        Operation move = operation("MOVE-1", OperationType.MOVE, OperationStatus.COMPLETED, warehouse, null);
        move.getItems().add(item(move, second, 3));

        when(operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.INCOME)).thenReturn(List.of(income));
        when(operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.OUTCOME)).thenReturn(List.of(outcome));
        when(operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.MOVE)).thenReturn(List.of(move));
        when(operationRepository.findByCreatedAtBetweenAndStatus(start, end, OperationStatus.COMPLETED))
                .thenReturn(List.of(income, outcome, move));

        var turnover = reportService.calculateTurnover(start, end);
        assertThat(turnover.summary()).containsEntry("total_incoming", 10L);
        assertThat(turnover.summary()).containsEntry("total_outgoing", 10L);
        assertThat(turnover.summary()).containsEntry("total_moved", 3L);

        var movement = reportService.getMovementReport(start, end);
        assertThat(movement.movements()).hasSize(4);
        assertThat(movement.movements()).extracting("operationNumber").contains("IN-1", "OUT-1", "MOVE-1");

        var topProducts = reportService.getTopProductsReport(1, start, end);
        assertThat(topProducts.topProducts()).singleElement()
                .satisfies(item -> {
                    assertThat(item.productSku()).isEqualTo("A-SKU");
                    assertThat(item.totalQuantity()).isEqualTo(14);
                });

        var supplierStats = reportService.getSupplierStatsReport(start, end);
        assertThat(supplierStats.supplierStats()).singleElement()
                .satisfies(item -> {
                    assertThat(item.supplierName()).isEqualTo("Supplier");
                    assertThat(item.totalIncomingOps()).isEqualTo(1);
                    assertThat(item.totalIncomingQty()).isEqualTo(10);
                });

        StorageCell utilized = cell(30L, "HOT");
        utilized.setCurrentVolumeCm3(BigDecimal.valueOf(90));
        utilized.setMaxVolumeCm3(BigDecimal.valueOf(100));
        utilized.setCurrentWeightKg(BigDecimal.valueOf(50));
        utilized.setMaxWeightKg(BigDecimal.valueOf(100));
        when(storageCellRepository.findAll()).thenReturn(List.of(utilized));

        var utilization = reportService.getCellUtilizationReport();
        assertThat(utilization.utilizations()).singleElement()
                .satisfies(item -> {
                    assertThat(item.cellCode()).isEqualTo("HOT");
                    assertThat(item.currentVolume()).isEqualByComparingTo("90");
                });
    }

    @Test
    void ediStatisticsAuditAndLowStockReportsUseRepositoryData() {
        when(ediMessageRepository.count()).thenReturn(5L);
        for (EdiMessageStatus status : EdiMessageStatus.values()) {
            when(ediMessageRepository.countByStatus(status)).thenReturn((long) status.ordinal());
        }
        for (EdiMessageType type : EdiMessageType.values()) {
            when(ediMessageRepository.countByMessageType(type)).thenReturn((long) type.ordinal() + 1);
        }
        when(ediProcessingQueueRepository.countByEdiMessage_Status(EdiMessageStatus.RECEIVED)).thenReturn(2L);
        when(ediProcessingQueueRepository.countByEdiMessage_Status(EdiMessageStatus.NORMALIZED)).thenReturn(3L);
        when(ediProcessingQueueRepository.countByEdiMessage_Status(EdiMessageStatus.FAILED)).thenReturn(1L);

        var edi = reportService.getEdiStatisticsReport();

        assertThat(edi.totalMessages()).isEqualTo(5);
        assertThat(edi.pendingQueueItems()).isEqualTo(5);
        assertThat(edi.failedQueueItems()).isEqualTo(1);
        assertThat(edi.messagesByType()).containsEntry(EdiMessageType.DESADV.name(), (long) EdiMessageType.DESADV.ordinal() + 1);

        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setEntityName("Operation");
        log.setEntityId("10");
        log.setAction("CREATED");
        log.setUsername("manager");
        log.setDetailsJson("{}");
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(auditLogRepository.findByOccurredAtBetween(eq(start), eq(end), any())).thenReturn(new PageImpl<>(List.of(log)));
        when(auditLogRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(log)));

        assertThat(reportService.getAuditReport(start, end, org.springframework.data.domain.PageRequest.of(0, 10)).items())
                .singleElement().extracting("action").isEqualTo("CREATED");
        assertThat(reportService.getAuditReport(null, null, org.springframework.data.domain.PageRequest.of(0, 10)).items())
                .hasSize(1);

        Product product = product(1L, "LOW", "Low stock product");
        Warehouse warehouse = new Warehouse();
        warehouse.setId(2L);
        warehouse.setCode("WH-LOW");
        ProductWarehouseMinStock minStock = new ProductWarehouseMinStock();
        minStock.setProduct(product);
        minStock.setWarehouse(warehouse);
        minStock.setMinStockLevel(10);
        StockBalance balance = new StockBalance(product, cell(20L, "LOW-CELL"), 4);
        balance.getCell().setWarehouse(warehouse);
        when(stockBalanceRepository.findAll()).thenReturn(List.of(balance));
        when(productWarehouseMinStockRepository.findAll()).thenReturn(List.of(minStock));

        assertThat(reportService.findLowStockAlerts()).singleElement()
                .satisfies(alert -> {
                    assertThat(alert.sku()).isEqualTo("LOW");
                    assertThat(alert.currentStock()).isEqualTo(4);
                    assertThat(alert.minLevel()).isEqualTo(10);
                });
    }

    @Test
    void dashboardReportBuildsKpiQueuesWarningsAndRecentOperations() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("WH-1");
        Product product = product(1L, "SKU-1", "Product");
        Counterparty supplier = counterparty(1L, "Supplier");
        User user = new User();
        user.setUsername("manager");

        Operation draftIncome = operation("IN-DRAFT", OperationType.INCOME, OperationStatus.DRAFT, warehouse, supplier);
        draftIncome.setCreatedBy(user);
        draftIncome.getItems().add(item(draftIncome, product, 2));
        Operation draftOutcome = operation("OUT-DRAFT", OperationType.OUTCOME, OperationStatus.DRAFT, warehouse, supplier);
        draftOutcome.setCreatedBy(user);
        draftOutcome.getItems().add(item(draftOutcome, product, 3));
        Operation completed = operation("DONE", OperationType.INCOME, OperationStatus.COMPLETED, warehouse, supplier);
        completed.setCreatedBy(user);
        completed.setCompletedBy(user);
        completed.setCompletedAt(LocalDateTime.now().minusHours(2));
        completed.getItems().add(item(completed, product, 5));

        EdiPartner partner = new EdiPartner();
        partner.setCode("EDI-1");
        partner.setName("Partner");
        EdiMessage failed = ediMessage(10L, EdiMessageType.DESADV, EdiMessageStatus.FAILED, partner, "{\"warehouseId\":1,\"items\":[{}]}");
        failed.setErrorMessage("bad");
        EdiMessage pending = ediMessage(11L, EdiMessageType.ORDERS, EdiMessageStatus.RECEIVED, partner, "{\"warehouseId\":1,\"items\":[{},{}]}");

        StorageCell hotCell = cell(30L, "HOT");
        hotCell.setWarehouse(warehouse);
        hotCell.setCurrentVolumeCm3(BigDecimal.valueOf(95));
        hotCell.setMaxVolumeCm3(BigDecimal.valueOf(100));
        hotCell.setCurrentWeightKg(BigDecimal.valueOf(80));
        hotCell.setMaxWeightKg(BigDecimal.valueOf(100));

        ProductWarehouseMinStock minStock = new ProductWarehouseMinStock();
        minStock.setProduct(product);
        minStock.setWarehouse(warehouse);
        minStock.setMinStockLevel(10);
        StockBalance balance = new StockBalance(product, hotCell, 0);

        when(operationRepository.findAll()).thenReturn(List.of(draftIncome, draftOutcome, completed));
        when(ediMessageRepository.findAll()).thenReturn(List.of(failed, pending));
        when(storageCellRepository.findAll()).thenReturn(List.of(hotCell));
        when(stockBalanceRepository.findAll()).thenReturn(List.of(balance));
        when(productWarehouseMinStockRepository.findAll()).thenReturn(List.of(minStock));

        var dashboard = reportService.getDashboardReport(1L, 7);

        assertThat(dashboard.kpi().expectedReceiving()).isEqualTo(2);
        assertThat(dashboard.kpi().readyToShip()).isEqualTo(2);
        assertThat(dashboard.kpi().failedEdi()).isEqualTo(1);
        assertThat(dashboard.kpi().zeroStockProducts()).isEqualTo(1);
        assertThat(dashboard.receivingQueue()).isNotEmpty();
        assertThat(dashboard.shippingQueue()).isNotEmpty();
        assertThat(dashboard.attentionItems()).extracting("type")
                .contains("EDI_ERROR", "ZERO_STOCK", "CELL_OVERLOAD", "OPERATION_DRAFT", "EDI_PENDING");
        assertThat(dashboard.recentOperations()).extracting("operationNumber").contains("DONE");
    }

    private Product product(Long id, String sku, String name) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        return product;
    }

    private Counterparty counterparty(Long id, String name) {
        Counterparty counterparty = new Counterparty();
        counterparty.setId(id);
        counterparty.setCode("CP-" + id);
        counterparty.setName(name);
        counterparty.setType(CounterpartyType.SUPPLIER);
        return counterparty;
    }

    private StorageCell cell(Long id, String code) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("WH-1");

        StorageCell cell = new StorageCell();
        cell.setId(id);
        cell.setCode(code);
        cell.setWarehouse(warehouse);
        return cell;
    }

    private OperationItem item(Operation operation, Product product, Integer quantity) {
        OperationItem item = new OperationItem();
        item.setOperation(operation);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    private Operation operation(String number, OperationType type, OperationStatus status, Warehouse warehouse, Counterparty counterparty) {
        Operation operation = new Operation();
        operation.setId((long) Math.abs(number.hashCode()));
        operation.setOperationNumber(number);
        operation.setType(type);
        operation.setStatus(status);
        operation.setWarehouse(warehouse);
        operation.setCounterparty(counterparty);
        operation.setCreatedAt(LocalDateTime.now().minusHours(3));
        operation.setCompletedAt(status == OperationStatus.COMPLETED ? LocalDateTime.now().minusHours(1) : null);
        User user = new User();
        user.setUsername("manager");
        operation.setCreatedBy(user);
        return operation;
    }

    private EdiMessage ediMessage(Long id, EdiMessageType type, EdiMessageStatus status, EdiPartner partner, String payload) {
        EdiMessage message = new EdiMessage();
        message.setId(id);
        message.setMessageType(type);
        message.setStatus(status);
        message.setPartner(partner);
        message.setDocumentNumber("DOC-" + id);
        message.setNormalizedPayload(payload);
        message.setReceivedAt(LocalDateTime.now().minusHours(1));
        return message;
    }
}
