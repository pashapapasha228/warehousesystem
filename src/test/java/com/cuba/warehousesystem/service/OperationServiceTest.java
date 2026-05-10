package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.exception.InsufficientStockException;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.CounterpartyRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.UserRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private OperationRepository operationRepository;
    @Mock
    private StockBalanceRepository stockBalanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StorageCellRepository storageCellRepository;
    @Mock
    private CounterpartyRepository counterpartyRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OperationService operationService;

    @Test
    void completeOutcomeRejectsInsufficientStock() {
        Product product = product();
        StorageCell cell = cell();
        Operation operation = operation(product, cell, 5);
        User user = new User();
        user.setUsername("manager");

        when(operationRepository.findWithItemsById(1L)).thenReturn(Optional.of(operation));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId()))
                .thenReturn(Optional.of(new StockBalance(product, cell, 2)));

        assertThatThrownBy(() -> operationService.completeOperation(1L, "manager"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient available stock");
    }

    private Operation operation(Product product, StorageCell cell, Integer quantity) {
        Operation operation = new Operation();
        operation.setId(1L);
        operation.setOperationNumber("OUTCOME-1");
        operation.setType(OperationType.OUTCOME);
        operation.setStatus(OperationStatus.DRAFT);
        operation.setWarehouse(cell.getWarehouse());
        operation.setCreatedBy(new User());

        OperationItem item = new OperationItem();
        item.setOperation(operation);
        item.setProduct(product);
        item.setFromCell(cell);
        item.setQuantity(quantity);
        operation.getItems().add(item);
        return operation;
    }

    private Product product() {
        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU-1");
        product.setName("Test product");
        product.setUnitOfMeasure("pcs");
        product.setWeightPerUnitKg(BigDecimal.ONE);
        product.setVolumePerUnitCm3(BigDecimal.ONE);
        product.setLengthCm(BigDecimal.ONE);
        product.setWidthCm(BigDecimal.ONE);
        product.setHeightCm(BigDecimal.ONE);
        return product;
    }

    private StorageCell cell() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("WH-1");

        StorageCell cell = new StorageCell();
        cell.setId(10L);
        cell.setCode("A-01");
        cell.setWarehouse(warehouse);
        return cell;
    }
}
