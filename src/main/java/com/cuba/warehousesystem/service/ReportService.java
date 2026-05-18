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
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.ProductWarehouseMinStock;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.repository.ProductWarehouseMinStockRepository;
import com.cuba.warehousesystem.repository.AuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final ProductWarehouseMinStockRepository productWarehouseMinStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StorageCellRepository storageCellRepository;
    private final EdiMessageRepository ediMessageRepository;
    private final EdiProcessingQueueRepository ediProcessingQueueRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

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
                ediProcessingQueueRepository.countByEdiMessage_Status(EdiMessageStatus.RECEIVED)
                        + ediProcessingQueueRepository.countByEdiMessage_Status(EdiMessageStatus.NORMALIZED),
                ediProcessingQueueRepository.countByEdiMessage_Status(EdiMessageStatus.FAILED)
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

    public DashboardReport getDashboardReport(Long warehouseId, int periodDays) {
        int safePeriodDays = periodDays <= 1 ? 1 : periodDays >= 30 ? 30 : 7;
        LocalDateTime periodEnd = LocalDateTime.now();
        LocalDateTime periodStart = safePeriodDays == 1
                ? periodEnd.toLocalDate().atStartOfDay()
                : periodEnd.minusDays(safePeriodDays);
        LocalDateTime todayStart = periodEnd.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        List<EdiMessageStatus> pendingStatuses = List.of(
                EdiMessageStatus.RECEIVED,
                EdiMessageStatus.NORMALIZED,
                EdiMessageStatus.PROCESSING
        );
        List<OperationStatus> openOperationStatuses = List.of(OperationStatus.DRAFT, OperationStatus.SHIPPED);

        List<Operation> operations = operationRepository.findAll().stream()
                .filter(operation -> matchesWarehouse(operation, warehouseId))
                .toList();
        List<EdiMessage> ediMessages = ediMessageRepository.findAll().stream()
                .filter(message -> matchesWarehouse(message, warehouseId))
                .toList();
        List<DashboardReport.StockWarningItem> stockWarnings = findLowStockAlerts().stream()
                .filter(alert -> warehouseId == null || Objects.equals(alert.warehouseId(), warehouseId))
                .sorted(Comparator.comparing((ProductAlert alert) -> alert.currentStock() > 0)
                        .thenComparing(ProductAlert::productName))
                .map(this::toStockWarningItem)
                .toList();
        List<DashboardReport.CellLoadItem> cellLoads = storageCellRepository.findAll().stream()
                .filter(cell -> warehouseId == null || Objects.equals(cell.getWarehouse().getId(), warehouseId))
                .map(this::toDashboardCellLoadItem)
                .toList();
        List<DashboardReport.CellLoadItem> topCells = cellLoads.stream()
                .sorted(Comparator.comparing(this::dashboardCellMaxUtilizationPercent).reversed())
                .limit(10)
                .toList();

        long pendingEdi = ediMessages.stream().filter(message -> pendingStatuses.contains(message.getStatus())).count();
        long failedEdi = ediMessages.stream().filter(message -> message.getStatus() == EdiMessageStatus.FAILED).count();
        long desadvToProcess = ediMessages.stream()
                .filter(message -> message.getMessageType() == EdiMessageType.DESADV)
                .filter(message -> pendingStatuses.contains(message.getStatus()) || message.getStatus() == EdiMessageStatus.FAILED)
                .count();
        long ordersToProcess = ediMessages.stream()
                .filter(message -> message.getMessageType() == EdiMessageType.ORDERS)
                .filter(message -> pendingStatuses.contains(message.getStatus()) || message.getStatus() == EdiMessageStatus.FAILED)
                .count();
        long draftIncome = operations.stream()
                .filter(operation -> operation.getType() == OperationType.INCOME && operation.getStatus() == OperationStatus.DRAFT)
                .count();
        long draftOutcome = operations.stream()
                .filter(operation -> operation.getType() == OperationType.OUTCOME && operation.getStatus() == OperationStatus.DRAFT)
                .count();
        long completedOperations = operations.stream()
                .filter(operation -> operation.getStatus() == OperationStatus.COMPLETED)
                .filter(operation -> isBetween(operation.getCompletedAt(), periodStart, periodEnd))
                .count();
        long draftOperations = operations.stream()
                .filter(operation -> openOperationStatuses.contains(operation.getStatus()))
                .count();
        long zeroStockProducts = stockWarnings.stream().filter(warning -> warning.currentStock() == 0).count();
        long belowMinProducts = stockWarnings.stream().filter(warning -> warning.currentStock() > 0).count();

        DashboardReport.DashboardKpi kpi = new DashboardReport.DashboardKpi(
                desadvToProcess + draftIncome,
                ordersToProcess + draftOutcome,
                pendingEdi,
                failedEdi,
                zeroStockProducts,
                belowMinProducts,
                average(cellLoads.stream().map(DashboardReport.CellLoadItem::volumePercent).toList()),
                average(cellLoads.stream().map(DashboardReport.CellLoadItem::weightPercent).toList()),
                completedOperations,
                draftOperations
        );

        DashboardReport.EdiSummary ediSummary = new DashboardReport.EdiSummary(
                ediMessages.stream().filter(message -> isBetween(message.getReceivedAt(), todayStart, tomorrowStart)).count(),
                pendingEdi,
                ediMessages.stream().filter(message -> message.getStatus() == EdiMessageStatus.PROCESSED).count(),
                failedEdi,
                ediMessages.stream()
                        .filter(message -> message.getStatus() == EdiMessageStatus.FAILED)
                        .sorted(Comparator.comparing(EdiMessage::getReceivedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(5)
                        .map(this::toEdiProblemItem)
                        .toList()
        );

        DashboardReport.CellUtilizationSummary cellUtilization = new DashboardReport.CellUtilizationSummary(
                kpi.averageVolumeUtilization(),
                kpi.averageWeightUtilization(),
                cellLoads.stream().filter(cell -> dashboardCellMaxUtilizationPercent(cell) >= 80.0).count(),
                cellLoads.stream().filter(cell -> dashboardCellMaxUtilizationPercent(cell) >= 90.0).count(),
                topCells
        );

        return new DashboardReport(
                kpi,
                buildAttentionItems(operations, ediMessages, stockWarnings, cellLoads, pendingStatuses, openOperationStatuses),
                buildReceivingQueue(operations, ediMessages, pendingStatuses),
                buildShippingQueue(operations, ediMessages, pendingStatuses),
                ediSummary,
                cellUtilization,
                stockWarnings,
                operations.stream()
                        .filter(operation -> isBetween(operationTimestamp(operation), periodStart, periodEnd))
                        .sorted(Comparator.comparing(this::operationTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(8)
                        .map(this::toRecentOperationItem)
                        .toList()
        );
    }

    public DashboardReport getDashboardReport() {
        List<ProductAlert> lowStockAlerts = findLowStockAlerts();
        List<CellUtilizationReport.CellUtilizationItem> sortedUtilizedCells = getCellUtilizationReport().utilizations().stream()
                .sorted(Comparator.comparing(this::cellMaxUtilizationPercent).reversed())
                .toList();
        List<CellUtilizationReport.CellUtilizationItem> mostUtilizedCells = sortedUtilizedCells.stream()
                .limit(10)
                .toList();
        List<DashboardReport.CellLoadItem> overloadedCellItems = sortedUtilizedCells.stream()
                .filter(cell -> cellMaxUtilizationPercent(cell) >= 90.0)
                .limit(8)
                .map(this::toCellLoadItem)
                .toList();

        Pageable actionPage = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<DashboardReport.OperationActionItem> inboundOperations = operationRepository
                .findByTypeAndStatus(OperationType.INCOME, OperationStatus.DRAFT, actionPage)
                .getContent()
                .stream()
                .map(this::toOperationActionItem)
                .toList();
        List<DashboardReport.OperationActionItem> outboundOperations = operationRepository
                .findByTypeAndStatus(OperationType.OUTCOME, OperationStatus.DRAFT, actionPage)
                .getContent()
                .stream()
                .map(this::toOperationActionItem)
                .toList();
        List<DashboardReport.OperationActionItem> waitingOperationItems = operationRepository
                .findByStatusIn(List.of(OperationStatus.DRAFT, OperationStatus.SHIPPED), actionPage)
                .getContent()
                .stream()
                .map(this::toOperationActionItem)
                .toList();

        List<EdiMessageStatus> stuckStatuses = List.of(
                EdiMessageStatus.RECEIVED,
                EdiMessageStatus.NORMALIZED,
                EdiMessageStatus.PROCESSING,
                EdiMessageStatus.FAILED
        );
        List<DashboardReport.EdiActionItem> stuckEdiMessages = ediMessageRepository
                .findByStatusIn(stuckStatuses, PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "receivedAt")))
                .getContent()
                .stream()
                .map(this::toEdiActionItem)
                .toList();

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        DashboardReport.TodaySummary todaySummary = new DashboardReport.TodaySummary(
                operationRepository.countByCreatedAtBetween(todayStart, tomorrowStart),
                operationRepository.countByCompletedAtBetween(todayStart, tomorrowStart),
                ediMessageRepository.countByReceivedAtBetween(todayStart, tomorrowStart)
        );
        List<DashboardReport.ActivityItem> todayActivity = auditLogRepository
                .findTop10ByOccurredAtBetweenOrderByOccurredAtDesc(todayStart, tomorrowStart)
                .stream()
                .map(this::toActivityItem)
                .toList();

        long totalStockQuantity = stockBalanceRepository.findAll().stream()
                .mapToLong(StockBalance::getQuantity)
                .sum();
        long toReceiveCount = operationRepository.countByTypeAndStatus(OperationType.INCOME, OperationStatus.DRAFT);
        long toShipCount = operationRepository.countByTypeAndStatus(OperationType.OUTCOME, OperationStatus.DRAFT);
        long stuckEdiCount = ediMessageRepository.countByStatusIn(stuckStatuses);
        long waitingOperationsCount = operationRepository.countByStatusIn(List.of(OperationStatus.DRAFT, OperationStatus.SHIPPED));

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
                lowStockAlerts,
                new DashboardReport.DashboardActionBlock(
                        "Что принять",
                        "Черновики приемки ждут фактической проверки.",
                        toReceiveCount,
                        toReceiveCount == 0 ? "success" : "warning",
                        "Открыть приемки",
                        "/operations?type=INCOME&status=DRAFT"
                ),
                new DashboardReport.DashboardActionBlock(
                        "Что отгрузить",
                        "Черновики отгрузки готовы к отправке.",
                        toShipCount,
                        toShipCount == 0 ? "success" : "warning",
                        "Открыть отгрузки",
                        "/operations?type=OUTCOME&status=DRAFT"
                ),
                new DashboardReport.DashboardActionBlock(
                        "Что зависло в EDI",
                        "Сообщения ожидают обработки или требуют разбора ошибки.",
                        stuckEdiCount,
                        stuckEdiCount == 0 ? "success" : "error",
                        "Разобрать EDI",
                        "/edi/queue"
                ),
                new DashboardReport.DashboardActionBlock(
                        "Где не хватает товара",
                        "Товары ниже минимального уровня по складам.",
                        lowStockAlerts.size(),
                        lowStockAlerts.isEmpty() ? "success" : "warning",
                        "Проверить остатки",
                        "/stock-balances"
                ),
                new DashboardReport.DashboardActionBlock(
                        "Где перегружены ячейки",
                        "Ячейки с загрузкой 90% и выше по весу или объему.",
                        overloadedCellItems.size(),
                        overloadedCellItems.isEmpty() ? "success" : "warning",
                        "Открыть ячейки",
                        "/storage-cells"
                ),
                new DashboardReport.DashboardActionBlock(
                        "Какие операции ждут действия",
                        "Черновики и отгрузки, ожидающие подтверждения клиента.",
                        waitingOperationsCount,
                        waitingOperationsCount == 0 ? "success" : "warning",
                        "Открыть операции",
                        "/operations"
                ),
                todaySummary,
                inboundOperations,
                outboundOperations,
                waitingOperationItems,
                stuckEdiMessages,
                overloadedCellItems,
                todayActivity
        );
    }

    private List<DashboardReport.WorkQueueItem> buildReceivingQueue(
            List<Operation> operations,
            List<EdiMessage> ediMessages,
            List<EdiMessageStatus> pendingStatuses
    ) {
        List<DashboardReport.WorkQueueItem> items = new ArrayList<>();
        ediMessages.stream()
                .filter(message -> message.getMessageType() == EdiMessageType.DESADV)
                .filter(message -> pendingStatuses.contains(message.getStatus()) || message.getStatus() == EdiMessageStatus.FAILED)
                .sorted(Comparator.comparing(EdiMessage::getReceivedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(message -> toWorkQueueItem(message, "Обработать"))
                .forEach(items::add);
        operations.stream()
                .filter(operation -> operation.getType() == OperationType.INCOME && operation.getStatus() == OperationStatus.DRAFT)
                .sorted(Comparator.comparing(Operation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(operation -> toWorkQueueItem(operation, "Принять"))
                .forEach(items::add);
        return items.stream().limit(6).toList();
    }

    private List<DashboardReport.WorkQueueItem> buildShippingQueue(
            List<Operation> operations,
            List<EdiMessage> ediMessages,
            List<EdiMessageStatus> pendingStatuses
    ) {
        List<DashboardReport.WorkQueueItem> items = new ArrayList<>();
        ediMessages.stream()
                .filter(message -> message.getMessageType() == EdiMessageType.ORDERS)
                .filter(message -> pendingStatuses.contains(message.getStatus()) || message.getStatus() == EdiMessageStatus.FAILED)
                .sorted(Comparator.comparing(EdiMessage::getReceivedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(message -> toWorkQueueItem(message, "Обработать"))
                .forEach(items::add);
        operations.stream()
                .filter(operation -> operation.getType() == OperationType.OUTCOME && operation.getStatus() == OperationStatus.DRAFT)
                .sorted(Comparator.comparing(Operation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(operation -> toWorkQueueItem(operation, "Отгрузить"))
                .forEach(items::add);
        return items.stream().limit(6).toList();
    }

    private List<DashboardReport.AttentionItem> buildAttentionItems(
            List<Operation> operations,
            List<EdiMessage> ediMessages,
            List<DashboardReport.StockWarningItem> stockWarnings,
            List<DashboardReport.CellLoadItem> cellLoads,
            List<EdiMessageStatus> pendingStatuses,
            List<OperationStatus> openOperationStatuses
    ) {
        List<DashboardReport.AttentionItem> items = new ArrayList<>();
        ediMessages.stream()
                .filter(message -> message.getStatus() == EdiMessageStatus.FAILED)
                .limit(4)
                .map(message -> new DashboardReport.AttentionItem(
                        "EDI_ERROR",
                        "error",
                        1,
                        "Ошибка EDI: " + message.getMessageType(),
                        messagePartnerName(message) + " · " + Objects.requireNonNullElse(message.getErrorMessage(), "нужен разбор сообщения"),
                        "Открыть",
                        "/edi/queue?ediStatus=FAILED"
                ))
                .forEach(items::add);
        stockWarnings.stream()
                .filter(warning -> warning.currentStock() == 0)
                .limit(4)
                .map(warning -> new DashboardReport.AttentionItem(
                        "ZERO_STOCK",
                        "error",
                        2,
                        "Нет остатка: " + warning.productName(),
                        warning.sku() + " · склад " + warning.warehouseCode(),
                        "Открыть товар",
                        warning.actionUrl()
                ))
                .forEach(items::add);
        cellLoads.stream()
                .filter(cell -> dashboardCellMaxUtilizationPercent(cell) >= 90.0)
                .limit(4)
                .map(cell -> new DashboardReport.AttentionItem(
                        "CELL_OVERLOAD",
                        "warning",
                        3,
                        "Ячейка перегружена: " + cell.cellCode(),
                        "Объем " + percentText(cell.volumePercent()) + " · вес " + percentText(cell.weightPercent()),
                        "Открыть",
                        cell.actionUrl()
                ))
                .forEach(items::add);
        stockWarnings.stream()
                .filter(warning -> warning.currentStock() > 0)
                .limit(4)
                .map(warning -> new DashboardReport.AttentionItem(
                        "LOW_STOCK",
                        "warning",
                        4,
                        "Ниже минимума: " + warning.productName(),
                        warning.currentStock() + " из " + warning.minLevel() + " · склад " + warning.warehouseCode(),
                        "Создать приемку",
                        "/operations/new?type=INCOME"
                ))
                .forEach(items::add);
        operations.stream()
                .filter(operation -> openOperationStatuses.contains(operation.getStatus()))
                .limit(4)
                .map(operation -> new DashboardReport.AttentionItem(
                        "OPERATION_DRAFT",
                        "info",
                        5,
                        "Операция ждет завершения: " + operation.getOperationNumber(),
                        operation.getType().name() + " · " + operation.getStatus().name(),
                        "Открыть",
                        "/operations/" + operation.getId()
                ))
                .forEach(items::add);
        ediMessages.stream()
                .filter(message -> pendingStatuses.contains(message.getStatus()))
                .limit(4)
                .map(message -> new DashboardReport.AttentionItem(
                        "EDI_PENDING",
                        "info",
                        6,
                        "EDI ожидает обработки: " + message.getMessageType(),
                        messagePartnerName(message) + " · " + message.getStatus().name(),
                        "Открыть очередь",
                        "/edi/queue"
                ))
                .forEach(items::add);
        return items.stream()
                .sorted(Comparator.comparingInt(DashboardReport.AttentionItem::priority))
                .limit(10)
                .toList();
    }

    private DashboardReport.StockWarningItem toStockWarningItem(ProductAlert alert) {
        return new DashboardReport.StockWarningItem(
                alert.productName(),
                alert.sku(),
                alert.warehouseId(),
                alert.warehouseCode(),
                alert.currentStock(),
                alert.minLevel(),
                alert.currentStock() == 0 ? "Нет остатка" : "Ниже минимума",
                "/products"
        );
    }

    private DashboardReport.WorkQueueItem toWorkQueueItem(EdiMessage message, String actionLabel) {
        return new DashboardReport.WorkQueueItem(
                message.getId(),
                "EDI",
                messagePartnerName(message),
                Objects.requireNonNullElse(message.getDocumentNumber(), "#" + message.getId()),
                ediPayloadItemCount(message),
                message.getStatus().name(),
                message.getMessageType(),
                null,
                actionLabel,
                "/edi/queue?ediStatus=" + message.getStatus().name()
        );
    }

    private DashboardReport.WorkQueueItem toWorkQueueItem(Operation operation, String actionLabel) {
        return new DashboardReport.WorkQueueItem(
                operation.getId(),
                "OPERATION",
                operation.getCounterparty() == null ? "Без контрагента" : operation.getCounterparty().getName(),
                Objects.requireNonNullElse(operation.getExternalDocumentNumber(), operation.getOperationNumber()),
                operation.getItems() == null ? 0 : operation.getItems().size(),
                operation.getStatus().name(),
                null,
                operation.getType(),
                actionLabel,
                "/operations/" + operation.getId()
        );
    }

    private DashboardReport.EdiProblemItem toEdiProblemItem(EdiMessage message) {
        return new DashboardReport.EdiProblemItem(
                message.getId(),
                message.getMessageType(),
                message.getStatus(),
                messagePartnerName(message),
                Objects.requireNonNullElse(message.getErrorMessage(), "Ошибка не детализирована"),
                "/edi/queue?ediStatus=FAILED"
        );
    }

    private DashboardReport.CellLoadItem toDashboardCellLoadItem(StorageCell cell) {
        return new DashboardReport.CellLoadItem(
                cell.getId(),
                cell.getWarehouse() == null ? null : cell.getWarehouse().getCode(),
                cell.getCode(),
                round(percent(cell.getCurrentVolumeCm3(), cell.getMaxVolumeCm3())),
                round(percent(cell.getCurrentWeightKg(), cell.getMaxWeightKg())),
                "/storage-cells"
        );
    }

    private DashboardReport.RecentOperationItem toRecentOperationItem(Operation operation) {
        String username = operation.getCompletedBy() != null
                ? operation.getCompletedBy().getUsername()
                : operation.getCreatedBy().getUsername();
        return new DashboardReport.RecentOperationItem(
                operation.getId(),
                operationTimestamp(operation),
                operation.getType(),
                operation.getStatus(),
                operation.getOperationNumber(),
                operation.getWarehouse() == null ? null : operation.getWarehouse().getCode(),
                username,
                "/operations/" + operation.getId()
        );
    }

    private boolean matchesWarehouse(Operation operation, Long warehouseId) {
        return warehouseId == null
                || (operation.getWarehouse() != null && Objects.equals(operation.getWarehouse().getId(), warehouseId));
    }

    private boolean matchesWarehouse(EdiMessage message, Long warehouseId) {
        if (warehouseId == null) {
            return true;
        }
        if (message.getRelatedOperation() != null && message.getRelatedOperation().getWarehouse() != null) {
            return Objects.equals(message.getRelatedOperation().getWarehouse().getId(), warehouseId);
        }
        Long payloadWarehouseId = payloadWarehouseId(message);
        return payloadWarehouseId == null || Objects.equals(payloadWarehouseId, warehouseId);
    }

    private LocalDateTime operationTimestamp(Operation operation) {
        return operation.getCompletedAt() != null ? operation.getCompletedAt() : operation.getCreatedAt();
    }

    private boolean isBetween(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private double average(List<Double> values) {
        return round(values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));
    }

    private double dashboardCellMaxUtilizationPercent(DashboardReport.CellLoadItem item) {
        return Math.max(item.volumePercent() == null ? 0.0 : item.volumePercent(), item.weightPercent() == null ? 0.0 : item.weightPercent());
    }

    private double percent(BigDecimal current, BigDecimal max) {
        if (current == null || max == null || BigDecimal.ZERO.compareTo(max) == 0) {
            return 0.0;
        }
        return current.multiply(BigDecimal.valueOf(100))
                .divide(max, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String percentText(Double value) {
        return (value == null ? 0 : value) + "%";
    }

    private String messagePartnerName(EdiMessage message) {
        if (message.getPartner() == null) {
            return "Без партнера";
        }
        return Objects.requireNonNullElse(message.getPartner().getName(), message.getPartner().getCode());
    }

    private int ediPayloadItemCount(EdiMessage message) {
        try {
            if (message.getNormalizedPayload() == null) {
                return 0;
            }
            JsonNode items = objectMapper.readTree(message.getNormalizedPayload()).path("items");
            return items.isArray() ? items.size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Long payloadWarehouseId(EdiMessage message) {
        try {
            if (message.getNormalizedPayload() == null) {
                return null;
            }
            JsonNode node = objectMapper.readTree(message.getNormalizedPayload()).path("warehouseId");
            return node.isIntegralNumber() ? node.asLong() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public List<ProductAlert> findLowStockAlerts() {
        Map<ProductWarehouseKey, Integer> totals = stockBalanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        balance -> new ProductWarehouseKey(
                                balance.getProduct().getId(),
                                balance.getCell().getWarehouse().getId()
                        ),
                        Collectors.summingInt(StockBalance::getQuantity)
                ));

        return productWarehouseMinStockRepository.findAll().stream()
                .filter(minimum -> minimum.getMinStockLevel() > 0)
                .filter(minimum -> totals.getOrDefault(new ProductWarehouseKey(
                        minimum.getProduct().getId(),
                        minimum.getWarehouse().getId()
                ), 0) < minimum.getMinStockLevel())
                .map(minimum -> {
                    int currentStock = totals.getOrDefault(new ProductWarehouseKey(
                            minimum.getProduct().getId(),
                            minimum.getWarehouse().getId()
                    ), 0);
                    return new ProductAlert(
                            minimum.getProduct().getName(),
                            minimum.getProduct().getSku(),
                            minimum.getWarehouse().getId(),
                            minimum.getWarehouse().getCode(),
                            currentStock,
                            minimum.getMinStockLevel()
                    );
                })
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

    private double cellWeightUtilizationPercent(CellUtilizationReport.CellUtilizationItem item) {
        if (item.maxWeight() == null || BigDecimal.ZERO.compareTo(item.maxWeight()) == 0) {
            return 0.0;
        }
        return item.currentWeight().multiply(BigDecimal.valueOf(100))
                .divide(item.maxWeight(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double cellMaxUtilizationPercent(CellUtilizationReport.CellUtilizationItem item) {
        return Math.max(cellVolumeUtilizationPercent(item), cellWeightUtilizationPercent(item));
    }

    private DashboardReport.CellLoadItem toCellLoadItem(CellUtilizationReport.CellUtilizationItem item) {
        return new DashboardReport.CellLoadItem(
                item.cellCode(),
                round(cellVolumeUtilizationPercent(item)),
                round(cellWeightUtilizationPercent(item)),
                "/storage-cells"
        );
    }

    private DashboardReport.OperationActionItem toOperationActionItem(Operation operation) {
        return new DashboardReport.OperationActionItem(
                operation.getId(),
                operation.getOperationNumber(),
                operation.getType(),
                operation.getStatus(),
                operation.getWarehouse() == null ? null : operation.getWarehouse().getCode(),
                operation.getCounterparty() == null ? null : operation.getCounterparty().getName(),
                operation.getItems() == null ? 0 : operation.getItems().size(),
                operation.getItems() == null ? 0 : Math.toIntExact(operation.getItems().stream().mapToLong(OperationItem::getQuantity).sum()),
                operation.getCreatedAt(),
                "/operations/" + operation.getId()
        );
    }

    private DashboardReport.EdiActionItem toEdiActionItem(EdiMessage message) {
        return new DashboardReport.EdiActionItem(
                message.getId(),
                message.getMessageType(),
                message.getStatus(),
                message.getDocumentNumber(),
                message.getPartner() == null ? null : message.getPartner().getCode(),
                message.getErrorMessage(),
                message.getReceivedAt(),
                "/edi/queue?ediStatus=" + message.getStatus().name()
        );
    }

    private DashboardReport.ActivityItem toActivityItem(AuditLog log) {
        return new DashboardReport.ActivityItem(
                log.getEntityName(),
                log.getEntityId(),
                log.getAction(),
                log.getUsername(),
                log.getOccurredAt(),
                activityUrl(log)
        );
    }

    private String activityUrl(AuditLog log) {
        if ("Operation".equals(log.getEntityName())) {
            return "/operations/" + log.getEntityId();
        }
        if ("EdiMessage".equals(log.getEntityName())) {
            return "/edi/messages";
        }
        return "/reports";
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

    private record ProductWarehouseKey(Long productId, Long warehouseId) {
    }
}
