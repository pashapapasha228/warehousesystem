package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.EdiAuditLogResponse;
import com.cuba.warehousesystem.dto.EdiCustomerReceiptRequest;
import com.cuba.warehousesystem.dto.EdiProcessRequest;
import com.cuba.warehousesystem.dto.EdiSimulationRequest;
import com.cuba.warehousesystem.dto.EdiMessageReceiveRequest;
import com.cuba.warehousesystem.dto.EdiMessageResponse;
import com.cuba.warehousesystem.dto.EdiProcessResultResponse;
import com.cuba.warehousesystem.dto.EdiProcessingQueueResponse;
import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.dto.OperationResponse;
import com.cuba.warehousesystem.model.CounterpartyType;
import com.cuba.warehousesystem.model.DocumentExecutionStage;
import com.cuba.warehousesystem.model.DocumentExecutionStatus;
import com.cuba.warehousesystem.event.EdiMessageProcessedEvent;
import com.cuba.warehousesystem.event.EdiMessageReceivedEvent;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.EdiAuditLog;
import com.cuba.warehousesystem.model.EdiDirection;
import com.cuba.warehousesystem.model.EdiMappingConfig;
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiPartner;
import com.cuba.warehousesystem.model.EdiProcessingQueue;
import com.cuba.warehousesystem.model.EdiQueueStatus;
import com.cuba.warehousesystem.model.OperationSource;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.repository.EdiAuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMappingConfigRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiPartnerRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EdiProcessingService {

    private final EdiMessageRepository ediMessageRepository;
    private final EdiPartnerRepository ediPartnerRepository;
    private final EdiMappingConfigRepository ediMappingConfigRepository;
    private final EdiProcessingQueueRepository ediProcessingQueueRepository;
    private final EdiAuditLogRepository ediAuditLogRepository;
    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StorageCellRepository storageCellRepository;
    private final OperationRepository operationRepository;
    private final OperationService operationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public EdiMessageResponse receiveInbound(EdiMessageReceiveRequest request) {
        EdiPartner partner = resolvePartner(request.partnerId(), request.partnerCode());
        if (!Boolean.TRUE.equals(partner.getIsActive()) || !Boolean.TRUE.equals(partner.getInboundEnabled())) {
            throw new BadRequestException("EDI partner is not active for inbound messages: " + partner.getCode());
        }

        EdiMessage message = new EdiMessage();
        message.setMessageType(request.messageType());
        message.setDirection(EdiDirection.INBOUND);
        message.setStatus(EdiMessageStatus.RECEIVED);
        message.setInterchangeRef(request.interchangeRef());
        message.setMessageRef(request.messageRef());
        message.setDocumentNumber(request.documentNumber());
        message.setRawPayload(request.rawPayload());
        message.setNormalizedPayload(toJson(request.normalizedPayload()));
        message.setPartner(partner);
        message.setReceivedAt(LocalDateTime.now());

        EdiMessage saved = ediMessageRepository.save(message);
        eventPublisher.publishEvent(new EdiMessageReceivedEvent(
                saved.getId(),
                saved.getMessageType(),
                partner.getId(),
                saved.getMessageRef(),
                LocalDateTime.now()
        ));
        return toResponse(saved);
    }

    public EdiProcessResultResponse processQueueItem(Long queueItemId, EdiProcessRequest request, String username) {
        EdiProcessingQueue queueItem = ediProcessingQueueRepository.findById(queueItemId)
                .orElseThrow(() -> new EntityNotFoundException("EDI processing queue item not found"));
        if (queueItem.getStatus() == EdiQueueStatus.DONE) {
            return toProcessResult(queueItem);
        }

        EdiMessage message = queueItem.getEdiMessage();
        queueItem.setStatus(EdiQueueStatus.RUNNING);
        queueItem.setAttemptCount(queueItem.getAttemptCount() + 1);
        queueItem.setStartedAt(LocalDateTime.now());
        queueItem.setLastError(null);
        message.setStatus(EdiMessageStatus.PROCESSING);

        try {
            OperationResponse operationResponse = createOperationFromMessage(message, request, username);
            if (operationResponse != null) {
                com.cuba.warehousesystem.model.Operation operation = findOperationReference(operationResponse.id());
                message.setRelatedOperation(operation);
                operationService.recordExecutionStep(operation, message, DocumentExecutionStage.EDI_RECEIVED,
                        DocumentExecutionStatus.DONE, "Created from EDI message " + message.getId(), username);
            }
            message.setStatus(EdiMessageStatus.PROCESSED);
            message.setProcessedAt(LocalDateTime.now());
            message.setErrorMessage(null);
            queueItem.setStatus(EdiQueueStatus.DONE);
            queueItem.setFinishedAt(LocalDateTime.now());
            EdiProcessingQueue savedQueue = ediProcessingQueueRepository.save(queueItem);
            eventPublisher.publishEvent(new EdiMessageProcessedEvent(
                    message.getId(),
                    message.getMessageType(),
                    message.getStatus(),
                    message.getRelatedOperation() == null ? null : message.getRelatedOperation().getId(),
                    null,
                    LocalDateTime.now()
            ));
            return toProcessResult(savedQueue);
        } catch (RuntimeException ex) {
            message.setStatus(EdiMessageStatus.FAILED);
            message.setProcessedAt(LocalDateTime.now());
            message.setErrorMessage(ex.getMessage());
            queueItem.setStatus(EdiQueueStatus.FAILED);
            queueItem.setFinishedAt(LocalDateTime.now());
            queueItem.setLastError(ex.getMessage());
            EdiProcessingQueue savedQueue = ediProcessingQueueRepository.save(queueItem);
            eventPublisher.publishEvent(new EdiMessageProcessedEvent(
                    message.getId(),
                    message.getMessageType(),
                    message.getStatus(),
                    null,
                    ex.getMessage(),
                    LocalDateTime.now()
            ));
            return toProcessResult(savedQueue);
        }
    }

    public EdiProcessResultResponse processQueueItem(Long queueItemId, String username) {
        return processQueueItem(queueItemId, new EdiProcessRequest(List.of()), username);
    }

    @Transactional(readOnly = true)
    public EdiMessageResponse getMessage(Long id) {
        return toResponse(ediMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EDI message not found")));
    }

    @Transactional(readOnly = true)
    public Page<EdiMessageResponse> getMessages(EdiMessageType type, EdiMessageStatus status, Pageable pageable) {
        Page<EdiMessage> messages;
        if (type != null && status != null) {
            messages = ediMessageRepository.findByMessageTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            messages = ediMessageRepository.findByMessageType(type, pageable);
        } else if (status != null) {
            messages = ediMessageRepository.findByStatus(status, pageable);
        } else {
            messages = ediMessageRepository.findAll(pageable);
        }
        return messages.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EdiProcessingQueueResponse> getQueue(EdiQueueStatus status, Pageable pageable) {
        Page<EdiProcessingQueue> queue = status == null
                ? ediProcessingQueueRepository.findAll(pageable)
                : ediProcessingQueueRepository.findByStatus(status, pageable);
        return queue.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EdiAuditLogResponse> getAudit(Long messageId, Pageable pageable) {
        Page<EdiAuditLog> auditLogs = messageId == null
                ? ediAuditLogRepository.findAll(pageable)
                : ediAuditLogRepository.findByEdiMessage_Id(messageId, pageable);
        return auditLogs.map(this::toResponse);
    }

    public EdiMessageResponse simulateSupplierDesadv(EdiSimulationRequest request) {
        EdiPartner partner = ediPartnerRepository.findById(request.partnerId())
                .orElseThrow(() -> new EntityNotFoundException("EDI partner not found"));
        if (partner.getCounterparty() == null || partner.getCounterparty().getType() != CounterpartyType.SUPPLIER) {
            throw new BadRequestException("Supplier simulation requires an EDI partner linked to a supplier.");
        }
        return receiveInbound(new EdiMessageReceiveRequest(
                partner.getId(),
                null,
                EdiMessageType.DESADV,
                "SIM-SUP-" + System.currentTimeMillis(),
                "SIM-SUP-" + System.nanoTime(),
                firstNonBlank(request.documentNumber(), "SIM-DESADV-" + System.currentTimeMillis()),
                "{\"simulated\":true,\"actor\":\"supplier\"}",
                objectMapper.valueToTree(Map.of(
                        "warehouseId", request.warehouseId(),
                        "documentDate", LocalDate.now().toString(),
                        "items", simulationItems(partner, EdiMessageType.DESADV, request.items())
                ))
        ));
    }

    public EdiMessageResponse simulateCustomerOrders(EdiSimulationRequest request) {
        EdiPartner partner = ediPartnerRepository.findById(request.partnerId())
                .orElseThrow(() -> new EntityNotFoundException("EDI partner not found"));
        if (partner.getCounterparty() == null || partner.getCounterparty().getType() != CounterpartyType.CUSTOMER) {
            throw new BadRequestException("Customer simulation requires an EDI partner linked to a customer.");
        }
        return receiveInbound(new EdiMessageReceiveRequest(
                partner.getId(),
                null,
                EdiMessageType.ORDERS,
                "SIM-CUS-" + System.currentTimeMillis(),
                "SIM-CUS-" + System.nanoTime(),
                firstNonBlank(request.documentNumber(), "SIM-ORDERS-" + System.currentTimeMillis()),
                "{\"simulated\":true,\"actor\":\"customer\"}",
                objectMapper.valueToTree(Map.of(
                        "warehouseId", request.warehouseId(),
                        "documentDate", LocalDate.now().toString(),
                        "items", simulationItems(partner, EdiMessageType.ORDERS, request.items())
                ))
        ));
    }

    public EdiMessageResponse simulateCustomerReceipt(EdiCustomerReceiptRequest request, String username) {
        EdiMessage sourceMessage = resolveReceiptSourceMessage(request);
        if (sourceMessage.getRelatedOperation() == null) {
            throw new BadRequestException("Customer receipt requires a related OUTCOME operation.");
        }
        if (sourceMessage.getMessageType() != EdiMessageType.ORDERS) {
            throw new BadRequestException("Customer receipt can complete only ORDERS flow.");
        }

        operationService.finalizeShippedOperation(
                sourceMessage.getRelatedOperation().getId(),
                username == null ? "system" : username,
                "Customer receipt confirmation"
        );

        sourceMessage.setStatus(EdiMessageStatus.COMPLETED);
        sourceMessage.setProcessedAt(LocalDateTime.now());
        sourceMessage.setErrorMessage(null);
        EdiMessage saved = ediMessageRepository.save(sourceMessage);
        return toResponse(saved);
    }

    private EdiMessage resolveReceiptSourceMessage(EdiCustomerReceiptRequest request) {
        if (request.messageId() != null) {
            return ediMessageRepository.findById(request.messageId())
                    .orElseThrow(() -> new EntityNotFoundException("EDI message not found"));
        }
        if (request.operationId() != null) {
            return ediMessageRepository.findByRelatedOperation_Id(request.operationId())
                    .orElseThrow(() -> new EntityNotFoundException("EDI message not found for operation"));
        }
        throw new BadRequestException("messageId or operationId is required.");
    }

    private List<Map<String, Object>> simulationItems(
            EdiPartner partner,
            EdiMessageType messageType,
            List<EdiSimulationRequest.Item> requestItems
    ) {
        if (requestItems == null || requestItems.isEmpty()) {
            throw new BadRequestException("Simulation must contain at least one item.");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (EdiSimulationRequest.Item item : requestItems) {
            Map<String, Object> payloadItem = new HashMap<>();
            payloadItem.put("quantity", item.quantity());
            payloadItem.put("unitPrice", BigDecimal.ZERO);
            if (item.mappingId() != null) {
                EdiMappingConfig mapping = ediMappingConfigRepository.findById(item.mappingId())
                        .orElseThrow(() -> new EntityNotFoundException("EDI mapping not found"));
                if (!mapping.getPartner().getId().equals(partner.getId()) || mapping.getMessageType() != messageType
                        || !Boolean.TRUE.equals(mapping.getIsActive())) {
                    throw new BadRequestException("Selected mapping does not match partner/message type.");
                }
                payloadItem.put("externalProductCode", mapping.getExternalProductCode());
                payloadItem.put("unitOfMeasure", firstNonBlank(item.unitOfMeasure(), mapping.getInternalUom()));
            } else if (item.productId() != null) {
                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new EntityNotFoundException("Product not found"));
                payloadItem.put("productId", product.getId());
                payloadItem.put("unitOfMeasure", firstNonBlank(item.unitOfMeasure(), product.getUnitOfMeasure()));
            } else if (item.externalProductCode() != null && !item.externalProductCode().isBlank()) {
                payloadItem.put("externalProductCode", item.externalProductCode());
                payloadItem.put("unitOfMeasure", firstNonBlank(item.unitOfMeasure(), "pcs"));
            } else {
                throw new BadRequestException("Each simulation item requires mappingId, productId or externalProductCode.");
            }
            result.add(payloadItem);
        }
        return result;
    }

    private OperationResponse createOperationFromMessage(EdiMessage message, EdiProcessRequest request, String username) {
        ensureInboundPartnerCanBeProcessed(message);

        if (message.getDirection() == EdiDirection.OUTBOUND || message.getMessageType() == EdiMessageType.ORDRSP) {
            return null;
        }

        JsonNode payload = parsePayload(message.getNormalizedPayload());
        OperationType operationType = resolveOperationType(message);

        Long warehouseId = readLong(payload, "warehouseId");
        if (warehouseId == null && message.getPartner().getDefaultWarehouse() != null) {
            warehouseId = message.getPartner().getDefaultWarehouse().getId();
        }
        if (warehouseId == null) {
            throw new BadRequestException("EDI payload must contain warehouseId or partner default warehouse must be set.");
        }

        Long counterpartyId = readLong(payload, "counterpartyId");
        if (counterpartyId == null && message.getPartner().getCounterparty() != null) {
            counterpartyId = message.getPartner().getCounterparty().getId();
        }

        List<OperationRequest.ItemRequest> items = buildItems(message, payload, operationType, warehouseId, request);
        validateEdiOperationItems(operationType, warehouseId, items);
        OperationRequest operationRequest = new OperationRequest(
                operationType,
                warehouseId,
                counterpartyId,
                OperationSource.EDI,
                firstNonBlank(message.getDocumentNumber(), readText(payload, "documentNumber")),
                readDate(payload, "documentDate"),
                firstNonBlank(readText(payload, "comment"), "Created from EDI message " + message.getId()),
                items
        );
        return operationService.createDraftOperation(operationRequest, username == null ? "system" : username);
    }

    private void ensureInboundPartnerCanBeProcessed(EdiMessage message) {
        EdiPartner partner = message.getPartner();
        if (partner == null) {
            throw new BadRequestException("EDI message must be linked to an EDI partner.");
        }
        if (!Boolean.TRUE.equals(partner.getIsActive())) {
            throw new BadRequestException("EDI partner is inactive: " + partner.getCode());
        }
        if (message.getDirection() == EdiDirection.INBOUND && !Boolean.TRUE.equals(partner.getInboundEnabled())) {
            throw new BadRequestException("EDI partner is not enabled for inbound messages: " + partner.getCode());
        }
        if (partner.getCounterparty() == null) {
            throw new BadRequestException("EDI partner must be linked to a counterparty: " + partner.getCode());
        }
    }

    private OperationType resolveOperationType(EdiMessage message) {
        if (message.getDirection() != EdiDirection.INBOUND) {
            throw new BadRequestException("Only inbound EDI messages can create warehouse operations.");
        }

        CounterpartyType counterpartyType = message.getPartner().getCounterparty().getType();
        if (message.getMessageType() == EdiMessageType.DESADV && counterpartyType == CounterpartyType.SUPPLIER) {
            return OperationType.INCOME;
        }
        if (message.getMessageType() == EdiMessageType.ORDERS && counterpartyType == CounterpartyType.CUSTOMER) {
            return OperationType.OUTCOME;
        }

        throw new BadRequestException(
                "Unsupported EDI scenario: " + message.getMessageType()
                        + " " + message.getDirection()
                        + " from " + counterpartyType
                        + ". Expected DESADV from SUPPLIER or ORDERS from CUSTOMER."
        );
    }

    private List<OperationRequest.ItemRequest> buildItems(
            EdiMessage message,
            JsonNode payload,
            OperationType operationType,
            Long warehouseId,
            EdiProcessRequest request
    ) {
        JsonNode itemNodes = payload.get("items");
        if (itemNodes == null || !itemNodes.isArray() || itemNodes.isEmpty()) {
            throw new BadRequestException("EDI payload must contain non-empty items array.");
        }

        Map<Integer, List<EdiProcessRequest.CellAssignment>> cellAssignments = request == null || request.cellAssignments() == null
                ? Map.of()
                : request.cellAssignments().stream()
                .collect(Collectors.groupingBy(EdiProcessRequest.CellAssignment::itemIndex));

        List<OperationRequest.ItemRequest> items = new ArrayList<>();
        int index = 0;
        for (JsonNode itemNode : itemNodes) {
            Product product = resolveProduct(message, itemNode);
            Integer totalQuantity = readRequiredInt(itemNode, "quantity");
            Long fromCellId = readLong(itemNode, "fromCellId");
            Long toCellId = readLong(itemNode, "toCellId");
            BigDecimal unitPrice = readBigDecimal(itemNode, "unitPrice");
            String unitOfMeasure = firstNonBlank(readText(itemNode, "unitOfMeasure"), product.getUnitOfMeasure());
            List<EdiProcessRequest.CellAssignment> assignments = cellAssignments.getOrDefault(index, List.of());

            if (!assignments.isEmpty()) {
                addAssignedOperationItems(items, operationType, product, totalQuantity, unitPrice, unitOfMeasure, assignments);
            } else {
                items.add(new OperationRequest.ItemRequest(
                        product.getId(),
                        totalQuantity,
                        unitPrice,
                        unitOfMeasure,
                        fromCellId,
                        toCellId
                ));
            }
            index++;
        }
        return items;
    }

    private void addAssignedOperationItems(
            List<OperationRequest.ItemRequest> items,
            OperationType operationType,
            Product product,
            Integer totalQuantity,
            BigDecimal unitPrice,
            String unitOfMeasure,
            List<EdiProcessRequest.CellAssignment> assignments
    ) {
        Map<Long, Integer> quantitiesByCell = new HashMap<>();
        for (EdiProcessRequest.CellAssignment assignment : assignments) {
            if (assignment.cellId() == null) {
                throw new BadRequestException("EDI cell assignment must contain cellId.");
            }
            Integer quantity = assignment.quantity() == null && assignments.size() == 1
                    ? totalQuantity
                    : assignment.quantity();
            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("EDI cell assignment quantity must be positive.");
            }
            quantitiesByCell.merge(assignment.cellId(), quantity, Integer::sum);
        }

        int assignedQuantity = quantitiesByCell.values().stream().mapToInt(Integer::intValue).sum();
        if (assignedQuantity != totalQuantity) {
            throw new BadRequestException("EDI cell assignment quantity must match item quantity for product "
                    + product.getSku() + ": expected " + totalQuantity + ", got " + assignedQuantity);
        }

        for (Map.Entry<Long, Integer> entry : quantitiesByCell.entrySet()) {
            Long cellId = entry.getKey();
            items.add(new OperationRequest.ItemRequest(
                    product.getId(),
                    entry.getValue(),
                    unitPrice,
                    unitOfMeasure,
                    operationType == OperationType.OUTCOME ? cellId : null,
                    operationType == OperationType.INCOME ? cellId : null
            ));
        }
    }

    private void validateEdiOperationItems(
            OperationType operationType,
            Long warehouseId,
            List<OperationRequest.ItemRequest> items
    ) {
        for (OperationRequest.ItemRequest item : items) {
            if (operationType == OperationType.INCOME) {
                if (item.toCellId() == null) {
                    throw new BadRequestException("Select target cell (toCellId) for every DESADV item before processing.");
                }
                ensureCellBelongsToWarehouse(item.toCellId(), warehouseId, "EDI DESADV item target cell");
            }
            if (operationType == OperationType.OUTCOME) {
                if (item.fromCellId() != null) {
                    ensureCellBelongsToWarehouse(item.fromCellId(), warehouseId, "EDI ORDERS item source cell");
                    ensureAvailableStock(item);
                } else {
                    Long available = stockBalanceRepository.sumAvailableByProductAndWarehouse(item.productId(), warehouseId);
                    if (available < item.quantity()) {
                        throw new BadRequestException("Insufficient available warehouse stock for EDI ORDERS productId " + item.productId());
                    }
                }
            }
        }
    }

    private void ensureCellBelongsToWarehouse(Long cellId, Long warehouseId, String context) {
        storageCellRepository.findById(cellId)
                .filter(cell -> cell.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new BadRequestException(context + " not found in warehouse: " + cellId));
    }

    private void ensureAvailableStock(OperationRequest.ItemRequest item) {
        StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(item.productId(), item.fromCellId())
                .orElseThrow(() -> new BadRequestException("No stock found for EDI ORDERS productId "
                        + item.productId() + " in fromCellId " + item.fromCellId()));
        if (balance.getQuantity() - balance.getReservedQuantity() < item.quantity()) {
            throw new BadRequestException("Insufficient available stock for EDI ORDERS productId "
                    + item.productId() + " in fromCellId " + item.fromCellId());
        }
    }

    private Product resolveProduct(EdiMessage message, JsonNode itemNode) {
        Long productId = readLong(itemNode, "productId");
        if (productId != null) {
            return productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        }

        String externalProductCode = readText(itemNode, "externalProductCode");
        if (externalProductCode == null || externalProductCode.isBlank()) {
            throw new BadRequestException("EDI item must contain externalProductCode or productId.");
        }

        EdiMappingConfig mapping = ediMappingConfigRepository
                .findByPartner_IdAndMessageTypeAndExternalProductCodeAndIsActiveTrue(
                        message.getPartner().getId(),
                        message.getMessageType(),
                        externalProductCode)
                .orElseThrow(() -> new BadRequestException("No active EDI product mapping for external code: " + externalProductCode));
        return mapping.getInternalProduct();
    }

    private EdiPartner resolvePartner(Long partnerId, String partnerCode) {
        if (partnerId != null) {
            return ediPartnerRepository.findById(partnerId)
                    .orElseThrow(() -> new EntityNotFoundException("EDI partner not found"));
        }
        if (partnerCode != null && !partnerCode.isBlank()) {
            return ediPartnerRepository.findByCode(partnerCode)
                    .orElseThrow(() -> new EntityNotFoundException("EDI partner not found"));
        }
        throw new BadRequestException("partnerId or partnerCode is required.");
    }

    private com.cuba.warehousesystem.model.Operation findOperationReference(Long operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("Operation not found"));
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid normalized EDI payload JSON.");
        }
    }

    private String toJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid normalized EDI payload.");
        }
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long readLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private Integer readRequiredInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asInt() <= 0) {
            throw new BadRequestException("EDI item field must be a positive integer: " + field);
        }
        return value.asInt();
    }

    private BigDecimal readBigDecimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    private LocalDate readDate(JsonNode node, String field) {
        String value = readText(node, field);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private EdiMessageResponse toResponse(EdiMessage message) {
        return new EdiMessageResponse(
                message.getId(),
                message.getMessageType(),
                message.getDirection(),
                message.getStatus(),
                message.getInterchangeRef(),
                message.getMessageRef(),
                message.getDocumentNumber(),
                message.getRawPayload(),
                message.getNormalizedPayload(),
                message.getPartner() == null ? null : message.getPartner().getId(),
                message.getPartner() == null ? null : message.getPartner().getCode(),
                message.getRelatedOperation() == null ? null : message.getRelatedOperation().getId(),
                message.getRelatedOperation() == null ? null : message.getRelatedOperation().getOperationNumber(),
                message.getReceivedAt(),
                message.getProcessedAt(),
                message.getErrorMessage()
        );
    }

    private EdiProcessingQueueResponse toResponse(EdiProcessingQueue queueItem) {
        EdiMessage message = queueItem.getEdiMessage();
        return new EdiProcessingQueueResponse(
                queueItem.getId(),
                message.getId(),
                message.getMessageType(),
                message.getStatus(),
                message.getMessageRef(),
                message.getDocumentNumber(),
                message.getPartner() == null ? null : message.getPartner().getCode(),
                message.getRelatedOperation() == null ? null : message.getRelatedOperation().getId(),
                message.getNormalizedPayload(),
                queueItem.getStatus(),
                queueItem.getAttemptCount(),
                queueItem.getScheduledAt(),
                queueItem.getStartedAt(),
                queueItem.getFinishedAt(),
                queueItem.getLastError()
        );
    }

    private EdiAuditLogResponse toResponse(EdiAuditLog auditLog) {
        return new EdiAuditLogResponse(
                auditLog.getId(),
                auditLog.getEdiMessage().getId(),
                auditLog.getStage(),
                auditLog.getStatus(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }

    private EdiProcessResultResponse toProcessResult(EdiProcessingQueue queueItem) {
        EdiMessage message = queueItem.getEdiMessage();
        return new EdiProcessResultResponse(
                queueItem.getId(),
                message.getId(),
                queueItem.getStatus(),
                message.getStatus(),
                message.getRelatedOperation() == null ? null : message.getRelatedOperation().getId(),
                queueItem.getLastError()
        );
    }
}
