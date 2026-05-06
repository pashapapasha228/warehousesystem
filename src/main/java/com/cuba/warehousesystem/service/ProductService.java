package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.ProductRequest;
import com.cuba.warehousesystem.dto.ProductResponse;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

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
        product.setUnitOfMeasure(defaultString(request.unitOfMeasure(), "pcs"));
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
                product.getUnitOfMeasure(),
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

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
