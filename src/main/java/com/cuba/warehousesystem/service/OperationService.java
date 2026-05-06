package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.OperationItemResponse;
import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.dto.OperationResponse;
import com.cuba.warehousesystem.dto.StockBalanceResponse;
import com.cuba.warehousesystem.event.OperationCompletedEvent;
import com.cuba.warehousesystem.event.OperationCreatedEvent;
import com.cuba.warehousesystem.event.StockBalanceChangedEvent;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.exception.InsufficientStockException;
import com.cuba.warehousesystem.exception.InvalidOperationException;
import com.cuba.warehousesystem.exception.StorageCapacityException;
import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationSource;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private final ApplicationEventPublisher eventPublisher;

    public OperationResponse createDraftOperation(OperationRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        Counterparty counterparty = request.counterpartyId() == null
                ? null
                : counterpartyRepository.findById(request.counterpartyId())
                .orElseThrow(() -> new EntityNotFoundException("Counterparty not found"));

        Operation operation = new Operation();
        operation.setOperationNumber(generateOperationNumber(request.type().name()));
        operation.setType(request.type());
        operation.setStatus(OperationStatus.DRAFT);
        operation.setWarehouse(warehouse);
        operation.setCreatedBy(user);
        operation.setCounterparty(counterparty);
        operation.setSource(request.source() == null ? OperationSource.MANUAL : request.source());
        operation.setExternalDocumentNumber(request.externalDocumentNumber());
        operation.setDocumentDate(request.documentDate());
        operation.setComment(request.comment());

        for (OperationRequest.ItemRequest itemReq : request.items()) {
            OperationItem item = buildItem(itemReq, operation, warehouse, request.type());
            operation.getItems().add(item);
        }

        Operation saved = operationRepository.save(operation);
        eventPublisher.publishEvent(new OperationCreatedEvent(
                saved.getId(),
                saved.getOperationNumber(),
                saved.getType(),
                saved.getWarehouse().getId(),
                username,
                LocalDateTime.now()
        ));
        return toResponse(saved);
    }

    public OperationResponse completeOperation(Long operationId, String username) {
        Operation operation = findOperationWithItems(operationId);
        User completedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (operation.getStatus() != OperationStatus.DRAFT) {
            throw new InvalidOperationException("Operation is already processed or cancelled.");
        }

        validateOperation(operation);

        for (OperationItem item : operation.getItems()) {
            updateStockBalanceAndCell(operation.getId(), username, item, operation.getType());
        }

        operation.setStatus(OperationStatus.COMPLETED);
        operation.setCompletedBy(completedBy);
        operation.setCompletedAt(LocalDateTime.now());

        Operation saved = operationRepository.save(operation);
        eventPublisher.publishEvent(new OperationCompletedEvent(
                saved.getId(),
                saved.getOperationNumber(),
                saved.getType(),
                saved.getWarehouse().getId(),
                username,
                LocalDateTime.now()
        ));
        return toResponse(saved);
    }

    public OperationResponse cancelOperation(Long operationId) {
        Operation operation = findOperationWithItems(operationId);
        if (operation.getStatus() != OperationStatus.DRAFT) {
            throw new InvalidOperationException("Only draft operations can be cancelled.");
        }
        operation.setStatus(OperationStatus.CANCELLED);
        return toResponse(operationRepository.save(operation));
    }

    @Transactional(readOnly = true)
    public OperationResponse getById(Long operationId) {
        return toResponse(findOperationWithItems(operationId));
    }

    @Transactional(readOnly = true)
    public Page<OperationResponse> getAll(OperationType type, OperationStatus status, Pageable pageable) {
        Page<Operation> operations;
        if (type != null && status != null) {
            operations = operationRepository.findByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            operations = operationRepository.findByType(type, pageable);
        } else if (status != null) {
            operations = operationRepository.findByStatus(status, pageable);
        } else {
            operations = operationRepository.findAll(pageable);
        }
        return operations.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockBalanceResponse> getStockBalance(Pageable pageable) {
        return stockBalanceRepository.findAll(pageable).map(this::toResponse);
    }

    private OperationItem buildItem(
            OperationRequest.ItemRequest itemReq,
            Operation operation,
            Warehouse warehouse,
            OperationType type
    ) {
        Product product = productRepository.findById(itemReq.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        OperationItem item = new OperationItem();
        item.setOperation(operation);
        item.setProduct(product);
        item.setQuantity(itemReq.quantity());
        item.setUnitPrice(itemReq.unitPrice() == null ? BigDecimal.ZERO : itemReq.unitPrice());
        item.setUnitOfMeasure(defaultUnitOfMeasure(itemReq.unitOfMeasure(), product));

        if (itemReq.fromCellId() != null) {
            item.setFromCell(findCellInWarehouse(itemReq.fromCellId(), warehouse, "Source cell not found"));
        }
        if (itemReq.toCellId() != null) {
            item.setToCell(findCellInWarehouse(itemReq.toCellId(), warehouse, "Target cell not found"));
        }

        validateItemShape(type, item);
        return item;
    }

    private void validateItemShape(OperationType type, OperationItem item) {
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BadRequestException("Operation item quantity must be positive.");
        }
        if (type == OperationType.INCOME && item.getToCell() == null) {
            throw new BadRequestException("INCOME operation item requires toCellId.");
        }
        if (type == OperationType.OUTCOME && item.getFromCell() == null) {
            throw new BadRequestException("OUTCOME operation item requires fromCellId.");
        }
        if (type == OperationType.MOVE) {
            if (item.getFromCell() == null || item.getToCell() == null) {
                throw new BadRequestException("MOVE operation item requires both fromCellId and toCellId.");
            }
            if (item.getFromCell().getId().equals(item.getToCell().getId())) {
                throw new BadRequestException("MOVE operation source and target cells must be different.");
            }
        }
    }

    private void validateOperation(Operation operation) {
        if (operation.getItems().isEmpty()) {
            throw new BadRequestException("Operation must contain at least one item.");
        }

        if (operation.getType() == OperationType.OUTCOME || operation.getType() == OperationType.MOVE) {
            for (OperationItem item : operation.getItems()) {
                StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                                item.getProduct().getId(), item.getFromCell().getId())
                        .orElseThrow(() -> new InsufficientStockException("No stock found for product " + item.getProduct().getSku()));

                if (balance.getQuantity() - balance.getReservedQuantity() < item.getQuantity()) {
                    throw new InsufficientStockException("Insufficient available stock for product " + item.getProduct().getSku());
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

        if (!Boolean.TRUE.equals(targetCell.getIsActive())) {
            throw new StorageCapacityException("Target cell is inactive: " + targetCell.getCode());
        }

        if (product.getLengthCm().compareTo(targetCell.getLengthCm()) > 0
                || product.getWidthCm().compareTo(targetCell.getWidthCm()) > 0
                || product.getHeightCm().compareTo(targetCell.getHeightCm()) > 0) {
            throw new StorageCapacityException("Product " + product.getSku() + " dimensions exceed cell " + targetCell.getCode());
        }

        int occupiedUnits = stockBalanceRepository.findByCell_Id(targetCell.getId()).stream()
                .mapToInt(StockBalance::getQuantity)
                .sum();
        if (occupiedUnits + item.getQuantity() > targetCell.getCapacityUnits()) {
            throw new StorageCapacityException("Cell " + targetCell.getCode() + " has insufficient unit capacity for product " + product.getSku());
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

    private void updateStockBalanceAndCell(Long operationId, String username, OperationItem item, OperationType type) {
        switch (type) {
            case INCOME -> increaseBalance(operationId, username, type, item.getProduct(), item.getToCell(), item.getQuantity());
            case OUTCOME -> decreaseBalance(operationId, username, type, item.getProduct(), item.getFromCell(), item.getQuantity());
            case MOVE -> {
                decreaseBalance(operationId, username, type, item.getProduct(), item.getFromCell(), item.getQuantity());
                increaseBalance(operationId, username, type, item.getProduct(), item.getToCell(), item.getQuantity());
            }
        }
    }

    private void increaseBalance(Long operationId, String username, OperationType operationType, Product product, StorageCell cell, Integer quantity) {
        StockBalance balance = getOrCreateBalance(product, cell);
        int previousQuantity = balance.getQuantity();
        balance.setQuantity(balance.getQuantity() + quantity);
        stockBalanceRepository.save(balance);

        cell.setCurrentWeightKg(cell.getCurrentWeightKg().add(product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(quantity))));
        cell.setCurrentVolumeCm3(cell.getCurrentVolumeCm3().add(product.getVolumePerUnitCm3().multiply(BigDecimal.valueOf(quantity))));
        storageCellRepository.save(cell);
        publishStockBalanceChanged(operationId, username, operationType, product, cell, previousQuantity, balance.getQuantity());
    }

    private void decreaseBalance(Long operationId, String username, OperationType operationType, Product product, StorageCell cell, Integer quantity) {
        StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId())
                .orElseThrow(() -> new InsufficientStockException("No stock to decrease"));
        if (balance.getQuantity() - balance.getReservedQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient available stock for product " + product.getSku());
        }

        int previousQuantity = balance.getQuantity();
        balance.setQuantity(balance.getQuantity() - quantity);
        stockBalanceRepository.save(balance);

        BigDecimal weightToRemove = product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(quantity)).min(cell.getCurrentWeightKg());
        BigDecimal volumeToRemove = product.getVolumePerUnitCm3().multiply(BigDecimal.valueOf(quantity)).min(cell.getCurrentVolumeCm3());
        cell.setCurrentWeightKg(cell.getCurrentWeightKg().subtract(weightToRemove));
        cell.setCurrentVolumeCm3(cell.getCurrentVolumeCm3().subtract(volumeToRemove));
        storageCellRepository.save(cell);
        publishStockBalanceChanged(operationId, username, operationType, product, cell, previousQuantity, balance.getQuantity());
    }

    private void publishStockBalanceChanged(
            Long operationId,
            String username,
            OperationType operationType,
            Product product,
            StorageCell cell,
            Integer previousQuantity,
            Integer newQuantity
    ) {
        eventPublisher.publishEvent(new StockBalanceChangedEvent(
                operationId,
                product.getId(),
                cell.getId(),
                previousQuantity,
                newQuantity,
                operationType,
                username,
                LocalDateTime.now()
        ));
    }

    private StockBalance getOrCreateBalance(Product product, StorageCell cell) {
        return stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId())
                .orElseGet(() -> new StockBalance(product, cell, 0));
    }

    private Operation findOperationWithItems(Long operationId) {
        return operationRepository.findWithItemsById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("Operation not found"));
    }

    private StorageCell findCellInWarehouse(Long cellId, Warehouse warehouse, String notFoundMessage) {
        StorageCell cell = storageCellRepository.findById(cellId)
                .orElseThrow(() -> new EntityNotFoundException(notFoundMessage));
        if (!cell.getWarehouse().getId().equals(warehouse.getId())) {
            throw new BadRequestException("Cell " + cell.getCode() + " does not belong to operation warehouse " + warehouse.getCode());
        }
        return cell;
    }

    private String defaultUnitOfMeasure(String requestedUom, Product product) {
        if (requestedUom != null && !requestedUom.isBlank()) {
            return requestedUom;
        }
        return product.getUnitOfMeasure() == null ? "pcs" : product.getUnitOfMeasure();
    }

    private OperationResponse toResponse(Operation operation) {
        List<OperationItemResponse> itemResponses = operation.getItems().stream()
                .map(this::toResponse)
                .toList();

        return new OperationResponse(
                operation.getId(),
                operation.getOperationNumber(),
                operation.getType(),
                operation.getStatus(),
                operation.getWarehouse().getId(),
                operation.getWarehouse().getCode(),
                operation.getCreatedBy().getId(),
                operation.getCompletedBy() == null ? null : operation.getCompletedBy().getId(),
                operation.getCounterparty() == null ? null : operation.getCounterparty().getId(),
                operation.getSource(),
                operation.getExternalDocumentNumber(),
                operation.getDocumentDate(),
                operation.getComment(),
                operation.getCreatedAt(),
                operation.getCompletedAt(),
                itemResponses
        );
    }

    private OperationItemResponse toResponse(OperationItem item) {
        return new OperationItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitOfMeasure(),
                item.getFromCell() == null ? null : item.getFromCell().getId(),
                item.getFromCell() == null ? null : item.getFromCell().getCode(),
                item.getToCell() == null ? null : item.getToCell().getId(),
                item.getToCell() == null ? null : item.getToCell().getCode()
        );
    }

    private StockBalanceResponse toResponse(StockBalance balance) {
        return new StockBalanceResponse(
                balance.getProduct().getId(),
                balance.getProduct().getSku(),
                balance.getProduct().getName(),
                balance.getCell().getId(),
                balance.getCell().getCode(),
                balance.getCell().getWarehouse().getId(),
                balance.getCell().getWarehouse().getCode(),
                balance.getQuantity(),
                balance.getReservedQuantity(),
                balance.getQuantity() - balance.getReservedQuantity(),
                balance.getUpdatedAt()
        );
    }

    private String generateOperationNumber(String type) {
        return type.toUpperCase() + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
