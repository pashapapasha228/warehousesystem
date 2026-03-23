package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.ProductAlert;
import com.cuba.warehousesystem.dto.TurnoverReport;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OperationRepository operationRepository;
    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;

    public TurnoverReport calculateTurnover(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> summary = new HashMap<>();
        List<Operation> incomes = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.INCOME);
        List<Operation> outcomes = operationRepository.findCompletedOperationsByTypeAndPeriod(start, end, OperationType.OUTCOME);

        long totalIn = incomes.stream().mapToLong(op -> op.getItems().stream().mapToInt(item -> item.getQuantity()).sum()).sum();
        long totalOut = outcomes.stream().mapToLong(op -> op.getItems().stream().mapToInt(item -> item.getQuantity()).sum()).sum();

        summary.put("period_start", start);
        summary.put("period_end", end);
        summary.put("total_incoming", totalIn);
        summary.put("total_outgoing", totalOut);
        summary.put("net_change", totalIn - totalOut);

        return new TurnoverReport(summary);
    }

    public List<ProductAlert> findLowStockAlerts() {
        // В реальном проекте здесь был бы более сложный запрос или расчет
        // Здесь упрощение: просто возвращаем все продукты с остатком 0
        // В дипломе можно указать, что в реальности это будет зависеть от min_stock_level
        List<ProductAlert> alerts = new ArrayList<>();
        var balances = stockBalanceRepository.findAll();
        for (var balance : balances) {
            if (balance.getQuantity() == 0) { // Простое правило для примера
                alerts.add(new ProductAlert(
                        balance.getProduct().getName(),
                        balance.getProduct().getSku(),
                        balance.getQuantity(),
                        0 // Минимальный уровень
                ));
            }
        }
        return alerts;
    }
}
