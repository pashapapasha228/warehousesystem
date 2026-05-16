package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.ABCAnalysisReport;
import com.cuba.warehousesystem.dto.StockBalanceReport;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.AuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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

    private Product product(Long id, String sku, String name) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setMinStockLevel(0);
        return product;
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
}
