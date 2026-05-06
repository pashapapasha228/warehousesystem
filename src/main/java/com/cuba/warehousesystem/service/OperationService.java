package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.exception.InsufficientStockException;
import com.cuba.warehousesystem.exception.StorageCapacityException;
import com.cuba.warehousesystem.model.Counterparty;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationService {

    private final OperationRepository operationRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageCellRepository storageCellRepository;
    private final CounterpartyRepository counterpartyRepository;

    public Operation createDraftOperation(OperationRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        Counterparty counterparty = request.counterpartyId() == null
                ? null
                : counterpartyRepository.findById(request.counterpartyId())
                .orElseThrow(() -> new EntityNotFoundException("Counterparty not found"));

        OperationType type = OperationType.valueOf(request.type());
        Operation operation = new Operation();
        operation.setOperationNumber(generateOperationNumber(type.name()));
        operation.setType(type);
        operation.setStatus(OperationStatus.DRAFT);
        operation.setWarehouse(warehouse);
        operation.setCreatedBy(user);
        operation.setCounterparty(counterparty);

        for (OperationRequest.ItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));

            OperationItem item = new OperationItem();
            item.setProduct(product);
            item.setQuantity(itemReq.quantity());
            item.setUnitOfMeasure(product.getUnitOfMeasure());

            if (itemReq.fromCellId() != null) {
                item.setFromCell(storageCellRepository.findById(itemReq.fromCellId())
                        .orElseThrow(() -> new EntityNotFoundException("Source cell not found")));
            }
            if (itemReq.toCellId() != null) {
                item.setToCell(storageCellRepository.findById(itemReq.toCellId())
                        .orElseThrow(() -> new EntityNotFoundException("Target cell not found")));
            }

            item.setOperation(operation);
            operation.getItems().add(item);
        }

        return operationRepository.save(operation);
    }

    public Operation completeOperation(Long operationId, String username) {
        Operation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("Operation not found"));
        User completedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (operation.getStatus() != OperationStatus.DRAFT) {
            throw new IllegalStateException("Operation is already processed or cancelled.");
        }

        validateOperation(operation);

        for (OperationItem item : operation.getItems()) {
            updateStockBalanceAndCell(item, operation.getType());
        }

        operation.setStatus(OperationStatus.COMPLETED);
        operation.setCompletedBy(completedBy);
        operation.setCompletedAt(LocalDateTime.now());

        return operationRepository.save(operation);
    }

    private void validateOperation(Operation operation) {
        if (operation.getType() == OperationType.OUTCOME || operation.getType() == OperationType.MOVE) {
            for (OperationItem item : operation.getItems()) {
                StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                                item.getProduct().getId(), item.getFromCell().getId())
                        .orElseThrow(() -> new InsufficientStockException("No stock found for product " + item.getProduct().getSku()));

                if (balance.getQuantity() < item.getQuantity()) {
                    throw new InsufficientStockException("Insufficient stock for product " + item.getProduct().getSku());
                }
            }
        }

        if (operation.getType() == OperationType.INCOME || operation.getType() == OperationType.MOVE) {
            for (OperationItem item : operation.getItems()) {
                validateTargetCell(item);
            }
        }
    }

    private void validateTargetCell(OperationItem item) {
        StorageCell targetCell = item.getToCell();
        Product product = item.getProduct();

        if (product.getLengthCm().compareTo(targetCell.getLengthCm()) > 0
                || product.getWidthCm().compareTo(targetCell.getWidthCm()) > 0
                || product.getHeightCm().compareTo(targetCell.getHeightCm()) > 0) {
            throw new StorageCapacityException("Product " + product.getSku() + " dimensions exceed cell " + targetCell.getCode());
        }

        StockBalance existingBalance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                product.getId(), targetCell.getId()).orElse(new StockBalance(product, targetCell, 0));

        if ((existingBalance.getQuantity() + item.getQuantity()) > targetCell.getCapacityUnits()) {
            throw new StorageCapacityException("Cell " + targetCell.getCode() + " has insufficient capacity for product " + product.getSku());
        }

        BigDecimal totalWeightToAdd = product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(item.getQuantity()));
        if (targetCell.getCurrentWeightKg().add(totalWeightToAdd).compareTo(targetCell.getMaxWeightKg()) > 0) {
            throw new StorageCapacityException("Cell " + targetCell.getCode() + " will exceed max weight limit after adding product " + product.getSku());
        }

        BigDecimal totalVolumeToAdd = product.getVolumePerUnitCm3().multiply(BigDecimal.valueOf(item.getQuantity()));
        if (targetCell.getCurrentVolumeCm3().add(totalVolumeToAdd).compareTo(targetCell.getMaxVolumeCm3()) > 0) {
            throw new StorageCapacityException("Cell " + targetCell.getCode() + " will exceed max volume limit after adding product " + product.getSku());
        }
    }

    private void updateStockBalanceAndCell(OperationItem item, OperationType type) {
        switch (type) {
            case INCOME -> increaseBalance(item.getProduct(), item.getToCell(), item.getQuantity());
            case OUTCOME -> decreaseBalance(item.getProduct(), item.getFromCell(), item.getQuantity());
            case MOVE -> {
                decreaseBalance(item.getProduct(), item.getFromCell(), item.getQuantity());
                increaseBalance(item.getProduct(), item.getToCell(), item.getQuantity());
            }
        }
    }

    private void increaseBalance(Product product, StorageCell cell, Integer quantity) {
        StockBalance balance = getOrCreateBalance(product, cell);
        balance.setQuantity(balance.getQuantity() + quantity);
        stockBalanceRepository.save(balance);

        cell.setCurrentWeightKg(cell.getCurrentWeightKg().add(product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(quantity))));
        cell.setCurrentVolumeCm3(cell.getCurrentVolumeCm3().add(product.getVolumePerUnitCm3().multiply(BigDecimal.valueOf(quantity))));
        storageCellRepository.save(cell);
    }

    private void decreaseBalance(Product product, StorageCell cell, Integer quantity) {
        StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId())
                .orElseThrow(() -> new InsufficientStockException("No stock to decrease"));
        balance.setQuantity(balance.getQuantity() - quantity);
        stockBalanceRepository.save(balance);

        BigDecimal weightToRemove = product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(quantity)).min(cell.getCurrentWeightKg());
        BigDecimal volumeToRemove = product.getVolumePerUnitCm3().multiply(BigDecimal.valueOf(quantity)).min(cell.getCurrentVolumeCm3());
        cell.setCurrentWeightKg(cell.getCurrentWeightKg().subtract(weightToRemove));
        cell.setCurrentVolumeCm3(cell.getCurrentVolumeCm3().subtract(volumeToRemove));
        storageCellRepository.save(cell);
    }

    private StockBalance getOrCreateBalance(Product product, StorageCell cell) {
        return stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId())
                .orElseGet(() -> new StockBalance(product, cell, 0));
    }

    private String generateOperationNumber(String type) {
        return type.toUpperCase() + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
