package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.CounterpartyRequest;
import com.cuba.warehousesystem.dto.CounterpartyResponse;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.repository.CounterpartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CounterpartyService {

    private final CounterpartyRepository counterpartyRepository;

    public CounterpartyResponse create(CounterpartyRequest request) {
        counterpartyRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new BadRequestException("Counterparty with code already exists: " + request.code());
        });
        Counterparty counterparty = new Counterparty();
        apply(counterparty, request);
        return toResponse(counterpartyRepository.save(counterparty));
    }

    @Transactional(readOnly = true)
    public CounterpartyResponse getById(Long id) {
        return toResponse(findCounterparty(id));
    }

    @Transactional(readOnly = true)
    public Page<CounterpartyResponse> getAll(Pageable pageable) {
        return counterpartyRepository.findAll(pageable).map(this::toResponse);
    }

    public CounterpartyResponse update(Long id, CounterpartyRequest request) {
        Counterparty counterparty = findCounterparty(id);
        counterpartyRepository.findByCode(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Counterparty with code already exists: " + request.code());
                });
        apply(counterparty, request);
        return toResponse(counterpartyRepository.save(counterparty));
    }

    public void delete(Long id) {
        Counterparty counterparty = findCounterparty(id);
        counterparty.setIsActive(false);
        counterpartyRepository.save(counterparty);
    }

    private Counterparty findCounterparty(Long id) {
        return counterpartyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Counterparty not found"));
    }

    private void apply(Counterparty counterparty, CounterpartyRequest request) {
        counterparty.setCode(request.code());
        counterparty.setName(request.name());
        counterparty.setType(request.type());
        counterparty.setTaxId(request.taxId());
        counterparty.setGln(request.gln());
        counterparty.setEmail(request.email());
        counterparty.setPhone(request.phone());
        counterparty.setAddress(request.address());
        counterparty.setContactInfo(request.contactInfo());
        counterparty.setIsActive(request.isActive() == null || request.isActive());
    }

    private CounterpartyResponse toResponse(Counterparty counterparty) {
        return new CounterpartyResponse(
                counterparty.getId(),
                counterparty.getCode(),
                counterparty.getName(),
                counterparty.getType(),
                counterparty.getTaxId(),
                counterparty.getGln(),
                counterparty.getEmail(),
                counterparty.getPhone(),
                counterparty.getAddress(),
                counterparty.getContactInfo(),
                counterparty.getIsActive(),
                counterparty.getCreatedAt(),
                counterparty.getUpdatedAt()
        );
    }
}
