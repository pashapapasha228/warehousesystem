package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.ProductCardResponse;
import com.cuba.warehousesystem.dto.ProductRequest;
import com.cuba.warehousesystem.dto.ProductResponse;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final OperationRepository operationRepository;
    private final OperationService operationService;

    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BadRequestException("Product with SKU already exists: " + request.sku());
        }
        Product product = new Product();
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findProduct(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductCardResponse getCard(Long id, Long warehouseId) {
        Product product = findProduct(id);
        List<StockBalance> balances = warehouseId == null
                ? stockBalanceRepository.findByProduct_Id(id)
                : stockBalanceRepository.findByProduct_IdAndCell_Warehouse_Id(id, warehouseId);

        int totalQuantity = balances.stream().mapToInt(StockBalance::getQuantity).sum();
        int totalReserved = balances.stream().mapToInt(StockBalance::getReservedQuantity).sum();

        Map<Long, ProductCardResponse.WarehouseAggregate> aggregates = new LinkedHashMap<>();
        for (StockBalance balance : balances) {
            Long currentWarehouseId = balance.getCell().getWarehouse().getId();
            ProductCardResponse.WarehouseAggregate existing = aggregates.get(currentWarehouseId);
            int quantity = balance.getQuantity() + (existing == null ? 0 : existing.quantity());
            int reserved = balance.getReservedQuantity() + (existing == null ? 0 : existing.reservedQuantity());
            aggregates.put(currentWarehouseId, new ProductCardResponse.WarehouseAggregate(
                    currentWarehouseId,
                    balance.getCell().getWarehouse().getCode(),
                    quantity,
                    reserved,
                    quantity - reserved
            ));
        }

        List<Operation> recentOperations = operationRepository.findRecentByProduct(id, warehouseId, PageRequest.of(0, 8));

        return new ProductCardResponse(
                toResponse(product),
                totalQuantity,
                totalReserved,
                totalQuantity - totalReserved,
                List.copyOf(aggregates.values()),
                balances.stream()
                        .map(balance -> new ProductCardResponse.Placement(
                                balance.getCell().getWarehouse().getId(),
                                balance.getCell().getWarehouse().getCode(),
                                balance.getCell().getId(),
                                balance.getCell().getCode(),
                                balance.getQuantity(),
                                balance.getReservedQuantity(),
                                balance.getQuantity() - balance.getReservedQuantity()
                        ))
                        .toList(),
                recentOperations.stream().map(operationService::toResponse).toList()
        );
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        productRepository.findBySku(request.sku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Product with SKU already exists: " + request.sku());
                });
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    public void delete(Long id) {
        Product product = findProduct(id);
        product.setIsActive(false);
        productRepository.save(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    private void apply(Product product, ProductRequest request) {
        product.setSku(request.sku());
        product.setBarcode(request.barcode());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setMinStockLevel(defaultInteger(request.minStockLevel()));
        product.setWeightPerUnitKg(defaultDecimal(request.weightPerUnitKg()));
        product.setVolumePerUnitCm3(defaultDecimal(request.volumePerUnitCm3()));
        product.setLengthCm(defaultDecimal(request.lengthCm()));
        product.setWidthCm(defaultDecimal(request.widthCm()));
        product.setHeightCm(defaultDecimal(request.heightCm()));
        product.setIsActive(request.isActive() == null || request.isActive());
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getBarcode(),
                product.getName(),
                product.getCategory(),
                product.getMinStockLevel(),
                product.getWeightPerUnitKg(),
                product.getVolumePerUnitCm3(),
                product.getLengthCm(),
                product.getWidthCm(),
                product.getHeightCm(),
                product.getIsActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
