package com.cuba.warehousesystem.listener;

import com.cuba.warehousesystem.event.EdiMessageProcessedEvent;
import com.cuba.warehousesystem.event.EdiMessageReceivedEvent;
import com.cuba.warehousesystem.model.EdiAuditLog;
import com.cuba.warehousesystem.model.EdiAuditStatus;
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiProcessingQueue;
import com.cuba.warehousesystem.repository.EdiAuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EdiProcessingListener {

    private final EdiMessageRepository ediMessageRepository;
    private final EdiProcessingQueueRepository ediProcessingQueueRepository;
    private final EdiAuditLogRepository ediAuditLogRepository;

    @Async("warehouseEventExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEdiMessageReceived(EdiMessageReceivedEvent event) {
        EdiMessage message = ediMessageRepository.findById(event.ediMessageId()).orElse(null);
        if (message == null) {
            return;
        }

        if (!ediProcessingQueueRepository.existsByEdiMessage_Id(message.getId())) {
            EdiProcessingQueue queueItem = new EdiProcessingQueue();
            queueItem.setEdiMessage(message);
            queueItem.setScheduledAt(LocalDateTime.now());
            ediProcessingQueueRepository.save(queueItem);
        }

        writeEdiAudit(message, "RECEIVE", EdiAuditStatus.SUCCESS, "EDI message accepted into processing queue");
    }

    @Async("warehouseEventExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEdiMessageProcessed(EdiMessageProcessedEvent event) {
        EdiMessage message = ediMessageRepository.findById(event.ediMessageId()).orElse(null);
        if (message == null) {
            return;
        }

        EdiAuditStatus auditStatus = event.errorMessage() == null ? EdiAuditStatus.SUCCESS : EdiAuditStatus.FAILED;
        writeEdiAudit(message, "PROCESS", auditStatus, event.errorMessage() == null ? "EDI message processed" : event.errorMessage());
    }

    private void writeEdiAudit(EdiMessage message, String stage, EdiAuditStatus status, String details) {
        EdiAuditLog auditLog = new EdiAuditLog();
        auditLog.setEdiMessage(message);
        auditLog.setStage(stage);
        auditLog.setStatus(status);
        auditLog.setDetails(details);
        auditLog.setCreatedAt(LocalDateTime.now());
        ediAuditLogRepository.save(auditLog);
    }
}
