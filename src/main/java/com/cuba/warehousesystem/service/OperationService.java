package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.OperationItemResponse;
import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.dto.OperationResponse;
import com.cuba.warehousesystem.dto.OperationVerificationItemResponse;
import com.cuba.warehousesystem.dto.OperationVerificationRequest;
import com.cuba.warehousesystem.dto.OperationVerificationResponse;
import com.cuba.warehousesystem.dto.DocumentExecutionStepResponse;
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
import com.cuba.warehousesystem.model.DocumentExecutionStage;
import com.cuba.warehousesystem.model.DocumentExecutionStatus;
import com.cuba.warehousesystem.model.DocumentExecutionStep;
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationSource;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.OperationVerification;
import com.cuba.warehousesystem.model.OperationVerificationItem;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final DocumentExecutionStepRepository documentExecutionStepRepository;
    private final OperationVerificationRepository operationVerificationRepository;
    private final EdiMessageRepository ediMessageRepository;
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
        recordExecutionStep(saved, null, DocumentExecutionStage.DRAFT_CREATED, DocumentExecutionStatus.DONE,
                "Draft operation created", username);
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

        if (operation.getStatus() == OperationStatus.SHIPPED) {
            return finalizeShippedOperation(operation, completedBy, username, "Client receipt confirmed");
        }

        if (operation.getStatus() != OperationStatus.DRAFT) {
            throw new InvalidOperationException("Operation is already processed or cancelled.");
        }

        return completeWithQuantities(operation, completedBy, username,
                operation.getItems().stream().collect(Collectors.toMap(OperationItem::getId, OperationItem::getQuantity)),
                "Operation completed");
    }

    public OperationResponse shipOperation(Long operationId, String username) {
        Operation operation = findOperationWithItems(operationId);
        User completedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (operation.getType() != OperationType.OUTCOME) {
            throw new InvalidOperationException("Only OUTCOME operations can be shipped.");
        }
        if (operation.getStatus() != OperationStatus.DRAFT) {
            throw new InvalidOperationException("Only draft OUTCOME operations can be shipped.");
        }

        validateOperationQuantities(operation, operation.getItems().stream()
                .collect(Collectors.toMap(OperationItem::getId, OperationItem::getQuantity)));
        for (OperationItem item : operation.getItems()) {
            updateStockBalanceAndCell(operation.getId(), username, item, operation.getType(), item.getQuantity());
        }

        operation.setStatus(OperationStatus.SHIPPED);
        operation.setCompletedBy(completedBy);
        operation.setCompletedAt(LocalDateTime.now());
        Operation saved = operationRepository.save(operation);
        recordExecutionStep(saved, null, DocumentExecutionStage.STOCK_POSTED, DocumentExecutionStatus.DONE,
                "Goods shipped, stock decreased", username);
        return toResponse(saved);
    }

    public OperationResponse finalizeShippedOperation(Long operationId, String username, String details) {
        Operation operation = findOperationWithItems(operationId);
        User completedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return finalizeShippedOperation(operation, completedBy, username, details);
    }

    public OperationResponse cancelOperation(Long operationId) {
        Operation operation = findOperationWithItems(operationId);
        if (operation.getStatus() != OperationStatus.DRAFT) {
            throw new InvalidOperationException("Only draft operations can be cancelled.");
        }
        operation.setStatus(OperationStatus.CANCELLED);
        Operation saved = operationRepository.save(operation);
        recordExecutionStep(saved, null, DocumentExecutionStage.CANCELLED, DocumentExecutionStatus.DONE, "Operation cancelled", null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OperationResponse getById(Long operationId) {
        return toResponse(findOperationWithItems(operationId));
    }

    @Transactional(readOnly = true)
    public Page<OperationResponse> getAll(OperationType type, OperationStatus status, Long warehouseId, Pageable pageable) {
        Page<Operation> operations;
        if (warehouseId != null && type != null && status != null) {
            operations = operationRepository.findByWarehouse_IdAndTypeAndStatus(warehouseId, type, status, pageable);
        } else if (warehouseId != null && type != null) {
            operations = operationRepository.findByWarehouse_IdAndType(warehouseId, type, pageable);
        } else if (warehouseId != null && status != null) {
            operations = operationRepository.findByWarehouse_IdAndStatus(warehouseId, status, pageable);
        } else if (warehouseId != null) {
            operations = operationRepository.findByWarehouse_Id(warehouseId, pageable);
        } else if (type != null && status != null) {
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
    public Page<StockBalanceResponse> getStockBalance(Long warehouseId, Pageable pageable) {
        if (warehouseId != null) {
            return stockBalanceRepository.findByCell_Warehouse_Id(warehouseId, pageable).map(this::toResponse);
        }
        return stockBalanceRepository.findAll(pageable).map(this::toResponse);
    }

    public OperationVerificationResponse verifyOperation(Long operationId, OperationVerificationRequest request, String username) {
        Operation operation = findOperationWithItems(operationId);
        User completedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (operation.getStatus() != OperationStatus.DRAFT) {
            throw new InvalidOperationException("Only draft operations can be fact-checked.");
        }

        Map<Long, OperationItem> itemsById = operation.getItems().stream()
                .collect(Collectors.toMap(OperationItem::getId, Function.identity()));

        OperationVerification verification = new OperationVerification();
        verification.setOperation(operation);
        verification.setDecision(request.decision());
        verification.setComment(request.comment());
        verification.setVerifiedBy(username);
        verification.setVerifiedAt(LocalDateTime.now());

        Map<Long, Integer> actualQuantities = request.items().stream()
                .collect(Collectors.toMap(OperationVerificationRequest.Item::operationItemId, OperationVerificationRequest.Item::actualQuantity));

        for (OperationVerificationRequest.Item requestItem : request.items()) {
            OperationItem operationItem = itemsById.get(requestItem.operationItemId());
            if (operationItem == null) {
                throw new BadRequestException("Verification item does not belong to operation: " + requestItem.operationItemId());
            }
            OperationVerificationItem item = new OperationVerificationItem();
            item.setVerification(verification);
            item.setOperationItem(operationItem);
            item.setPlannedQuantity(operationItem.getQuantity());
            item.setActualQuantity(requestItem.actualQuantity());
            item.setDiscrepancyQuantity(requestItem.actualQuantity() - operationItem.getQuantity());
            item.setReason(requestItem.reason());
            verification.getItems().add(item);
        }

        if (verification.getItems().size() != operation.getItems().size()) {
            throw new BadRequestException("Fact check must contain every operation item.");
        }

        OperationVerification saved = operationVerificationRepository.save(verification);
        recordExecutionStep(operation, null, DocumentExecutionStage.FACT_CHECK, DocumentExecutionStatus.DONE,
                "Fact check decision: " + request.decision(), username);

        if (request.decision() == VerificationDecision.REJECT) {
            operation.setStatus(OperationStatus.CANCELLED);
            operation.setCompletedBy(completedBy);
            operation.setCompletedAt(LocalDateTime.now());
            operationRepository.save(operation);
            recordExecutionStep(operation, null, DocumentExecutionStage.CANCELLED, DocumentExecutionStatus.DONE,
                    "Rejected by fact check", username);
        } else {
            completeWithQuantities(operation, completedBy, username, actualQuantities,
                    request.decision() == VerificationDecision.ACCEPT_PARTIALLY ? "Partially accepted by fact check" : "Accepted by fact check");
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OperationVerificationResponse> getVerifications(Long operationId) {
        return operationVerificationRepository.findByOperation_IdOrderByVerifiedAtDesc(operationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentExecutionStepResponse> getExecutionChain(Long operationId) {
        return documentExecutionStepRepository.findByOperation_IdOrderByCreatedAtAsc(operationId).stream()
                .map(this::toResponse)
                .toList();
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
                if (item.getFromCell() == null) {
                    Long available = stockBalanceRepository.sumAvailableByProductAndWarehouse(
                            item.getProduct().getId(), operation.getWarehouse().getId());
                    if (available < item.getQuantity()) {
                        throw new InsufficientStockException("Insufficient available warehouse stock for product " + item.getProduct().getSku());
                    }
                } else {
                    StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                                    item.getProduct().getId(), item.getFromCell().getId())
                            .orElseThrow(() -> new InsufficientStockException("No stock found for product " + item.getProduct().getSku()));

                    if (balance.getQuantity() - balance.getReservedQuantity() < item.getQuantity()) {
                        throw new InsufficientStockException("Insufficient available stock for product " + item.getProduct().getSku());
                    }
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
        updateStockBalanceAndCell(operationId, username, item, type, item.getQuantity());
    }

    private void updateStockBalanceAndCell(Long operationId, String username, OperationItem item, OperationType type, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return;
        }
        switch (type) {
            case INCOME -> increaseBalance(operationId, username, type, item.getProduct(), item.getToCell(), quantity);
            case OUTCOME -> decreaseFromCellOrWarehouse(operationId, username, item, quantity);
            case MOVE -> {
                decreaseBalance(operationId, username, type, item.getProduct(), item.getFromCell(), quantity);
                increaseBalance(operationId, username, type, item.getProduct(), item.getToCell(), quantity);
            }
        }
    }

    private void decreaseFromCellOrWarehouse(Long operationId, String username, OperationItem item, Integer quantity) {
        if (item.getFromCell() != null) {
            decreaseBalance(operationId, username, OperationType.OUTCOME, item.getProduct(), item.getFromCell(), quantity);
            return;
        }

        int remaining = quantity;
        List<StockBalance> balances = stockBalanceRepository
                .findByProduct_IdAndCell_Warehouse_Id(item.getProduct().getId(), item.getOperation().getWarehouse().getId()).stream()
                .sorted(Comparator.comparing(balance -> balance.getCell().getCode()))
                .toList();
        for (StockBalance balance : balances) {
            int available = balance.getQuantity() - balance.getReservedQuantity();
            if (available <= 0) {
                continue;
            }
            int picked = Math.min(remaining, available);
            decreaseBalance(operationId, username, OperationType.OUTCOME, item.getProduct(), balance.getCell(), picked);
            remaining -= picked;
            if (remaining == 0) {
                return;
            }
        }
        throw new InsufficientStockException("Insufficient available warehouse stock for product " + item.getProduct().getSku());
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

    private OperationResponse completeWithQuantities(
            Operation operation,
            User completedBy,
            String username,
            Map<Long, Integer> quantitiesByItemId,
            String completionDetails
    ) {
        validateOperationQuantities(operation, quantitiesByItemId);

        for (OperationItem item : operation.getItems()) {
            updateStockBalanceAndCell(operation.getId(), username, item, operation.getType(), quantitiesByItemId.get(item.getId()));
        }

        operation.setStatus(OperationStatus.COMPLETED);
        operation.setCompletedBy(completedBy);
        operation.setCompletedAt(LocalDateTime.now());

        Operation saved = operationRepository.save(operation);
        recordExecutionStep(saved, null, DocumentExecutionStage.STOCK_POSTED, DocumentExecutionStatus.DONE,
                "Stock posted", username);
        recordExecutionStep(saved, null, DocumentExecutionStage.COMPLETED, DocumentExecutionStatus.DONE,
                completionDetails, username);
        if (saved.getSource() == OperationSource.EDI && saved.getType() == OperationType.INCOME) {
            markRelatedEdiMessageCompleted(saved);
        }
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

    private OperationResponse finalizeShippedOperation(Operation operation, User completedBy, String username, String details) {
        if (operation.getStatus() != OperationStatus.SHIPPED) {
            throw new InvalidOperationException("Only shipped OUTCOME operations can be completed.");
        }
        operation.setStatus(OperationStatus.COMPLETED);
        operation.setCompletedBy(completedBy);
        operation.setCompletedAt(LocalDateTime.now());
        Operation saved = operationRepository.save(operation);
        recordExecutionStep(saved, null, DocumentExecutionStage.COMPLETED, DocumentExecutionStatus.DONE,
                details, username);
        markRelatedEdiMessageCompleted(saved);
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

    private void markRelatedEdiMessageCompleted(Operation operation) {
        ediMessageRepository.findByRelatedOperation_Id(operation.getId()).ifPresent(message -> {
            message.setStatus(EdiMessageStatus.COMPLETED);
            message.setProcessedAt(LocalDateTime.now());
            message.setErrorMessage(null);
            ediMessageRepository.save(message);
        });
    }

    private void validateOperationQuantities(Operation operation, Map<Long, Integer> quantitiesByItemId) {
        if (operation.getItems().isEmpty()) {
            throw new BadRequestException("Operation must contain at least one item.");
        }

        for (OperationItem item : operation.getItems()) {
            Integer quantity = quantitiesByItemId.get(item.getId());
            if (quantity == null || quantity < 0) {
                throw new BadRequestException("Actual quantity must be zero or positive for item " + item.getId());
            }
            if (operation.getType() == OperationType.MOVE && quantity == 0) {
                continue;
            }
            if ((operation.getType() == OperationType.INCOME || operation.getType() == OperationType.MOVE) && quantity > 0) {
                validateTargetCell(item, quantity);
            }
            if ((operation.getType() == OperationType.OUTCOME || operation.getType() == OperationType.MOVE) && quantity > 0) {
                if (item.getFromCell() == null) {
                    Long available = stockBalanceRepository.sumAvailableByProductAndWarehouse(
                            item.getProduct().getId(), operation.getWarehouse().getId());
                    if (available < quantity) {
                        throw new InsufficientStockException("Insufficient available warehouse stock for product " + item.getProduct().getSku());
                    }
                } else {
                    StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(
                                    item.getProduct().getId(), item.getFromCell().getId())
                            .orElseThrow(() -> new InsufficientStockException("No stock found for product " + item.getProduct().getSku()));
                    if (balance.getQuantity() - balance.getReservedQuantity() < quantity) {
                        throw new InsufficientStockException("Insufficient available stock for product " + item.getProduct().getSku());
                    }
                }
            }
        }
    }

    private void validateTargetCell(OperationItem item, Integer quantity) {
        Integer plannedQuantity = item.getQuantity();
        item.setQuantity(quantity);
        try {
            validateTargetCell(item);
        } finally {
            item.setQuantity(plannedQuantity);
        }
    }

    public void recordExecutionStep(
            Operation operation,
            com.cuba.warehousesystem.model.EdiMessage ediMessage,
            DocumentExecutionStage stage,
            DocumentExecutionStatus status,
            String details,
            String username
    ) {
        DocumentExecutionStep step = new DocumentExecutionStep();
        step.setOperation(operation);
        step.setEdiMessage(ediMessage);
        step.setStage(stage);
        step.setStatus(status);
        step.setDetails(details);
        step.setCreatedBy(username);
        step.setCreatedAt(LocalDateTime.now());
        documentExecutionStepRepository.save(step);
    }

    public OperationResponse toResponse(Operation operation) {
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

    private OperationVerificationResponse toResponse(OperationVerification verification) {
        return new OperationVerificationResponse(
                verification.getId(),
                verification.getOperation().getId(),
                verification.getDecision(),
                verification.getComment(),
                verification.getVerifiedBy(),
                verification.getVerifiedAt(),
                verification.getItems().stream().map(this::toResponse).toList()
        );
    }

    private OperationVerificationItemResponse toResponse(OperationVerificationItem item) {
        OperationItem operationItem = item.getOperationItem();
        return new OperationVerificationItemResponse(
                item.getId(),
                operationItem.getId(),
                operationItem.getProduct().getId(),
                operationItem.getProduct().getSku(),
                operationItem.getProduct().getName(),
                item.getPlannedQuantity(),
                item.getActualQuantity(),
                item.getDiscrepancyQuantity(),
                item.getReason()
        );
    }

    private DocumentExecutionStepResponse toResponse(DocumentExecutionStep step) {
        return new DocumentExecutionStepResponse(
                step.getId(),
                step.getOperation() == null ? null : step.getOperation().getId(),
                step.getEdiMessage() == null ? null : step.getEdiMessage().getId(),
                step.getStage(),
                step.getStatus(),
                step.getDetails(),
                step.getCreatedBy(),
                step.getCreatedAt()
        );
    }

    private String generateOperationNumber(String type) {
        return type.toUpperCase() + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
