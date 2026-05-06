package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.WarehouseRequest;
import com.cuba.warehousesystem.dto.WarehouseResponse;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseResponse create(WarehouseRequest request) {
        warehouseRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new BadRequestException("Warehouse with code already exists: " + request.code());
        });
        Warehouse warehouse = new Warehouse();
        apply(warehouse, request);
        return toResponse(warehouseRepository.save(warehouse));
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long id) {
        return toResponse(findWarehouse(id));
    }

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAll(Pageable pageable) {
        return warehouseRepository.findAll(pageable).map(this::toResponse);
    }

    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = findWarehouse(id);
        warehouseRepository.findByCode(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Warehouse with code already exists: " + request.code());
                });
        apply(warehouse, request);
        return toResponse(warehouseRepository.save(warehouse));
    }

    public void delete(Long id) {
        Warehouse warehouse = findWarehouse(id);
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
    }

    private void apply(Warehouse warehouse, WarehouseRequest request) {
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setAddress(request.address());
        warehouse.setIsActive(request.isActive() == null || request.isActive());
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getIsActive(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}
