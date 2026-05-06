package com.cuba.warehousesystem.listener;

import com.cuba.warehousesystem.event.StockBalanceChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class StockBalanceEventListener {

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockBalanceChanged(StockBalanceChangedEvent event) {
        log.debug(
                "Stock balance changed for product {} in cell {}: {} -> {}",
                event.productId(),
                event.cellId(),
                event.previousQuantity(),
                event.newQuantity()
        );
    }
}
