package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.StorageCellRequest;
import com.cuba.warehousesystem.dto.StorageCellResponse;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class StorageCellService {

    private final StorageCellRepository storageCellRepository;
    private final WarehouseRepository warehouseRepository;

    public StorageCellResponse create(StorageCellRequest request) {
        Warehouse warehouse = findWarehouse(request.warehouseId());
        ensureUniqueCode(request.warehouseId(), request.code(), null);

        StorageCell cell = new StorageCell();
        cell.setWarehouse(warehouse);
        apply(cell, request);
        return toResponse(storageCellRepository.save(cell));
    }

    @Transactional(readOnly = true)
    public StorageCellResponse getById(Long id) {
        return toResponse(findCell(id));
    }

    @Transactional(readOnly = true)
    public Page<StorageCellResponse> getAll(Long warehouseId, String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return storageCellRepository.search(warehouseId, search.trim(), pageable).map(this::toResponse);
        }
        if (warehouseId != null) {
            return storageCellRepository.findByWarehouse_Id(warehouseId, pageable).map(this::toResponse);
        }
        return storageCellRepository.findAll(pageable).map(this::toResponse);
    }

    public StorageCellResponse update(Long id, StorageCellRequest request) {
        StorageCell cell = findCell(id);
        Warehouse warehouse = findWarehouse(request.warehouseId());
        ensureUniqueCode(request.warehouseId(), request.code(), id);

        cell.setWarehouse(warehouse);
        apply(cell, request);
        return toResponse(storageCellRepository.save(cell));
    }

    public void delete(Long id) {
        StorageCell cell = findCell(id);
        cell.setIsActive(false);
        storageCellRepository.save(cell);
    }

    private StorageCell findCell(Long id) {
        return storageCellRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Storage cell not found"));
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
    }

    private void ensureUniqueCode(Long warehouseId, String code, Long currentCellId) {
        storageCellRepository.findByWarehouse_IdAndCode(warehouseId, code)
                .filter(existing -> currentCellId == null || !existing.getId().equals(currentCellId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Storage cell code already exists in warehouse: " + code);
                });
    }

    private void apply(StorageCell cell, StorageCellRequest request) {
        cell.setCode(request.code());
        cell.setZone(request.zone());
        cell.setRack(request.rack());
        cell.setShelf(request.shelf());
        cell.setLevel(request.level());
        cell.setCapacityUnits(defaultInteger(request.capacityUnits()));
        cell.setMaxWeightKg(defaultDecimal(request.maxWeightKg()));
        cell.setMaxVolumeCm3(defaultDecimal(request.maxVolumeCm3()));
        cell.setLengthCm(defaultDecimal(request.lengthCm()));
        cell.setWidthCm(defaultDecimal(request.widthCm()));
        cell.setHeightCm(defaultDecimal(request.heightCm()));
        cell.setIsActive(request.isActive() == null || request.isActive());
    }

    private StorageCellResponse toResponse(StorageCell cell) {
        return new StorageCellResponse(
                cell.getId(),
                cell.getWarehouse().getId(),
                cell.getWarehouse().getCode(),
                cell.getCode(),
                cell.getZone(),
                cell.getRack(),
                cell.getShelf(),
                cell.getLevel(),
                cell.getCapacityUnits(),
                cell.getMaxWeightKg(),
                cell.getMaxVolumeCm3(),
                cell.getLengthCm(),
                cell.getWidthCm(),
                cell.getHeightCm(),
                cell.getCurrentWeightKg(),
                cell.getCurrentVolumeCm3(),
                cell.getIsActive(),
                cell.getCreatedAt(),
                cell.getUpdatedAt()
        );
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
