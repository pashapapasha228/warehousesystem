package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.*;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.*;
import com.cuba.warehousesystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OperationRepository operationRepository;
    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StorageCellRepository storageCellRepository;

    // --- Добавляем репозиторий для Counterparty ---
    private final CounterpartyRepository counterpartyRepository; // Не забудь создать

    // --- Уже существующие методы ---
    public TurnoverReport calculateTurnover(LocalDateTime start, LocalDateTime end) {
        List<Operation> incomes = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.INCOME);
        List<Operation> outcomes = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.OUTCOME);

        long totalIn = incomes.stream().mapToLong(op -> op.getItems().stream().mapToInt(item -> item.getQuantity()).sum()).sum();
        long totalOut = outcomes.stream().mapToLong(op -> op.getItems().stream().mapToInt(item -> item.getQuantity()).sum()).sum();

        var summary = new java.util.HashMap<String, Object>();
        summary.put("period_start", start);
        summary.put("period_end", end);
        summary.put("total_incoming", totalIn);
        summary.put("total_outgoing", totalOut);
        summary.put("net_change", totalIn - totalOut);

        return new TurnoverReport(summary);
    }

    public List<ProductAlert> findLowStockAlerts() {
        // ... (реализация осталась прежней)
        // Для примера, если у Product есть minStockLevel:
        // return stockBalanceRepository.findAll().stream()
        //         .filter(balance -> balance.getQuantity() < balance.getProduct().getMinStockLevel())
        //         .map(balance -> new ProductAlert(
        //                 balance.getProduct().getName(),
        //                 balance.getProduct().getSku(),
        //                 balance.getQuantity(),
        //                 balance.getProduct().getMinStockLevel()
        //         ))
        //         .collect(Collectors.toList());
        return List.of(); // Пока возвращаем пустой список, если логика не реализована
    }

    // --- Новые методы для аналитики ---

    public StockBalanceReport getStockBalanceReport() {
        List<StockBalance> balances = stockBalanceRepository.findAll();
        List<StockBalanceReport.BalanceItem> items = balances.stream()
                .filter(b -> b.getQuantity() > 0)
                .map(b -> new StockBalanceReport.BalanceItem(
                        b.getProduct().getSku(),
                        b.getProduct().getName(),
                        b.getQuantity(),
                        b.getCell().getCode()
                ))
                .collect(Collectors.toList());
        return new StockBalanceReport(items);
    }

    public MovementReport getMovementReport(LocalDateTime start, LocalDateTime end) {
        List<Operation> operations = operationRepository.findByCreatedAtBetweenAndStatus(start, end, OperationStatus.COMPLETED);
        List<MovementReport.MovementItem> movements = operations.stream()
                .flatMap(op -> op.getItems().stream().map(item -> new MovementReport.MovementItem(
                        op.getOperationNumber(),
                        op.getType().name(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getFromCell() != null ? item.getFromCell().getCode() : "N/A",
                        item.getToCell() != null ? item.getToCell().getCode() : "N/A",
                        op.getCompletedAt()
                )))
                .collect(Collectors.toList());
        return new MovementReport(movements);
    }

    public TopProductReport getTopProductsReport(int topN, LocalDateTime start, LocalDateTime end) {
        List<Operation> incomeOps = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.INCOME);
        List<Operation> outcomeOps = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.OUTCOME);
        List<OperationItem> allItems = List.of(incomeOps, outcomeOps).stream()
                .flatMap(List::stream)
                .flatMap(op -> op.getItems().stream())
                .collect(Collectors.toList());

        Map<Long, Object[]> grouped = allItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new Object[]{
                                        list.stream().mapToInt(OperationItem::getQuantity).sum(),
                                        (long) list.size()
                                }
                        )
                ));

        List<TopProductReport.TopProductItem> topItems = grouped.entrySet().stream()
                .sorted((e1, e2) -> ((Integer)e2.getValue()[0]).compareTo((Integer)e1.getValue()[0]))
                .limit(topN)
                .map(entry -> {
                    Product p = productRepository.findById(entry.getKey())
                            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
                    return new TopProductReport.TopProductItem(
                            p.getSku(),
                            p.getName(),
                            (Integer) entry.getValue()[0],
                            (Long) entry.getValue()[1]
                    );
                })
                .collect(Collectors.toList());

        return new TopProductReport(topItems);
    }

    // --- РЕАЛИЗАЦИЯ метода поставщиков ---
    public SupplierStatsReport getSupplierStatsReport(LocalDateTime start, LocalDateTime end) {
        // Находим все завершенные приходные операции за период
        List<Operation> incomeOps = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.INCOME);

        // Группируем по контрагенту (поставщику), суммируем количество операций и общее количество товара
        Map<Counterparty, Object[]> grouped = incomeOps.stream()
                .filter(op -> op.getCounterparty() != null) // Исключаем внутренние операции без контрагента
                .collect(Collectors.groupingBy(
                        Operation::getCounterparty, // Группируем по объекту Counterparty
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                ops -> new Object[]{
                                        (long) ops.size(), // Количество операций
                                        ops.stream().mapToLong(op -> op.getItems().stream().mapToInt(item -> item.getQuantity()).sum()).sum() // Общее количество товара
                                }
                        )
                ));

        // Преобразуем в DTO
        List<SupplierStatsReport.SupplierStatItem> stats = grouped.entrySet().stream()
                .map(entry -> new SupplierStatsReport.SupplierStatItem(
                        entry.getKey().getName(), // Имя поставщика
                        (Long) entry.getValue()[0], // Количество операций
                        Math.toIntExact((Long) entry.getValue()[1]) // Общее количество товара (Long -> Integer)
                ))
                .collect(Collectors.toList());

        return new SupplierStatsReport(stats);
    }
    // ---------------------------------------

    /**
     * Отчет о загруженности складских ячеек.
     * Предполагает, что StorageCell хранит maxVolume, calculatedVolume, currentVolume, maxWeight, currentWeight.
     */
    public List<CellUtilizationReport.CellUtilizationItem> getCellUtilizationReport() {
        List<StorageCell> cells = storageCellRepository.findAll();
        return cells.stream()
                .map(cell -> new CellUtilizationReport.CellUtilizationItem(
                        cell.getCode(),
                        cell.getCurrentVolumeCm3(),
                        cell.getMaxVolumeCm3(),
                        cell.getCurrentWeightKg(),
                        cell.getMaxWeightKg()
                ))
                .collect(Collectors.toList());
    }

    // --- Добавим новый метод: ABC-анализ ---
    public ABCAnalysisReport getABCAnalysisReport(LocalDateTime periodStart, LocalDateTime periodEnd) {
        // 1. Получить оборот (количество или стоимость) каждого товара за период
        List<TopProductReport.TopProductItem> topProducts = getTopProductsReport(Integer.MAX_VALUE, periodStart, periodEnd).topProducts();

        if (topProducts.isEmpty()) {
            return new ABCAnalysisReport(List.of());
        }

        // 2. Вычислить общий оборот
        long totalTurnover = topProducts.stream().mapToLong(item -> (long)item.totalQuantity()).sum();

        List<ABCAnalysisReport.ABCItem> abcItems = new ArrayList<>();
        double cumulativePercentage = 0.0;
        for (TopProductReport.TopProductItem item : topProducts) {
            double percentage = totalTurnover > 0 ? (item.totalQuantity() / (double) totalTurnover) * 100.0 : 0.0;
            cumulativePercentage += percentage;
            String category = cumulativePercentage <= 80.0 ? "A" : cumulativePercentage <= 95.0 ? "B" : "C";
            abcItems.add(new ABCAnalysisReport.ABCItem(
                    item.productSku(),
                    item.productName(),
                    item.totalQuantity(),
                    cumulativePercentage,
                    category
            ));
        }

        return new ABCAnalysisReport(abcItems);
    }
}
