package com.cuba.warehousesystem.listener;

import com.cuba.warehousesystem.event.OperationCompletedEvent;
import com.cuba.warehousesystem.event.StockBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReportCacheEvictListener {

    private static final Set<String> REPORT_CACHE_NAMES = Set.of(
            "turnover",
            "stockBalance",
            "movement",
            "topProducts",
            "supplierStats",
            "cellUtilization",
            "abcAnalysis",
            "ediStatistics",
            "auditReport",
            "dashboard"
    );

    private final CacheManager cacheManager;

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOperationCompleted(OperationCompletedEvent event) {
        evictReportCaches();
    }

    @Async("warehouseEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockBalanceChanged(StockBalanceChangedEvent event) {
        evictReportCaches();
    }

    private void evictReportCaches() {
        cacheManager.getCacheNames().stream()
                .filter(name -> name.startsWith("report") || REPORT_CACHE_NAMES.contains(name))
                .map(cacheManager::getCache)
                .filter(cache -> cache != null)
                .forEach(cache -> cache.clear());
    }
}
