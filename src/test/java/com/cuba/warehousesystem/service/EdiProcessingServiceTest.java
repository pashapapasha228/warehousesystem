package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.EdiMessageReceiveRequest;
import com.cuba.warehousesystem.dto.EdiProcessResultResponse;
import com.cuba.warehousesystem.dto.OperationRequest;
import com.cuba.warehousesystem.dto.OperationResponse;
import com.cuba.warehousesystem.event.EdiMessageReceivedEvent;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.CounterpartyType;
import com.cuba.warehousesystem.model.EdiDirection;
import com.cuba.warehousesystem.model.EdiMappingConfig;
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiPartner;
import com.cuba.warehousesystem.model.EdiProcessingQueue;
import com.cuba.warehousesystem.model.EdiQueueStatus;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationSource;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.EdiAuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMappingConfigRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiPartnerRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EdiProcessingServiceTest {

    private final EdiMessageRepository ediMessageRepository = mock(EdiMessageRepository.class);
    private final EdiPartnerRepository ediPartnerRepository = mock(EdiPartnerRepository.class);
    private final EdiMappingConfigRepository ediMappingConfigRepository = mock(EdiMappingConfigRepository.class);
    private final EdiProcessingQueueRepository ediProcessingQueueRepository = mock(EdiProcessingQueueRepository.class);
    private final EdiAuditLogRepository ediAuditLogRepository = mock(EdiAuditLogRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StockBalanceRepository stockBalanceRepository = mock(StockBalanceRepository.class);
    private final StorageCellRepository storageCellRepository = mock(StorageCellRepository.class);
    private final OperationRepository operationRepository = mock(OperationRepository.class);
    private final OperationService operationService = mock(OperationService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private EdiProcessingService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new EdiProcessingService(
                ediMessageRepository,
                ediPartnerRepository,
                ediMappingConfigRepository,
                ediProcessingQueueRepository,
                ediAuditLogRepository,
                productRepository,
                stockBalanceRepository,
                storageCellRepository,
                operationRepository,
                operationService,
                eventPublisher,
                objectMapper
        );
    }

    @Test
    void receiveInboundDesadvFromSupplierStoresPayloadAndPublishesReceiveEvent() throws Exception {
        EdiPartner partner = partner(CounterpartyType.SUPPLIER);
        when(ediPartnerRepository.findByCode("SUP-EDI")).thenReturn(Optional.of(partner));
        when(ediMessageRepository.save(any(EdiMessage.class))).thenAnswer(invocation -> {
            EdiMessage message = invocation.getArgument(0);
            message.setId(10L);
            return message;
        });

        service.receiveInbound(new EdiMessageReceiveRequest(
                null,
                "SUP-EDI",
                EdiMessageType.DESADV,
                "UNB-1",
                "MSG-1",
                "ASN-1",
                "{raw}",
                objectMapper.readTree("{\"items\":[]}")
        ));

        ArgumentCaptor<EdiMessage> messageCaptor = ArgumentCaptor.forClass(EdiMessage.class);
        verify(ediMessageRepository).save(messageCaptor.capture());
        EdiMessage saved = messageCaptor.getValue();
        assertThat(saved.getDirection()).isEqualTo(EdiDirection.INBOUND);
        assertThat(saved.getStatus()).isEqualTo(EdiMessageStatus.RECEIVED);
        assertThat(saved.getRawPayload()).isEqualTo("{raw}");
        assertThat(saved.getNormalizedPayload()).contains("\"items\"");
        verify(eventPublisher).publishEvent(any(EdiMessageReceivedEvent.class));
    }

    @Test
    void desadvFromSupplierCreatesDraftIncomeWithoutCompletingStockMovement() {
        EdiProcessingQueue queue = queue(EdiMessageType.DESADV, CounterpartyType.SUPPLIER, payload("\"toCellId\":10,"));
        Product product = product();
        StorageCell targetCell = cell(10L);
        mockMapping(product);
        when(storageCellRepository.findById(10L)).thenReturn(Optional.of(targetCell));
        mockDraftOperation(100L, "INCOME-100");

        EdiProcessResultResponse result = service.processQueueItem(1L, "manager");

        ArgumentCaptor<OperationRequest> requestCaptor = ArgumentCaptor.forClass(OperationRequest.class);
        verify(operationService).createDraftOperation(requestCaptor.capture(), eq("manager"));
        assertThat(requestCaptor.getValue().type()).isEqualTo(OperationType.INCOME);
        assertThat(requestCaptor.getValue().source()).isEqualTo(OperationSource.EDI);
        assertThat(result.relatedOperationId()).isEqualTo(100L);
        verify(operationService, never()).completeOperation(any(), any());
        verify(stockBalanceRepository, never()).save(any());
    }

    @Test
    void ordersFromCustomerCreatesDraftOutcomeWithoutCompletingStockMovement() {
        EdiProcessingQueue queue = queue(EdiMessageType.ORDERS, CounterpartyType.CUSTOMER, payload("\"fromCellId\":20,"));
        Product product = product();
        StorageCell sourceCell = cell(20L);
        mockMapping(product);
        when(storageCellRepository.findById(20L)).thenReturn(Optional.of(sourceCell));
        when(stockBalanceRepository.findByProduct_IdAndCell_Id(1000L, 20L))
                .thenReturn(Optional.of(new StockBalance(product, sourceCell, 10)));
        mockDraftOperation(200L, "OUTCOME-200");

        EdiProcessResultResponse result = service.processQueueItem(1L, "manager");

        ArgumentCaptor<OperationRequest> requestCaptor = ArgumentCaptor.forClass(OperationRequest.class);
        verify(operationService).createDraftOperation(requestCaptor.capture(), eq("manager"));
        assertThat(requestCaptor.getValue().type()).isEqualTo(OperationType.OUTCOME);
        assertThat(result.relatedOperationId()).isEqualTo(200L);
        verify(operationService, never()).completeOperation(any(), any());
        verify(stockBalanceRepository, never()).save(any());
    }

    @Test
    void ordrspDoesNotCreateWarehouseOperation() {
        queue(EdiMessageType.ORDRSP, CounterpartyType.SUPPLIER, payload(""));

        EdiProcessResultResponse result = service.processQueueItem(1L, "manager");

        assertThat(result.messageStatus()).isEqualTo(EdiMessageStatus.PROCESSED);
        assertThat(result.relatedOperationId()).isNull();
        verify(operationService, never()).createDraftOperation(any(), any());
    }

    @Test
    void missingRequiredCellFailsQueueItem() {
        queue(EdiMessageType.DESADV, CounterpartyType.SUPPLIER, payload(""));
        mockMapping(product());

        EdiProcessResultResponse result = service.processQueueItem(1L, "manager");

        assertThat(result.queueStatus()).isEqualTo(EdiQueueStatus.FAILED);
        assertThat(result.errorMessage()).contains("toCellId");
        verify(operationService, never()).createDraftOperation(any(), any());
    }

    @Test
    void missingProductMappingFailsQueueItem() {
        queue(EdiMessageType.DESADV, CounterpartyType.SUPPLIER, payload("\"toCellId\":10,"));
        when(ediMappingConfigRepository.findAllByPartner_IdAndExternalProductCodeAndIsActiveTrue(1L, "EXT-1"))
                .thenReturn(List.of());

        EdiProcessResultResponse result = service.processQueueItem(1L, "manager");

        assertThat(result.queueStatus()).isEqualTo(EdiQueueStatus.FAILED);
        assertThat(result.errorMessage()).contains("No active EDI product mapping");
        verify(operationService, never()).createDraftOperation(any(), any());
    }

    @Test
    void inactivePartnerCannotBeProcessed() {
        EdiProcessingQueue queue = queue(EdiMessageType.DESADV, CounterpartyType.SUPPLIER, payload("\"toCellId\":10,"));
        queue.getEdiMessage().getPartner().setIsActive(false);

        EdiProcessResultResponse result = service.processQueueItem(1L, "manager");

        assertThat(result.queueStatus()).isEqualTo(EdiQueueStatus.FAILED);
        assertThat(result.errorMessage()).contains("inactive");
        verify(operationService, never()).createDraftOperation(any(), any());
    }

    private EdiProcessingQueue queue(EdiMessageType type, CounterpartyType counterpartyType, String normalizedPayload) {
        EdiMessage message = new EdiMessage();
        message.setId(99L);
        message.setMessageType(type);
        message.setDirection(EdiDirection.INBOUND);
        message.setStatus(EdiMessageStatus.RECEIVED);
        message.setPartner(partner(counterpartyType));
        message.setDocumentNumber("DOC-1");
        message.setNormalizedPayload(normalizedPayload);

        EdiProcessingQueue queue = new EdiProcessingQueue();
        queue.setId(1L);
        queue.setEdiMessage(message);
        queue.setStatus(EdiQueueStatus.PENDING);
        queue.setAttemptCount(0);
        queue.setScheduledAt(LocalDateTime.now());
        when(ediProcessingQueueRepository.findById(1L)).thenReturn(Optional.of(queue));
        when(ediProcessingQueueRepository.save(any(EdiProcessingQueue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return queue;
    }

    private String payload(String cellField) {
        return """
                {
                  "warehouseId": 500,
                  "documentDate": "2026-05-14",
                  "items": [
                    {"externalProductCode": "EXT-1", "quantity": 2, %s "unitPrice": 10.00}
                  ]
                }
                """.formatted(cellField);
    }

    private void mockMapping(Product product) {
        EdiMappingConfig mapping = new EdiMappingConfig();
        mapping.setPartner(partner(CounterpartyType.SUPPLIER));
        mapping.setMessageType(EdiMessageType.DESADV);
        mapping.setExternalProductCode("EXT-1");
        mapping.setInternalProduct(product);
        when(ediMappingConfigRepository.findAllByPartner_IdAndExternalProductCodeAndIsActiveTrue(eq(1L), eq("EXT-1")))
                .thenReturn(List.of(mapping));
    }

    private void mockDraftOperation(Long id, String number) {
        OperationResponse response = new OperationResponse(
                id,
                number,
                OperationType.INCOME,
                OperationStatus.DRAFT,
                500L,
                "WH-1",
                1L,
                null,
                300L,
                OperationSource.EDI,
                "DOC-1",
                null,
                null,
                LocalDateTime.now(),
                null,
                List.of()
        );
        Operation operation = new Operation();
        operation.setId(id);
        operation.setOperationNumber(number);
        when(operationService.createDraftOperation(any(OperationRequest.class), any())).thenReturn(response);
        when(operationRepository.findById(id)).thenReturn(Optional.of(operation));
    }

    private EdiPartner partner(CounterpartyType type) {
        Counterparty counterparty = new Counterparty();
        counterparty.setId(300L);
        counterparty.setCode(type == CounterpartyType.SUPPLIER ? "SUP-1" : "CUS-1");
        counterparty.setType(type);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(500L);
        warehouse.setCode("WH-1");

        EdiPartner partner = new EdiPartner();
        partner.setId(1L);
        partner.setCode(type == CounterpartyType.SUPPLIER ? "SUP-EDI" : "CUS-EDI");
        partner.setCounterparty(counterparty);
        partner.setDefaultWarehouse(warehouse);
        partner.setInboundEnabled(true);
        partner.setIsActive(true);
        return partner;
    }

    private Product product() {
        Product product = new Product();
        product.setId(1000L);
        product.setSku("SKU-1");
        product.setName("Product 1");
        product.setWeightPerUnitKg(BigDecimal.ONE);
        product.setVolumePerUnitCm3(BigDecimal.ONE);
        product.setLengthCm(BigDecimal.ONE);
        product.setWidthCm(BigDecimal.ONE);
        product.setHeightCm(BigDecimal.ONE);
        return product;
    }

    private StorageCell cell(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(500L);
        warehouse.setCode("WH-1");

        StorageCell cell = new StorageCell();
        cell.setId(id);
        cell.setCode("A-01");
        cell.setWarehouse(warehouse);
        cell.setIsActive(true);
        return cell;
    }
}
