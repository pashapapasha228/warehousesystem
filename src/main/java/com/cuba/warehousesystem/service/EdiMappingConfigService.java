package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.EdiMappingConfigRequest;
import com.cuba.warehousesystem.dto.EdiMappingConfigResponse;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.EdiMappingConfig;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiPartner;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.repository.EdiMappingConfigRepository;
import com.cuba.warehousesystem.repository.EdiPartnerRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class EdiMappingConfigService {

    private final EdiMappingConfigRepository ediMappingConfigRepository;
    private final EdiPartnerRepository ediPartnerRepository;
    private final ProductRepository productRepository;

    public EdiMappingConfigResponse create(EdiMappingConfigRequest request) {
        EdiMappingConfig mapping = new EdiMappingConfig();
        apply(mapping, request);
        return toResponse(ediMappingConfigRepository.save(mapping));
    }

    @Transactional(readOnly = true)
    public EdiMappingConfigResponse getById(Long id) {
        return toResponse(findMapping(id));
    }

    @Transactional(readOnly = true)
    public Page<EdiMappingConfigResponse> getAll(String search, Long partnerId, Pageable pageable) {
        if (StringUtils.hasText(search) && partnerId != null) {
            return ediMappingConfigRepository.searchByPartner(partnerId, search.trim(), pageable).map(this::toResponse);
        }
        if (StringUtils.hasText(search)) {
            return ediMappingConfigRepository.search(search.trim(), pageable).map(this::toResponse);
        }
        if (partnerId != null) {
            return ediMappingConfigRepository.findByPartner_Id(partnerId, pageable).map(this::toResponse);
        }
        return ediMappingConfigRepository.findAll(pageable).map(this::toResponse);
    }

    public EdiMappingConfigResponse update(Long id, EdiMappingConfigRequest request) {
        EdiMappingConfig mapping = findMapping(id);
        apply(mapping, request);
        return toResponse(ediMappingConfigRepository.save(mapping));
    }

    public void delete(Long id) {
        EdiMappingConfig mapping = findMapping(id);
        mapping.setIsActive(false);
        ediMappingConfigRepository.save(mapping);
    }

    private void apply(EdiMappingConfig mapping, EdiMappingConfigRequest request) {
        EdiPartner partner = ediPartnerRepository.findById(request.partnerId())
                .orElseThrow(() -> new EntityNotFoundException("EDI partner not found"));
        Product product = productRepository.findById(request.internalProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        mapping.setPartner(partner);
        if (mapping.getMessageType() == null) {
            mapping.setMessageType(EdiMessageType.DESADV);
        }
        mapping.setExternalProductCode(request.externalProductCode());
        mapping.setInternalProduct(product);
        mapping.setIsActive(request.isActive() == null || request.isActive());
    }

    private EdiMappingConfig findMapping(Long id) {
        return ediMappingConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EDI mapping config not found"));
    }

    private EdiMappingConfigResponse toResponse(EdiMappingConfig mapping) {
        return new EdiMappingConfigResponse(
                mapping.getId(),
                mapping.getPartner().getId(),
                mapping.getPartner().getCode(),
                mapping.getMessageType(),
                mapping.getExternalProductCode(),
                mapping.getInternalProduct().getId(),
                mapping.getInternalProduct().getSku(),
                mapping.getInternalProduct().getName(),
                mapping.getIsActive(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt()
        );
    }
}
