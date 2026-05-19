package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.exception.InsufficientStockException;
import com.cuba.warehousesystem.dto.OperationVerificationRequest;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationVerification;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.VerificationDecision;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.CounterpartyRepository;
import com.cuba.warehousesystem.repository.DocumentExecutionStepRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.OperationVerificationRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    private DocumentExecutionStepRepository documentExecutionStepRepository;
    @Mock
    private OperationVerificationRepository operationVerificationRepository;
    @Mock
    private EdiMessageRepository ediMessageRepository;
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

    @Test
    void completeIncomeIncreasesBalanceAndCellLoad() {
        Product product = product();
        product.setWeightPerUnitKg(BigDecimal.valueOf(2));
        product.setVolumePerUnitCm3(BigDecimal.valueOf(3));
        StorageCell cell = cell();
        cell.setCurrentWeightKg(BigDecimal.valueOf(4));
        cell.setCurrentVolumeCm3(BigDecimal.valueOf(5));
        Operation operation = operation(product, cell, 5);
        operation.setType(OperationType.INCOME);
        operation.getItems().get(0).setFromCell(null);
        operation.getItems().get(0).setToCell(cell);
        User user = user("manager");

        when(operationRepository.findWithItemsById(1L)).thenReturn(Optional.of(operation));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(stockBalanceRepository.findByCell_Id(cell.getId())).thenReturn(List.of());
        when(stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId())).thenReturn(Optional.empty());
        when(operationRepository.save(any(Operation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = operationService.completeOperation(1L, "manager");

        assertThat(response.status()).isEqualTo(OperationStatus.COMPLETED);
        assertThat(cell.getCurrentWeightKg()).isEqualByComparingTo("14");
        assertThat(cell.getCurrentVolumeCm3()).isEqualByComparingTo("20");
        verify(stockBalanceRepository).save(org.mockito.ArgumentMatchers.argThat(balance -> balance.getQuantity() == 5));
        verify(storageCellRepository).save(cell);
        verify(eventPublisher).publishEvent(any(com.cuba.warehousesystem.event.OperationCompletedEvent.class));
    }

    @Test
    void shipOutcomeDecreasesStockAndMarksOperationAsShipped() {
        Product product = product();
        product.setWeightPerUnitKg(BigDecimal.valueOf(2));
        product.setVolumePerUnitCm3(BigDecimal.valueOf(3));
        StorageCell cell = cell();
        cell.setCurrentWeightKg(BigDecimal.valueOf(20));
        cell.setCurrentVolumeCm3(BigDecimal.valueOf(30));
        Operation operation = operation(product, cell, 4);
        User user = user("manager");
        StockBalance balance = new StockBalance(product, cell, 10);
        balance.setReservedQuantity(1);

        when(operationRepository.findWithItemsById(1L)).thenReturn(Optional.of(operation));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId()))
                .thenReturn(Optional.of(balance));
        when(operationRepository.save(any(Operation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = operationService.shipOperation(1L, "manager");

        assertThat(response.status()).isEqualTo(OperationStatus.SHIPPED);
        assertThat(balance.getQuantity()).isEqualTo(6);
        assertThat(cell.getCurrentWeightKg()).isEqualByComparingTo("12");
        assertThat(cell.getCurrentVolumeCm3()).isEqualByComparingTo("18");
        verify(stockBalanceRepository).save(balance);
    }

    @Test
    void rejectVerificationCancelsDraftOperation() {
        Product product = product();
        StorageCell cell = cell();
        Operation operation = operation(product, cell, 5);
        User user = user("manager");

        when(operationRepository.findWithItemsById(1L)).thenReturn(Optional.of(operation));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(operationVerificationRepository.save(any(OperationVerification.class))).thenAnswer(invocation -> {
            OperationVerification verification = invocation.getArgument(0);
            verification.setId(50L);
            verification.getItems().forEach(item -> item.setId(60L));
            return verification;
        });
        when(operationRepository.save(any(Operation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = operationService.verifyOperation(1L, new OperationVerificationRequest(
                VerificationDecision.REJECT,
                "Damaged",
                List.of(new OperationVerificationRequest.Item(10L, 0, "Missing"))
        ), "manager");

        assertThat(response.decision()).isEqualTo(VerificationDecision.REJECT);
        assertThat(operation.getStatus()).isEqualTo(OperationStatus.CANCELLED);
        verify(operationRepository).save(operation);
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
        item.setId(10L);
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
        cell.setCapacityUnits(100);
        cell.setMaxWeightKg(BigDecimal.valueOf(1000));
        cell.setMaxVolumeCm3(BigDecimal.valueOf(1000));
        cell.setLengthCm(BigDecimal.valueOf(100));
        cell.setWidthCm(BigDecimal.valueOf(100));
        cell.setHeightCm(BigDecimal.valueOf(100));
        cell.setCurrentWeightKg(BigDecimal.ZERO);
        cell.setCurrentVolumeCm3(BigDecimal.ZERO);
        cell.setIsActive(true);
        return cell;
    }

    private User user(String username) {
        User user = new User();
        user.setId(100L);
        user.setUsername(username);
        return user;
    }
}
