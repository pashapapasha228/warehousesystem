package com.cuba.warehousesystem.service;

import com.cuba.warehousesystem.dto.CounterpartyRequest;
import com.cuba.warehousesystem.dto.EdiMappingConfigRequest;
import com.cuba.warehousesystem.dto.EdiPartnerRequest;
import com.cuba.warehousesystem.dto.ProductRequest;
import com.cuba.warehousesystem.dto.ProductWarehouseMinStockRequest;
import com.cuba.warehousesystem.dto.StorageCellRequest;
import com.cuba.warehousesystem.dto.UserCreateRequest;
import com.cuba.warehousesystem.dto.UserUpdateRequest;
import com.cuba.warehousesystem.dto.WarehouseRequest;
import com.cuba.warehousesystem.exception.BadRequestException;
import com.cuba.warehousesystem.exception.EntityNotFoundException;
import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.CounterpartyType;
import com.cuba.warehousesystem.model.EdiMappingConfig;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiPartner;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.ProductCategory;
import com.cuba.warehousesystem.model.ProductWarehouseMinStock;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.UserRole;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.CounterpartyRepository;
import com.cuba.warehousesystem.repository.EdiMappingConfigRepository;
import com.cuba.warehousesystem.repository.EdiPartnerRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.ProductWarehouseMinStockRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.UserRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServicesTest {

    @Test
    void warehouseCreateAppliesDefaultsAndRejectsDuplicateCode() {
        WarehouseRepository repository = mock(WarehouseRepository.class);
        WarehouseService service = new WarehouseService(repository);
        Warehouse saved = warehouse(1L, "WH-1");
        when(repository.save(any(Warehouse.class))).thenReturn(saved);

        assertThat(service.create(new WarehouseRequest("WH-1", "Main warehouse", "Minsk", null)).isActive())
                .isTrue();

        when(repository.findByCode("WH-1")).thenReturn(Optional.of(saved));
        assertThatThrownBy(() -> service.create(new WarehouseRequest("WH-1", "Duplicate", null, true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void warehouseUpdateAllowsSameCodeButRejectsAnotherWarehouseCode() {
        WarehouseRepository repository = mock(WarehouseRepository.class);
        WarehouseService service = new WarehouseService(repository);
        Warehouse current = warehouse(1L, "WH-1");
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.findByCode("WH-1")).thenReturn(Optional.of(current));
        when(repository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.update(1L, new WarehouseRequest("WH-1", "Updated", "Address", false)).name())
                .isEqualTo("Updated");

        when(repository.findByCode("WH-2")).thenReturn(Optional.of(warehouse(2L, "WH-2")));
        assertThatThrownBy(() -> service.update(1L, new WarehouseRequest("WH-2", "Updated", null, true)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void storageCellCreateDefaultsCapacityFieldsAndChecksWarehouseScopedCode() {
        Warehouse warehouse = warehouse(1L, "WH-1");
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        StorageCellRepository cellRepository = mock(StorageCellRepository.class);
        StorageCellService service = new StorageCellService(cellRepository, warehouseRepository);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(cellRepository.save(any(StorageCell.class))).thenAnswer(invocation -> {
            StorageCell cell = invocation.getArgument(0);
            cell.setId(10L);
            return cell;
        });

        var response = service.create(new StorageCellRequest(
                1L, "A-01", "A", null, null, null,
                null, null, null, null, null, null, null
        ));

        assertThat(response.capacityUnits()).isZero();
        assertThat(response.maxWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.isActive()).isTrue();

        when(cellRepository.findByWarehouse_IdAndCode(1L, "A-01")).thenReturn(Optional.of(cell(10L, "A-01", warehouse)));
        assertThatThrownBy(() -> service.create(new StorageCellRequest(
                1L, "A-01", null, null, null, null,
                1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, true
        ))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void storageCellGetAllChoosesSearchWarehouseFilterOrAll() {
        StorageCellRepository cellRepository = mock(StorageCellRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        StorageCellService service = new StorageCellService(cellRepository, warehouseRepository);
        var pageable = PageRequest.of(0, 10);

        when(cellRepository.search(1L, "A", pageable)).thenReturn(new PageImpl<>(List.of()));
        when(cellRepository.findByWarehouse_Id(1L, pageable)).thenReturn(new PageImpl<>(List.of()));
        when(cellRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        service.getAll(1L, " A ", pageable);
        service.getAll(1L, null, pageable);
        service.getAll(null, null, pageable);

        verify(cellRepository).search(1L, "A", pageable);
        verify(cellRepository).findByWarehouse_Id(1L, pageable);
        verify(cellRepository).findAll(pageable);
    }

    @Test
    void productCreateCalculatesVolumeAndSetWarehouseMinimumClearsDashboardCache() {
        ProductRepository productRepository = mock(ProductRepository.class);
        StockBalanceRepository stockBalanceRepository = mock(StockBalanceRepository.class);
        OperationRepository operationRepository = mock(OperationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        ProductWarehouseMinStockRepository minStockRepository = mock(ProductWarehouseMinStockRepository.class);
        OperationService operationService = mock(OperationService.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache dashboardCache = mock(Cache.class);
        ProductService service = new ProductService(
                productRepository,
                stockBalanceRepository,
                operationRepository,
                warehouseRepository,
                minStockRepository,
                operationService,
                cacheManager
        );

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return product;
        });

        var created = service.create(new ProductRequest(
                "SKU-1", "BC-1", "Product", null,
                BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(5), null
        ));

        assertThat(created.category()).isEqualTo(ProductCategory.OTHER);
        assertThat(created.volumePerUnitCm3()).isEqualByComparingTo("60");

        Product product = product(1L, "SKU-1");
        Warehouse warehouse = warehouse(2L, "WH-2");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(minStockRepository.save(any(ProductWarehouseMinStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cacheManager.getCache("dashboard")).thenReturn(dashboardCache);

        var minimum = service.setWarehouseMinStock(1L, new ProductWarehouseMinStockRequest(2L, 7));

        assertThat(minimum.minStockLevel()).isEqualTo(7);
        verify(dashboardCache).clear();
    }

    @Test
    void productCardAggregatesBalancesByWarehouseAndIncludesMinimums() {
        Product product = product(1L, "SKU-1");
        Warehouse warehouse = warehouse(2L, "WH-2");
        StorageCell first = cell(10L, "A-01", warehouse);
        StorageCell second = cell(11L, "A-02", warehouse);
        StockBalance firstBalance = new StockBalance(product, first, 5);
        firstBalance.setReservedQuantity(1);
        StockBalance secondBalance = new StockBalance(product, second, 7);
        secondBalance.setReservedQuantity(2);

        ProductRepository productRepository = mock(ProductRepository.class);
        StockBalanceRepository stockBalanceRepository = mock(StockBalanceRepository.class);
        OperationRepository operationRepository = mock(OperationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        ProductWarehouseMinStockRepository minStockRepository = mock(ProductWarehouseMinStockRepository.class);
        ProductService service = new ProductService(
                productRepository,
                stockBalanceRepository,
                operationRepository,
                warehouseRepository,
                minStockRepository,
                mock(OperationService.class),
                mock(CacheManager.class)
        );

        ProductWarehouseMinStock minStock = new ProductWarehouseMinStock();
        minStock.setProduct(product);
        minStock.setWarehouse(warehouse);
        minStock.setMinStockLevel(4);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockBalanceRepository.findByProduct_Id(1L)).thenReturn(List.of(firstBalance, secondBalance));
        when(minStockRepository.findByProduct_Id(1L)).thenReturn(List.of(minStock));
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));
        when(operationRepository.findRecentByProduct(1L, null, PageRequest.of(0, 8))).thenReturn(List.of());

        var card = service.getCard(1L, null);

        assertThat(card.totalQuantity()).isEqualTo(12);
        assertThat(card.totalAvailableQuantity()).isEqualTo(9);
        assertThat(card.warehouseAggregates()).singleElement()
                .satisfies(aggregate -> {
                    assertThat(aggregate.quantity()).isEqualTo(12);
                    assertThat(aggregate.minStockLevel()).isEqualTo(4);
                });
        assertThat(card.placements()).hasSize(2);
    }

    @Test
    void userServiceEncodesPasswordsAndKeepsExistingPasswordWhenBlank() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserService service = new UserService(userRepository, encoder);

        when(encoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        var created = service.create(new UserCreateRequest("manager", "secret", "Manager", "m@example.com", UserRole.MANAGER, null));

        assertThat(created.username()).isEqualTo("manager");
        verify(encoder).encode("secret");

        User user = user(1L, "manager");
        user.setPasswordHash("existing");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.update(1L, new UserUpdateRequest(" ", "Updated", "u@example.com", UserRole.ADMIN, true));

        assertThat(user.getPasswordHash()).isEqualTo("existing");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void counterpartyCrudRejectsDuplicateCodeAndSoftDeletes() {
        CounterpartyRepository repository = mock(CounterpartyRepository.class);
        CounterpartyService service = new CounterpartyService(repository);
        Counterparty counterparty = counterparty(1L, "SUP-1", CounterpartyType.SUPPLIER);

        when(repository.save(any(Counterparty.class))).thenAnswer(invocation -> {
            Counterparty saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var created = service.create(counterpartyRequest("SUP-2"));
        assertThat(created.code()).isEqualTo("SUP-2");

        when(repository.findByCode("SUP-1")).thenReturn(Optional.of(counterparty));
        assertThatThrownBy(() -> service.create(counterpartyRequest("SUP-1")))
                .isInstanceOf(BadRequestException.class);

        when(repository.findById(1L)).thenReturn(Optional.of(counterparty));
        when(repository.findByCode("SUP-3")).thenReturn(Optional.empty());

        var updated = service.update(1L, counterpartyRequest("SUP-3"));
        assertThat(updated.code()).isEqualTo("SUP-3");

        service.delete(1L);

        assertThat(counterparty.getIsActive()).isFalse();
    }

    @Test
    void counterpartyGetAllUsesSearchOrFindAll() {
        CounterpartyRepository repository = mock(CounterpartyRepository.class);
        CounterpartyService service = new CounterpartyService(repository);
        var pageable = PageRequest.of(0, 10);
        when(repository.search("sup", pageable)).thenReturn(new PageImpl<>(List.of(counterparty(1L, "SUP-1", CounterpartyType.SUPPLIER))));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(counterparty(2L, "CUS-1", CounterpartyType.CUSTOMER))));

        assertThat(service.getAll(" sup ", pageable).getContent()).hasSize(1);
        assertThat(service.getAll(null, pageable).getContent()).hasSize(1);

        verify(repository).search("sup", pageable);
        verify(repository).findAll(pageable);
    }

    @Test
    void ediPartnerResolvesCounterpartyDeduplicatesWarehousesAndSortsWarehouseResponses() {
        EdiPartnerRepository partnerRepository = mock(EdiPartnerRepository.class);
        CounterpartyRepository counterpartyRepository = mock(CounterpartyRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        EdiPartnerService service = new EdiPartnerService(partnerRepository, counterpartyRepository, warehouseRepository);

        Counterparty counterparty = counterparty(1L, "SUP-1", CounterpartyType.SUPPLIER);
        Warehouse b = warehouse(20L, "B");
        Warehouse a = warehouse(10L, "A");

        when(counterpartyRepository.findById(1L)).thenReturn(Optional.of(counterparty));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(b));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(a));
        when(partnerRepository.save(any(EdiPartner.class))).thenAnswer(invocation -> {
            EdiPartner partner = invocation.getArgument(0);
            partner.setId(100L);
            return partner;
        });

        var response = service.create(new EdiPartnerRequest("EDI-1", "Partner", "123", 1L, List.of(20L, 10L, 20L), null, true, null));

        assertThat(response.counterpartyId()).isEqualTo(1L);
        assertThat(response.warehouseIds()).containsExactly(20L, 10L);
        assertThat(response.warehouses()).extracting("code").containsExactly("A", "B");
        assertThat(response.inboundEnabled()).isTrue();
        assertThat(response.outboundEnabled()).isTrue();
    }

    @Test
    void ediPartnerUpdateRejectsDuplicateCodeAndSoftDeletes() {
        EdiPartnerRepository partnerRepository = mock(EdiPartnerRepository.class);
        CounterpartyRepository counterpartyRepository = mock(CounterpartyRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        EdiPartnerService service = new EdiPartnerService(partnerRepository, counterpartyRepository, warehouseRepository);
        EdiPartner partner = new EdiPartner();
        partner.setId(1L);
        partner.setCode("EDI-1");
        partner.setName("Old");
        EdiPartner other = new EdiPartner();
        other.setId(2L);
        other.setCode("EDI-2");

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
        when(partnerRepository.findByCode("EDI-2")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L, new EdiPartnerRequest("EDI-2", "Partner", null, null, List.of(), true, false, true)))
                .isInstanceOf(BadRequestException.class);

        when(partnerRepository.findByCode("EDI-3")).thenReturn(Optional.empty());
        when(partnerRepository.save(any(EdiPartner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update(1L, new EdiPartnerRequest("EDI-3", "Updated", null, null, null, false, null, false));
        assertThat(updated.code()).isEqualTo("EDI-3");
        assertThat(updated.inboundEnabled()).isFalse();
        assertThat(updated.outboundEnabled()).isFalse();
        assertThat(updated.isActive()).isFalse();

        service.delete(1L);
        assertThat(partner.getIsActive()).isFalse();
    }

    @Test
    void ediMappingCreateDefaultsMessageTypeAndResolvesReferences() {
        EdiMappingConfigRepository mappingRepository = mock(EdiMappingConfigRepository.class);
        EdiPartnerRepository partnerRepository = mock(EdiPartnerRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        EdiMappingConfigService service = new EdiMappingConfigService(mappingRepository, partnerRepository, productRepository);

        EdiPartner partner = new EdiPartner();
        partner.setId(1L);
        partner.setCode("EDI-1");
        Product product = product(2L, "SKU-2");

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(mappingRepository.save(any(EdiMappingConfig.class))).thenAnswer(invocation -> {
            EdiMappingConfig mapping = invocation.getArgument(0);
            mapping.setId(3L);
            return mapping;
        });

        var response = service.create(new EdiMappingConfigRequest(1L, "EXT-2", 2L, null));

        assertThat(response.messageType()).isEqualTo(EdiMessageType.DESADV);
        assertThat(response.partnerCode()).isEqualTo("EDI-1");
        assertThat(response.internalSku()).isEqualTo("SKU-2");

        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new EdiMappingConfigRequest(1L, "EXT-99", 99L, true)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void ediMappingGetAllCoversSearchAndPartnerFiltersAndDelete() {
        EdiMappingConfigRepository mappingRepository = mock(EdiMappingConfigRepository.class);
        EdiPartnerRepository partnerRepository = mock(EdiPartnerRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        EdiMappingConfigService service = new EdiMappingConfigService(mappingRepository, partnerRepository, productRepository);
        var pageable = PageRequest.of(0, 10);
        EdiMappingConfig mapping = mapping(1L);

        when(mappingRepository.searchByPartner(1L, "ext", pageable)).thenReturn(new PageImpl<>(List.of(mapping)));
        when(mappingRepository.search("ext", pageable)).thenReturn(new PageImpl<>(List.of(mapping)));
        when(mappingRepository.findByPartner_Id(1L, pageable)).thenReturn(new PageImpl<>(List.of(mapping)));
        when(mappingRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(mapping)));

        assertThat(service.getAll(" ext ", 1L, pageable)).hasSize(1);
        assertThat(service.getAll(" ext ", null, pageable)).hasSize(1);
        assertThat(service.getAll(null, 1L, pageable)).hasSize(1);
        assertThat(service.getAll(null, null, pageable)).hasSize(1);

        when(mappingRepository.findById(1L)).thenReturn(Optional.of(mapping));
        when(mappingRepository.save(any(EdiMappingConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(1L);

        assertThat(mapping.getIsActive()).isFalse();
    }

    @Test
    void userWarehouseStorageAndProductDeletePathsSoftDeactivateEntities() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserService userService = new UserService(userRepository, encoder);
        User user = user(1L, "manager");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        userService.delete(1L);
        assertThat(user.getIsActive()).isFalse();

        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        WarehouseService warehouseService = new WarehouseService(warehouseRepository);
        Warehouse warehouse = warehouse(1L, "WH-1");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        warehouseService.delete(1L);
        assertThat(warehouse.getIsActive()).isFalse();

        StorageCellRepository cellRepository = mock(StorageCellRepository.class);
        StorageCellService cellService = new StorageCellService(cellRepository, warehouseRepository);
        StorageCell cell = cell(10L, "A-01", warehouse);
        when(cellRepository.findById(10L)).thenReturn(Optional.of(cell));
        when(cellRepository.save(any(StorageCell.class))).thenAnswer(invocation -> invocation.getArgument(0));
        cellService.delete(10L);
        assertThat(cell.getIsActive()).isFalse();

        ProductRepository productRepository = mock(ProductRepository.class);
        ProductService productService = new ProductService(
                productRepository,
                mock(StockBalanceRepository.class),
                mock(OperationRepository.class),
                warehouseRepository,
                mock(ProductWarehouseMinStockRepository.class),
                mock(OperationService.class),
                mock(CacheManager.class)
        );
        Product product = product(1L, "SKU-1");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        productService.delete(1L);
        assertThat(product.getIsActive()).isFalse();
    }

    private Warehouse warehouse(Long id, String code) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setCode(code);
        warehouse.setName(code + " name");
        warehouse.setIsActive(true);
        return warehouse;
    }

    private StorageCell cell(Long id, String code, Warehouse warehouse) {
        StorageCell cell = new StorageCell();
        cell.setId(id);
        cell.setCode(code);
        cell.setWarehouse(warehouse);
        cell.setCapacityUnits(100);
        cell.setMaxWeightKg(BigDecimal.valueOf(1000));
        cell.setMaxVolumeCm3(BigDecimal.valueOf(1000));
        cell.setLengthCm(BigDecimal.valueOf(100));
        cell.setWidthCm(BigDecimal.valueOf(100));
        cell.setHeightCm(BigDecimal.valueOf(100));
        cell.setCurrentWeightKg(BigDecimal.ZERO);
        cell.setCurrentVolumeCm3(BigDecimal.ZERO);
        cell.setIsActive(true);
        return cell;
    }

    private Product product(Long id, String sku) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(sku + " name");
        product.setCategory(ProductCategory.OTHER);
        product.setWeightPerUnitKg(BigDecimal.ONE);
        product.setLengthCm(BigDecimal.ONE);
        product.setWidthCm(BigDecimal.ONE);
        product.setHeightCm(BigDecimal.ONE);
        product.setVolumePerUnitCm3(BigDecimal.ONE);
        product.setIsActive(true);
        return product;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFullName(username);
        user.setRole(UserRole.MANAGER);
        user.setIsActive(true);
        return user;
    }

    private Counterparty counterparty(Long id, String code, CounterpartyType type) {
        Counterparty counterparty = new Counterparty();
        counterparty.setId(id);
        counterparty.setCode(code);
        counterparty.setName(code + " name");
        counterparty.setType(type);
        counterparty.setIsActive(true);
        return counterparty;
    }

    private CounterpartyRequest counterpartyRequest(String code) {
        return new CounterpartyRequest(code, code + " name", CounterpartyType.SUPPLIER, null, null, null, null, null, null, true);
    }

    private EdiMappingConfig mapping(Long id) {
        EdiPartner partner = new EdiPartner();
        partner.setId(1L);
        partner.setCode("EDI-1");
        Product product = product(1L, "SKU-1");
        EdiMappingConfig mapping = new EdiMappingConfig();
        mapping.setId(id);
        mapping.setPartner(partner);
        mapping.setMessageType(EdiMessageType.DESADV);
        mapping.setExternalProductCode("EXT-1");
        mapping.setInternalProduct(product);
        mapping.setIsActive(true);
        return mapping;
    }
}
