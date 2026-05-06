package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.ABCAnalysisReport;
import com.cuba.warehousesystem.dto.AuditReport;
import com.cuba.warehousesystem.dto.CellUtilizationReport;
import com.cuba.warehousesystem.dto.DashboardReport;
import com.cuba.warehousesystem.dto.EdiStatisticsReport;
import com.cuba.warehousesystem.dto.MovementReport;
import com.cuba.warehousesystem.dto.ProductAlert;
import com.cuba.warehousesystem.dto.StockBalanceReport;
import com.cuba.warehousesystem.dto.SupplierStatsReport;
import com.cuba.warehousesystem.dto.TopProductReport;
import com.cuba.warehousesystem.dto.TurnoverReport;
import com.cuba.warehousesystem.model.AuditLog;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiQueueStatus;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.repository.AuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final OperationRepository operationRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StorageCellRepository storageCellRepository;
    private final EdiMessageRepository ediMessageRepository;
    private final EdiProcessingQueueRepository ediProcessingQueueRepository;
    private final AuditLogRepository auditLogRepository;

    @Cacheable(value = "turnover", key = "{#start, #end}")
    public TurnoverReport calculateTurnover(LocalDateTime start, LocalDateTime end) {
        List<Operation> incomes = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.INCOME);
        List<Operation> outcomes = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.OUTCOME);
        List<Operation> moves = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.MOVE);

        long totalIn = totalQuantity(incomes);
        long totalOut = totalQuantity(outcomes);
        long totalMove = totalQuantity(moves);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("period_start", start);
        summary.put("period_end", end);
        summary.put("total_incoming", totalIn);
        summary.put("total_outgoing", totalOut);
        summary.put("total_moved", totalMove);
        summary.put("net_change", totalIn - totalOut);
        summary.put("completed_operations", incomes.size() + outcomes.size() + moves.size());
        return new TurnoverReport(summary);
    }

    @Cacheable("stockBalance")
    public StockBalanceReport getStockBalanceReport() {
        List<StockBalanceReport.BalanceItem> items = stockBalanceRepository.findAll().stream()
                .filter(balance -> balance.getQuantity() > 0)
                .sorted(Comparator.comparing(balance -> balance.getProduct().getSku()))
                .map(balance -> new StockBalanceReport.BalanceItem(
                        balance.getProduct().getSku(),
                        balance.getProduct().getName(),
                        balance.getQuantity(),
                        balance.getCell().getCode()
                ))
                .toList();
        return new StockBalanceReport(items);
    }

    @Cacheable(value = "movement", key = "{#start, #end}")
    public MovementReport getMovementReport(LocalDateTime start, LocalDateTime end) {
        List<MovementReport.MovementItem> movements = operationRepository
                .findByCreatedAtBetweenAndStatus(start, end, OperationStatus.COMPLETED)
                .stream()
                .flatMap(operation -> operation.getItems().stream().map(item -> new MovementReport.MovementItem(
                        operation.getOperationNumber(),
                        operation.getType().name(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getFromCell() == null ? null : item.getFromCell().getCode(),
                        item.getToCell() == null ? null : item.getToCell().getCode(),
                        operation.getCompletedAt()
                )))
                .toList();
        return new MovementReport(movements);
    }

    @Cacheable(value = "topProducts", key = "{#topN, #start, #end}")
    public TopProductReport getTopProductsReport(int topN, LocalDateTime start, LocalDateTime end) {
        List<Operation> operations = operationRepository.findByCreatedAtBetweenAndStatus(start, end, OperationStatus.COMPLETED);
        Map<Long, ProductMovementAccumulator> grouped = new HashMap<>();

        operations.stream()
                .flatMap(operation -> operation.getItems().stream())
                .forEach(item -> grouped.computeIfAbsent(item.getProduct().getId(), id -> new ProductMovementAccumulator(item.getProduct()))
                        .addQuantity(item.getQuantity()));

        List<TopProductReport.TopProductItem> topItems = grouped.values().stream()
                .sorted(Comparator.comparingInt(ProductMovementAccumulator::quantity).reversed())
                .limit(Math.max(topN, 1))
                .map(accumulator -> new TopProductReport.TopProductItem(
                        accumulator.product().getSku(),
                        accumulator.product().getName(),
                        accumulator.quantity(),
                        accumulator.movementCount()
                ))
                .toList();
        return new TopProductReport(topItems);
    }

    @Cacheable(value = "supplierStats", key = "{#start, #end}")
    public SupplierStatsReport getSupplierStatsReport(LocalDateTime start, LocalDateTime end) {
        Map<Long, SupplierAccumulator> grouped = operationRepository
                .findCompletedOperationsByTypeAndPeriod(start, end, OperationType.INCOME)
                .stream()
                .filter(operation -> operation.getCounterparty() != null)
                .collect(Collectors.toMap(
                        operation -> operation.getCounterparty().getId(),
                        SupplierAccumulator::from,
                        SupplierAccumulator::merge,
                        LinkedHashMap::new
                ));

        List<SupplierStatsReport.SupplierStatItem> stats = grouped.values().stream()
                .sorted(Comparator.comparingLong(SupplierAccumulator::totalQuantity).reversed())
                .map(accumulator -> new SupplierStatsReport.SupplierStatItem(
                        accumulator.supplierName(),
                        accumulator.operationCount(),
                        Math.toIntExact(accumulator.totalQuantity())
                ))
                .toList();
        return new SupplierStatsReport(stats);
    }

    @Cacheable("cellUtilization")
    public CellUtilizationReport getCellUtilizationReport() {
        List<CellUtilizationReport.CellUtilizationItem> utilizations = storageCellRepository.findAll().stream()
                .map(cell -> new CellUtilizationReport.CellUtilizationItem(
                        cell.getCode(),
                        cell.getCurrentVolumeCm3(),
                        cell.getMaxVolumeCm3(),
                        cell.getCurrentWeightKg(),
                        cell.getMaxWeightKg()
                ))
                .toList();
        return new CellUtilizationReport(utilizations);
    }

    @Cacheable(value = "abcAnalysis", key = "{#periodStart, #periodEnd}")
    public ABCAnalysisReport getABCAnalysisReport(LocalDateTime periodStart, LocalDateTime periodEnd) {
        List<Operation> outcomeOps = operationRepository.findCompletedOperationsByTypeAndPeriod(
                periodStart,
                periodEnd,
                OperationType.OUTCOME
        );
        Map<Long, ProductMovementAccumulator> grouped = new HashMap<>();
        outcomeOps.stream()
                .flatMap(operation -> operation.getItems().stream())
                .forEach(item -> grouped.computeIfAbsent(item.getProduct().getId(), id -> new ProductMovementAccumulator(item.getProduct()))
                        .addQuantity(item.getQuantity()));

        long totalTurnover = grouped.values().stream().mapToLong(ProductMovementAccumulator::quantity).sum();
        if (totalTurnover == 0) {
            return new ABCAnalysisReport(List.of());
        }

        List<ProductMovementAccumulator> sorted = grouped.values().stream()
                .sorted(Comparator.comparingInt(ProductMovementAccumulator::quantity).reversed())
                .toList();

        List<ABCAnalysisReport.ABCItem> abcItems = new ArrayList<>();
        double cumulativePercentage = 0.0;
        for (ProductMovementAccumulator accumulator : sorted) {
            cumulativePercentage += accumulator.quantity() * 100.0 / totalTurnover;
            String category = cumulativePercentage <= 80.0 ? "A" : cumulativePercentage <= 95.0 ? "B" : "C";
            abcItems.add(new ABCAnalysisReport.ABCItem(
                    accumulator.product().getSku(),
                    accumulator.product().getName(),
                    accumulator.quantity(),
                    round(cumulativePercentage),
                    category
            ));
        }
        return new ABCAnalysisReport(abcItems);
    }

    @Cacheable("ediStatistics")
    public EdiStatisticsReport getEdiStatisticsReport() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (EdiMessageStatus status : EdiMessageStatus.values()) {
            byStatus.put(status.name(), ediMessageRepository.countByStatus(status));
        }

        Map<String, Long> byType = new LinkedHashMap<>();
        for (EdiMessageType type : EdiMessageType.values()) {
            byType.put(type.name(), ediMessageRepository.countByMessageType(type));
        }

        return new EdiStatisticsReport(
                ediMessageRepository.count(),
                byStatus,
                byType,
                ediProcessingQueueRepository.countByStatus(EdiQueueStatus.PENDING),
                ediProcessingQueueRepository.countByStatus(EdiQueueStatus.FAILED)
        );
    }

    public AuditReport getAuditReport(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Page<AuditLog> page = start == null || end == null
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.findByOccurredAtBetween(start, end, pageable);
        return new AuditReport(page.getContent().stream()
                .map(log -> new AuditReport.AuditItem(
                        log.getId(),
                        log.getEntityName(),
                        log.getEntityId(),
                        log.getAction(),
                        log.getUsername(),
                        log.getOccurredAt(),
                        log.getDetailsJson()
                ))
                .toList());
    }

    @Cacheable("dashboard")
    public DashboardReport getDashboardReport() {
        List<ProductAlert> lowStockAlerts = findLowStockAlerts();
        List<CellUtilizationReport.CellUtilizationItem> mostUtilizedCells = getCellUtilizationReport().utilizations().stream()
                .sorted(Comparator.comparing(this::cellVolumeUtilizationPercent).reversed())
                .limit(10)
                .toList();

        long totalStockQuantity = stockBalanceRepository.findAll().stream()
                .mapToLong(StockBalance::getQuantity)
                .sum();

        return new DashboardReport(
                productRepository.countByIsActiveTrue(),
                warehouseRepository.countByIsActiveTrue(),
                storageCellRepository.countByIsActiveTrue(),
                operationRepository.countByStatus(OperationStatus.COMPLETED),
                operationRepository.countByStatus(OperationStatus.DRAFT),
                totalStockQuantity,
                lowStockAlerts.size(),
                ediMessageRepository.countByStatus(EdiMessageStatus.RECEIVED)
                        + ediMessageRepository.countByStatus(EdiMessageStatus.NORMALIZED)
                        + ediMessageRepository.countByStatus(EdiMessageStatus.PROCESSING),
                ediMessageRepository.countByStatus(EdiMessageStatus.FAILED),
                mostUtilizedCells,
                lowStockAlerts
        );
    }

    public List<ProductAlert> findLowStockAlerts() {
        Map<Product, Integer> totals = stockBalanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        StockBalance::getProduct,
                        Collectors.summingInt(StockBalance::getQuantity)
                ));

        return totals.entrySet().stream()
                .filter(entry -> entry.getKey().getMinStockLevel() > 0)
                .filter(entry -> entry.getValue() < entry.getKey().getMinStockLevel())
                .map(entry -> new ProductAlert(
                        entry.getKey().getName(),
                        entry.getKey().getSku(),
                        entry.getValue(),
                        entry.getKey().getMinStockLevel()
                ))
                .toList();
    }

    private long totalQuantity(List<Operation> operations) {
        return operations.stream()
                .flatMap(operation -> operation.getItems().stream())
                .mapToLong(OperationItem::getQuantity)
                .sum();
    }

    private double cellVolumeUtilizationPercent(CellUtilizationReport.CellUtilizationItem item) {
        if (item.maxVolume() == null || BigDecimal.ZERO.compareTo(item.maxVolume()) == 0) {
            return 0.0;
        }
        return item.currentVolume().multiply(BigDecimal.valueOf(100))
                .divide(item.maxVolume(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static class ProductMovementAccumulator {
        private final Product product;
        private int quantity;
        private long movementCount;

        ProductMovementAccumulator(Product product) {
            this.product = product;
        }

        void addQuantity(int delta) {
            this.quantity += delta;
            this.movementCount++;
        }

        Product product() {
            return product;
        }

        int quantity() {
            return quantity;
        }

        long movementCount() {
            return movementCount;
        }
    }

    private record SupplierAccumulator(String supplierName, long operationCount, long totalQuantity) {
        static SupplierAccumulator from(Operation operation) {
            return new SupplierAccumulator(
                    operation.getCounterparty().getName(),
                    1,
                    operation.getItems().stream().mapToLong(OperationItem::getQuantity).sum()
            );
        }

        SupplierAccumulator merge(SupplierAccumulator other) {
            return new SupplierAccumulator(
                    Objects.requireNonNullElse(supplierName, other.supplierName()),
                    operationCount + other.operationCount(),
                    totalQuantity + other.totalQuantity()
            );
        }
    }
}
