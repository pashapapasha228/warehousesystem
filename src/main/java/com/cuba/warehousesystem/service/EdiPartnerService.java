package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.EdiPartnerRequest;
import com.cuba.warehousesystem.dto.EdiPartnerResponse;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.EdiPartner;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.CounterpartyRepository;
import com.cuba.warehousesystem.repository.EdiPartnerRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EdiPartnerService {

    private final EdiPartnerRepository ediPartnerRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final WarehouseRepository warehouseRepository;

    public EdiPartnerResponse create(EdiPartnerRequest request) {
        if (ediPartnerRepository.existsByCode(request.code())) {
            throw new BadRequestException("EDI partner with code already exists: " + request.code());
        }
        EdiPartner partner = new EdiPartner();
        apply(partner, request);
        return toResponse(ediPartnerRepository.save(partner));
    }

    @Transactional(readOnly = true)
    public EdiPartnerResponse getById(Long id) {
        return toResponse(findPartner(id));
    }

    @Transactional(readOnly = true)
    public Page<EdiPartnerResponse> getAll(Pageable pageable) {
        return ediPartnerRepository.findAll(pageable).map(this::toResponse);
    }

    public EdiPartnerResponse update(Long id, EdiPartnerRequest request) {
        EdiPartner partner = findPartner(id);
        ediPartnerRepository.findByCode(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("EDI partner with code already exists: " + request.code());
                });
        apply(partner, request);
        return toResponse(ediPartnerRepository.save(partner));
    }

    public void delete(Long id) {
        EdiPartner partner = findPartner(id);
        partner.setIsActive(false);
        ediPartnerRepository.save(partner);
    }

    private void apply(EdiPartner partner, EdiPartnerRequest request) {
        partner.setCode(request.code());
        partner.setName(request.name());
        partner.setGln(request.gln());
        partner.setCounterparty(request.counterpartyId() == null ? null : findCounterparty(request.counterpartyId()));
        partner.setDefaultWarehouse(request.defaultWarehouseId() == null ? null : findWarehouse(request.defaultWarehouseId()));
        partner.setInboundEnabled(request.inboundEnabled() == null || request.inboundEnabled());
        partner.setOutboundEnabled(request.outboundEnabled() != null && request.outboundEnabled());
        partner.setIsActive(request.isActive() == null || request.isActive());
    }

    private EdiPartner findPartner(Long id) {
        return ediPartnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EDI partner not found"));
    }

    private Counterparty findCounterparty(Long id) {
        return counterpartyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Counterparty not found"));
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
    }

    private EdiPartnerResponse toResponse(EdiPartner partner) {
        return new EdiPartnerResponse(
                partner.getId(),
                partner.getCode(),
                partner.getName(),
                partner.getGln(),
                partner.getCounterparty() == null ? null : partner.getCounterparty().getId(),
                partner.getCounterparty() == null ? null : partner.getCounterparty().getName(),
                partner.getDefaultWarehouse() == null ? null : partner.getDefaultWarehouse().getId(),
                partner.getDefaultWarehouse() == null ? null : partner.getDefaultWarehouse().getCode(),
                partner.getInboundEnabled(),
                partner.getOutboundEnabled(),
                partner.getIsActive(),
                partner.getCreatedAt(),
                partner.getUpdatedAt()
        );
    }
}
