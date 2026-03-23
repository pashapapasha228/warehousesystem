package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.exception.InsufficientStockException;
import com.cuba.warehousesystem.exception.StorageCapacityException;
import com.cuba.warehousesystem.model.*;
import com.cuba.warehousesystem.repository.*;
import jakarta.persistence.EntityNotFoundException;
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

        public Operation createDraftOperation(OperationRequest request, String username) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

            Operation operation = new Operation();
            operation.setOperationNumber(generateOperationNumber(request.type()));
            operation.setType(OperationType.valueOf(request.type()));
            operation.setStatus(OperationStatus.DRAFT);
            operation.setWarehouse(warehouse);
            operation.setCreatedBy(user);

            for (OperationRequest.ItemRequest itemReq : request.items()) {
                OperationItem item = new OperationItem();
                item.setProduct(productRepository.findById(itemReq.productId()).orElse(null));
                item.setQuantity(itemReq.quantity());

                if (itemReq.fromCellId() != null) {
                    item.setFromCell(storageCellRepository.findById(itemReq.fromCellId()).orElse(null));
                }
                if (itemReq.toCellId() != null) {
                    item.setToCell(storageCellRepository.findById(itemReq.toCellId()).orElse(null));
                }

                item.setOperation(operation);
                operation.getItems().add(item);
            }

            return operationRepository.save(operation);
        }

        public Operation completeOperation(Long operationId, String username) {
            Operation operation = operationRepository.findById(operationId)
                    .orElseThrow(() -> new EntityNotFoundException("Operation not found"));

            if (operation.getStatus() != OperationStatus.DRAFT) {
                throw new IllegalStateException("Operation is already processed or cancelled.");
            }

            // Проверка остатков для операций расхода
            if (operation.getType() == OperationType.OUTCOME) {
                for (OperationItem item : operation.getItems()) {
                    StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                                    item.getProduct().getId(), item.getFromCell().getId())
                            .orElseThrow(() -> new InsufficientStockException("No stock found for product " + item.getProduct().getSku()));

                    if (balance.getQuantity() < item.getQuantity()) {
                        throw new InsufficientStockException("Insufficient stock for product " + item.getProduct().getSku());
                    }
                }
            }

            // Проверка вместимости ячеек для операций прихода и перемещения
            if (operation.getType() == OperationType.INCOME || operation.getType() == OperationType.MOVE) {
                for (OperationItem item : operation.getItems()) {
                    StorageCell targetCell = item.getToCell(); // Ячейка, в которую идёт товар
                    Product product = item.getProduct();

                    // --- НОВАЯ ПРОВЕРКА: Физические габариты ---
                    if (product.getLengthPerUnitCm().compareTo(targetCell.getLengthCm()) > 0 ||
                            product.getWidthPerUnitCm().compareTo(targetCell.getWidthCm()) > 0 ||
                            product.getHeightPerUnitCm().compareTo(targetCell.getHeightCm()) > 0) {
                        throw new StorageCapacityException(
                                String.format("Product %s dimensions (%.2f x %.2f x %.2f cm) exceed Cell %s dimensions (%.2f x %.2f x %.2f cm)",
                                        product.getSku(),
                                        product.getLengthPerUnitCm(), product.getWidthPerUnitCm(), product.getHeightPerUnitCm(),
                                        targetCell.getCode(),
                                        targetCell.getLengthCm(), targetCell.getWidthCm(), targetCell.getHeightCm()
                                )
                        );
                    }
                    // -----------------------------------------

                    // Проверка общего места (capacity)
                    StockBalance existingBalance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                            product.getId(), targetCell.getId()).orElse(new StockBalance(product, targetCell, 0));

                    if ((existingBalance.getQuantity() + item.getQuantity()) > targetCell.getCapacity()) {
                        throw new StorageCapacityException("Cell " + targetCell.getCode() + " has insufficient capacity for product " + product.getSku());
                    }

                    // Проверка веса
                    BigDecimal totalWeightToAdd = product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(item.getQuantity()));
                    if ((targetCell.getCurrentWeightKg().add(totalWeightToAdd)).compareTo(targetCell.getMaxWeightKg()) > 0) {
                        throw new StorageCapacityException("Cell " + targetCell.getCode() + " will exceed max weight limit after adding product " + product.getSku());
                    }

                    // Проверка объёма (остаётся как дополнительная мера, но не заменяет проверку габаритов)
                    BigDecimal totalVolumeToAdd = product.getVolumePerUnitCubicCm().multiply(BigDecimal.valueOf(item.getQuantity()));
                    if ((targetCell.getCurrentVolumeCubicCm().add(totalVolumeToAdd)).compareTo(targetCell.getCalculatedVolumeCubicCm()) > 0) {
                        throw new StorageCapacityException("Cell " + targetCell.getCode() + " will exceed max volume limit after adding product " + product.getSku());
                    }
                }
            }


            // Обновление остатков и ячеек
            for (OperationItem item : operation.getItems()) {
                updateStockBalanceAndCell(item, operation.getType());
            }

            // Финализация операции
            operation.setStatus(OperationStatus.COMPLETED);
            operation.setCompletedAt(LocalDateTime.now());

            return operationRepository.save(operation);
        }

        private void updateStockBalanceAndCell(OperationItem item, OperationType type) {
            switch (type) {
                case INCOME -> {
                    StockBalance balance = getOrCreateBalance(item.getProduct(), item.getToCell());
                    balance.setQuantity(balance.getQuantity() + item.getQuantity());
                    stockBalanceRepository.save(balance);

                    // Обновляем текущий вес и объём в ячейке
                    StorageCell cell = item.getToCell();
                    BigDecimal weightToAdd = item.getProduct().getWeightPerUnitKg().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal volumeToAdd = item.getProduct().getVolumePerUnitCubicCm().multiply(BigDecimal.valueOf(item.getQuantity()));
                    cell.setCurrentWeightKg(cell.getCurrentWeightKg().add(weightToAdd));
                    cell.setCurrentVolumeCubicCm(cell.getCurrentVolumeCubicCm().add(volumeToAdd));
                    storageCellRepository.save(cell);
                }
                case OUTCOME -> {
                    StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                                    item.getProduct().getId(), item.getFromCell().getId())
                            .orElseThrow(() -> new InsufficientStockException("No stock to decrease"));
                    balance.setQuantity(Math.max(0, balance.getQuantity() - item.getQuantity())); // Защита от отрицательных значений
                    stockBalanceRepository.save(balance);

                    // Обновляем текущий вес и объём в ячейке
                    StorageCell cell = item.getFromCell();
                    BigDecimal weightToRemove = item.getProduct().getWeightPerUnitKg().multiply(BigDecimal.valueOf(item.getQuantity()));
                    weightToRemove = weightToRemove.min(cell.getCurrentWeightKg()); // Защита от отрицательных
                    BigDecimal volumeToRemove = item.getProduct().getVolumePerUnitCubicCm().multiply(BigDecimal.valueOf(item.getQuantity()));
                    volumeToRemove = volumeToRemove.min(cell.getCurrentVolumeCubicCm()); // Защита от отрицательных
                    cell.setCurrentWeightKg(cell.getCurrentWeightKg().subtract(weightToRemove));
                    cell.setCurrentVolumeCubicCm(cell.getCurrentVolumeCubicCm().subtract(volumeToRemove));
                    storageCellRepository.save(cell);
                }
                case MOVE -> {
                    // Уменьшаем из одной ячейки (аналогично OUTCOME)
                    StockBalance fromBalance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                                    item.getProduct().getId(), item.getFromCell().getId())
                            .orElseThrow(() -> new InsufficientStockException("No stock to move"));
                    fromBalance.setQuantity(Math.max(0, fromBalance.getQuantity() - item.getQuantity()));
                    stockBalanceRepository.save(fromBalance);

                    StorageCell fromCell = item.getFromCell();
                    BigDecimal weightToRemove = item.getProduct().getWeightPerUnitKg().multiply(BigDecimal.valueOf(item.getQuantity()));
                    weightToRemove = weightToRemove.min(fromCell.getCurrentWeightKg());
                    BigDecimal volumeToRemove = item.getProduct().getVolumePerUnitCubicCm().multiply(BigDecimal.valueOf(item.getQuantity()));
                    volumeToRemove = volumeToRemove.min(fromCell.getCurrentVolumeCubicCm());
                    fromCell.setCurrentWeightKg(fromCell.getCurrentWeightKg().subtract(weightToRemove));
                    fromCell.setCurrentVolumeCubicCm(fromCell.getCurrentVolumeCubicCm().subtract(volumeToRemove));
                    storageCellRepository.save(fromCell);

                    // Увеличиваем в другой ячейке (аналогично INCOME)
                    StockBalance toBalance = getOrCreateBalance(item.getProduct(), item.getToCell());
                    toBalance.setQuantity(toBalance.getQuantity() + item.getQuantity());
                    stockBalanceRepository.save(toBalance);

                    StorageCell toCell = item.getToCell();
                    BigDecimal weightToAdd = item.getProduct().getWeightPerUnitKg().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal volumeToAdd = item.getProduct().getVolumePerUnitCubicCm().multiply(BigDecimal.valueOf(item.getQuantity()));
                    toCell.setCurrentWeightKg(toCell.getCurrentWeightKg().add(weightToAdd));
                    toCell.setCurrentVolumeCubicCm(toCell.getCurrentVolumeCubicCm().add(volumeToAdd));
                    storageCellRepository.save(toCell);
                }
            }
        }

        private StockBalance getOrCreateBalance(Product product, StorageCell cell) {
            return stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId())
                    .orElseGet(() -> {
                        StockBalance newBalance = new StockBalance();
                        newBalance.setProduct(product);
                        newBalance.setCell(cell);
                        newBalance.setQuantity(0);
                        return newBalance;
                    });
        }

        private String generateOperationNumber(String type) {
            return type.toUpperCase() + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
}
