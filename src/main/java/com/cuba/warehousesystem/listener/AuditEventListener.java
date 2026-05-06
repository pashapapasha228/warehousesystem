package com.cuba.warehousesystem.listener;

import com.cuba.warehousesystem.event.EdiMessageProcessedEvent;
import com.cuba.warehousesystem.event.EdiMessageReceivedEvent;
import com.cuba.warehousesystem.event.OperationCompletedEvent;
import com.cuba.warehousesystem.event.OperationCreatedEvent;
import com.cuba.warehousesystem.event.StockBalanceChangedEvent;
import com.cuba.warehousesystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogService auditLogService;

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOperationCreated(OperationCreatedEvent event) {
        auditLogService.write(
                "Operation",
                String.valueOf(event.operationId()),
                "CREATED",
                event.username(),
                "{\"operationNumber\":\"" + event.operationNumber() + "\",\"type\":\"" + event.type() + "\"}"
        );
    }

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOperationCompleted(OperationCompletedEvent event) {
        auditLogService.write(
                "Operation",
                String.valueOf(event.operationId()),
                "COMPLETED",
                event.username(),
                "{\"operationNumber\":\"" + event.operationNumber() + "\",\"type\":\"" + event.type() + "\"}"
        );
    }

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockBalanceChanged(StockBalanceChangedEvent event) {
        auditLogService.write(
                "StockBalance",
                event.productId() + ":" + event.cellId(),
                "CHANGED",
                event.username(),
                "{\"operationId\":" + event.operationId()
                        + ",\"operationType\":\"" + event.operationType()
                        + "\",\"previousQuantity\":" + event.previousQuantity()
                        + ",\"newQuantity\":" + event.newQuantity()
                        + "}"
        );
    }

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEdiMessageReceived(EdiMessageReceivedEvent event) {
        auditLogService.write(
                "EdiMessage",
                String.valueOf(event.ediMessageId()),
                "RECEIVED",
                "system",
                "{\"messageType\":\"" + event.messageType() + "\",\"messageRef\":\"" + event.messageRef() + "\"}"
        );
    }

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEdiMessageProcessed(EdiMessageProcessedEvent event) {
        auditLogService.write(
                "EdiMessage",
                String.valueOf(event.ediMessageId()),
                "PROCESSED",
                "system",
                "{\"messageType\":\"" + event.messageType()
                        + "\",\"status\":\"" + event.status()
                        + "\",\"relatedOperationId\":" + event.relatedOperationId()
                        + "}"
        );
    }
}
