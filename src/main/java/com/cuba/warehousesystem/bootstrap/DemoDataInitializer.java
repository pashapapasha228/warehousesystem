package com.cuba.warehousesystem.bootstrap;

import com.cuba.warehousesystem.model.AuditLog;
import com.cuba.warehousesystem.model.Counterparty;
import com.cuba.warehousesystem.model.CounterpartyType;
import com.cuba.warehousesystem.model.DocumentExecutionStage;
import com.cuba.warehousesystem.model.DocumentExecutionStatus;
import com.cuba.warehousesystem.model.DocumentExecutionStep;
import com.cuba.warehousesystem.model.EdiAuditLog;
import com.cuba.warehousesystem.model.EdiAuditStatus;
import com.cuba.warehousesystem.model.EdiDirection;
import com.cuba.warehousesystem.model.EdiMappingConfig;
import com.cuba.warehousesystem.model.EdiMessage;
import com.cuba.warehousesystem.model.EdiMessageStatus;
import com.cuba.warehousesystem.model.EdiMessageType;
import com.cuba.warehousesystem.model.EdiPartner;
import com.cuba.warehousesystem.model.EdiProcessingQueue;
import com.cuba.warehousesystem.model.EdiQueueStatus;
import com.cuba.warehousesystem.model.Operation;
import com.cuba.warehousesystem.model.OperationItem;
import com.cuba.warehousesystem.model.OperationSource;
import com.cuba.warehousesystem.model.OperationStatus;
import com.cuba.warehousesystem.model.OperationType;
import com.cuba.warehousesystem.model.Product;
import com.cuba.warehousesystem.model.ProductCategory;
import com.cuba.warehousesystem.model.ProductWarehouseMinStock;
import com.cuba.warehousesystem.model.StockBalance;
import com.cuba.warehousesystem.model.StorageCell;
import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.UserRole;
import com.cuba.warehousesystem.model.Warehouse;
import com.cuba.warehousesystem.repository.AuditLogRepository;
import com.cuba.warehousesystem.repository.CounterpartyRepository;
import com.cuba.warehousesystem.repository.DocumentExecutionStepRepository;
import com.cuba.warehousesystem.repository.EdiAuditLogRepository;
import com.cuba.warehousesystem.repository.EdiMappingConfigRepository;
import com.cuba.warehousesystem.repository.EdiMessageRepository;
import com.cuba.warehousesystem.repository.EdiPartnerRepository;
import com.cuba.warehousesystem.repository.EdiProcessingQueueRepository;
import com.cuba.warehousesystem.repository.OperationRepository;
import com.cuba.warehousesystem.repository.ProductRepository;
import com.cuba.warehousesystem.repository.ProductWarehouseMinStockRepository;
import com.cuba.warehousesystem.repository.StockBalanceRepository;
import com.cuba.warehousesystem.repository.StorageCellRepository;
import com.cuba.warehousesystem.repository.UserRepository;
import com.cuba.warehousesystem.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "warehouse.demo-data", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class DemoDataInitializer implements ApplicationRunner {

    private static final String DEMO_MARKER_ENTITY = "DemoData";
    private static final String DEMO_MARKER_ID = "warehouse-erp-demo-v1";

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageCellRepository storageCellRepository;
    private final ProductRepository productRepository;
    private final ProductWarehouseMinStockRepository productWarehouseMinStockRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final OperationRepository operationRepository;
    private final EdiPartnerRepository ediPartnerRepository;
    private final EdiMappingConfigRepository ediMappingConfigRepository;
    private final EdiMessageRepository ediMessageRepository;
    private final EdiProcessingQueueRepository ediProcessingQueueRepository;
    private final EdiAuditLogRepository ediAuditLogRepository;
    private final DocumentExecutionStepRepository documentExecutionStepRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting warehouse ERP demo data initialization...");

        Map<String, User> users = seedUsers();
        Map<String, Warehouse> warehouses = seedWarehouses();
        Map<String, StorageCell> cells = seedStorageCells(warehouses);
        Map<String, Product> products = seedProducts(warehouses);
        Map<String, Counterparty> counterparties = seedCounterparties();

        seedBalances(products, cells);
        seedOperations(users, warehouses, cells, products, counterparties);
        seedEdi(warehouses, products, counterparties);
        seedAuditTrail(users);

        log.info("Warehouse ERP demo data initialization completed.");
    }

    private Map<String, User> seedUsers() {
        Map<String, User> result = new LinkedHashMap<>();
        result.put("admin", upsertUser("admin", "admin123", "Администратор системы", "admin@warehouse.local", UserRole.ADMIN, true));
        result.put("manager", upsertUser("manager", "manager123", "Менеджер склада", "manager@warehouse.local", UserRole.MANAGER, true));
        result.put("storekeeper", upsertUser("storekeeper", "storekeeper123", "Кладовщик смены", "storekeeper@warehouse.local", UserRole.STOREKEEPER, true));
        result.put("warehouse_admin", upsertUser("warehouse_admin", "admin123", "Ольга Ковалевская", "o.kovalevskaya@warehouse.local", UserRole.ADMIN, true));
        result.put("warehouse_manager", upsertUser("warehouse_manager", "manager123", "Игорь Мельников", "i.melnikov@warehouse.local", UserRole.MANAGER, true));
        result.put("warehouse_worker_1", upsertUser("warehouse_worker_1", "worker123", "Антон Руденко", "a.rudenko@warehouse.local", UserRole.STOREKEEPER, true));
        result.put("warehouse_worker_2", upsertUser("warehouse_worker_2", "worker123", "Марина Литвин", "m.litvin@warehouse.local", UserRole.STOREKEEPER, true));
        return result;
    }

    private User upsertUser(String username, String password, String fullName, String email, UserRole role, boolean active) {
        User user = userRepository.findByUsername(username).orElseGet(User::new);
        if (user.getId() == null) {
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(fullName);
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(email);
        }
        user.setRole(role);
        user.setIsActive(active);
        return userRepository.save(user);
    }

    private Map<String, Warehouse> seedWarehouses() {
        Map<String, Warehouse> result = new LinkedHashMap<>();
        result.put("MSK-MAIN", upsertWarehouse("MSK-MAIN", "Главный склад Минск", "г. Минск, ул. Промышленная, 14", true));
        result.put("GRD-REG", upsertWarehouse("GRD-REG", "Региональный склад Гродно", "г. Гродно, пр-т Космонавтов, 88", true));
        result.put("WEB-FULFILL", upsertWarehouse("WEB-FULFILL", "Склад интернет-заказов", "Минский район, логистический парк Щомыслица, корпус 3", true));
        return result;
    }

    private Warehouse upsertWarehouse(String code, String name, String address, boolean active) {
        Warehouse warehouse = warehouseRepository.findByCode(code).orElseGet(Warehouse::new);
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setAddress(address);
        warehouse.setIsActive(active);
        return warehouseRepository.save(warehouse);
    }

    private Map<String, StorageCell> seedStorageCells(Map<String, Warehouse> warehouses) {
        Map<String, StorageCell> result = new LinkedHashMap<>();
        createCells(result, warehouses.get("MSK-MAIN"), List.of("A", "B"), 4, 2, 2, 650, bd("2800"), bd("6200000"), bd("240"), bd("120"), bd("220"), true);
        createCells(result, warehouses.get("GRD-REG"), List.of("A", "B"), 3, 2, 1, 360, bd("1500"), bd("3200000"), bd("200"), bd("100"), bd("160"), true);
        createCells(result, warehouses.get("WEB-FULFILL"), List.of("P", "Q"), 3, 2, 1, 240, bd("900"), bd("1800000"), bd("160"), bd("90"), bd("125"), true);

        StorageCell inactive = upsertCell(warehouses.get("GRD-REG"), "C-99-01-01", "C", "99", "01", "01", 120, bd("500"), bd("900000"), bd("120"), bd("80"), bd("90"), false);
        result.put(key(warehouses.get("GRD-REG"), inactive.getCode()), inactive);
        return result;
    }

    private void createCells(
            Map<String, StorageCell> result,
            Warehouse warehouse,
            List<String> zones,
            int racks,
            int shelves,
            int levels,
            int capacityUnits,
            BigDecimal maxWeightKg,
            BigDecimal maxVolumeCm3,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            boolean active
    ) {
        for (String zone : zones) {
            for (int rack = 1; rack <= racks; rack++) {
                for (int shelf = 1; shelf <= shelves; shelf++) {
                    for (int level = 1; level <= levels; level++) {
                        String rackCode = "%02d".formatted(rack);
                        String shelfCode = "%02d".formatted(shelf);
                        String levelCode = "%02d".formatted(level);
                        String code = zone + "-" + rackCode + "-" + shelfCode + "-" + levelCode;
                        int modifier = zone.charAt(0) + rack + shelf + level;
                        StorageCell cell = upsertCell(
                                warehouse,
                                code,
                                zone,
                                rackCode,
                                shelfCode,
                                levelCode,
                                capacityUnits - (modifier % 4) * 30,
                                maxWeightKg.subtract(bd(String.valueOf((modifier % 5) * 80))),
                                maxVolumeCm3.subtract(bd(String.valueOf((modifier % 6) * 120000))),
                                lengthCm,
                                widthCm,
                                heightCm,
                                active
                        );
                        result.put(key(warehouse, code), cell);
                    }
                }
            }
        }
    }

    private StorageCell upsertCell(
            Warehouse warehouse,
            String code,
            String zone,
            String rack,
            String shelf,
            String level,
            int capacityUnits,
            BigDecimal maxWeightKg,
            BigDecimal maxVolumeCm3,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            boolean active
    ) {
        StorageCell cell = storageCellRepository.findByWarehouse_IdAndCode(warehouse.getId(), code).orElseGet(StorageCell::new);
        cell.setWarehouse(warehouse);
        cell.setCode(code);
        cell.setZone(zone);
        cell.setRack(rack);
        cell.setShelf(shelf);
        cell.setLevel(level);
        cell.setCapacityUnits(capacityUnits);
        cell.setMaxWeightKg(maxWeightKg);
        cell.setMaxVolumeCm3(maxVolumeCm3);
        cell.setLengthCm(lengthCm);
        cell.setWidthCm(widthCm);
        cell.setHeightCm(heightCm);
        cell.setIsActive(active);
        if (cell.getCurrentWeightKg() == null) {
            cell.setCurrentWeightKg(BigDecimal.ZERO);
        }
        if (cell.getCurrentVolumeCm3() == null) {
            cell.setCurrentVolumeCm3(BigDecimal.ZERO);
        }
        return storageCellRepository.save(cell);
    }

    private Map<String, Product> seedProducts(Map<String, Warehouse> warehouses) {
        List<ProductSeed> seeds = List.of(
                p("NB-L14-GEN4", "4811001000011", "Ноутбук Lenovo ThinkPad L14 Gen 4", "Электроника", 12, "1.65", "35", "24", "2.2", true),
                p("MON-DELL-P2422H", "4811001000028", "Монитор Dell P2422H 24\"", "Электроника", 18, "5.20", "54", "18", "42", true),
                p("KB-LOGI-MXKEYS", "4811001000035", "Клавиатура Logitech MX Keys", "Электроника", 20, "0.81", "43", "14", "2.5", true),
                p("MSE-LOGI-MX3S", "4811001000042", "Мышь Logitech MX Master 3S", "Электроника", 25, "0.14", "13", "9", "5", true),
                p("SSD-SAMS-980-1TB", "4811001000059", "SSD Samsung 980 1TB NVMe", "Комплектующие", 30, "0.08", "8", "2.2", "0.3", true),
                p("RAM-KING-32-DDR4", "4811001000066", "Модуль памяти Kingston 32GB DDR4", "Комплектующие", 28, "0.04", "13", "3", "0.4", true),
                p("CPU-INT-I5-13400", "4811001000073", "Процессор Intel Core i5-13400", "Комплектующие", 10, "0.09", "4.5", "4.5", "0.8", true),
                p("MB-ASUS-B760M", "4811001000080", "Материнская плата ASUS Prime B760M-A", "Комплектующие", 8, "0.75", "24", "24", "5", true),
                p("PSU-CHIEF-650W", "4811001000097", "Блок питания Chieftec 650W", "Комплектующие", 10, "1.90", "16", "15", "9", true),
                p("CASE-FD-CORE1000", "4811001000103", "Корпус Fractal Design Core 1000", "Комплектующие", 4, "4.10", "42", "20", "38", true),
                p("CBL-UTP-CAT6-305", "4811001000110", "Кабель UTP Cat.6 бухта 305 м", "Кабельная продукция", 15, "12.50", "36", "36", "24", true),
                p("CBL-HDMI-2M", "4811001000127", "Кабель HDMI 2.0 2 м", "Кабельная продукция", 35, "0.18", "18", "14", "3", true),
                p("CBL-DP-18M", "4811001000134", "Кабель DisplayPort 1.8 м", "Кабельная продукция", 30, "0.20", "18", "14", "3", true),
                p("CBL-PATCH-C6-1M", "4811001000141", "Патч-корд Cat.6 1 м серый", "Кабельная продукция", 80, "0.05", "12", "12", "2", true),
                p("CBL-PATCH-C6-3M", "4811001000158", "Патч-корд Cat.6 3 м синий", "Кабельная продукция", 70, "0.10", "14", "14", "3", true),
                p("TOOL-SCREW-BOSCH", "4811001000165", "Шуруповерт Bosch GSR 120-LI", "Инструменты", 6, "1.20", "28", "23", "9", true),
                p("TOOL-MULTI-FLUKE", "4811001000172", "Мультиметр Fluke 117", "Инструменты", 4, "0.55", "18", "9", "5", true),
                p("TOOL-CRIMP-KNIPEX", "4811001000189", "Кримпер Knipex для RJ45", "Инструменты", 5, "0.48", "20", "8", "3", true),
                p("TOOL-LABEL-BROTHER", "4811001000196", "Принтер этикеток Brother PT-E550W", "Инструменты", 3, "1.05", "25", "20", "9", true),
                p("OFF-PAPER-A4", "4811001000202", "Бумага офисная A4 80 г/м2", "Офис", 60, "2.50", "30", "21", "5", true),
                p("OFF-TONER-HP85A", "4811001000219", "Картридж HP 85A совместимый", "Офис", 12, "0.85", "36", "11", "11", true),
                p("OFF-BINDER-75", "4811001000226", "Папка-регистратор 75 мм", "Офис", 25, "0.42", "32", "29", "8", true),
                p("OFF-LABEL-100X150", "4811001000233", "Термоэтикетка 100x150, рулон", "Офис", 40, "0.65", "11", "11", "10", true),
                p("CONS-STRETCH-500", "4811001000240", "Стрейч-пленка 500 мм 17 мкм", "Расходники", 45, "2.20", "50", "10", "10", true),
                p("CONS-TAPE-48", "4811001000257", "Скотч упаковочный 48 мм прозрачный", "Расходники", 90, "0.18", "10", "10", "5", true),
                p("CONS-PALLET-EUR", "4811001000264", "Паллет EUR 1200x800", "Расходники", 20, "22.00", "120", "80", "14", true),
                p("CONS-GLOVES-NITRILE", "4811001000271", "Перчатки нитриловые размер L", "Расходники", 70, "0.35", "23", "12", "7", true),
                p("NET-SW-TP-SG108", "4811001000288", "Коммутатор TP-Link SG108 8 портов", "Сетевое оборудование", 16, "0.44", "16", "10", "3", true),
                p("NET-SW-CISCO-2960", "4811001000295", "Коммутатор Cisco Catalyst 2960X", "Сетевое оборудование", 3, "4.60", "45", "28", "5", true),
                p("NET-RTR-MIK-4011", "4811001000301", "Маршрутизатор MikroTik RB4011", "Сетевое оборудование", 8, "0.95", "23", "16", "4.5", true),
                p("NET-AP-UBI-U6PRO", "4811001000318", "Точка доступа Ubiquiti UniFi U6 Pro", "Сетевое оборудование", 12, "0.68", "20", "20", "5", true),
                p("NET-SFP-1G-SM", "4811001000325", "SFP модуль 1G SM 20 км", "Сетевое оборудование", 20, "0.04", "6", "2", "1", true),
                p("NET-PATCHP-24", "4811001000332", "Патч-панель 24 порта Cat.6", "Сетевое оборудование", 8, "1.40", "48", "11", "5", true),
                p("SRV-DELL-R450", "4811001000349", "Сервер Dell PowerEdge R450", "Серверное оборудование", 2, "18.00", "70", "48", "9", true),
                p("SRV-HP-DL360-G10", "4811001000356", "Сервер HPE ProLiant DL360 Gen10", "Серверное оборудование", 2, "16.50", "70", "45", "9", true),
                p("SRV-RAIL-DELL", "4811001000363", "Рельсы монтажные Dell ReadyRails", "Серверное оборудование", 4, "3.20", "78", "8", "6", true),
                p("SRV-HDD-4TB-SAS", "4811001000370", "HDD 4TB SAS 12G 7.2K", "Серверное оборудование", 6, "0.72", "14", "10", "3", true),
                p("SRV-UPS-APC-1500", "4811001000387", "ИБП APC Smart-UPS 1500VA", "Серверное оборудование", 3, "24.00", "43", "22", "18", true),
                p("SRV-PDU-8C13", "4811001000394", "PDU 8xC13 горизонтальный", "Серверное оборудование", 5, "1.10", "48", "6", "5", true),
                p("EL-SCANNER-ZEBRA", "4811001000400", "Сканер штрихкода Zebra DS2208", "Электроника", 10, "0.32", "17", "7", "10", true),
                p("EL-PRINTER-ZD421", "4811001000417", "Принтер этикеток Zebra ZD421", "Электроника", 6, "2.25", "22", "18", "15", true),
                p("EL-TSD-HONEYWELL", "4811001000424", "ТСД Honeywell EDA52", "Электроника", 7, "0.29", "16", "8", "2", true),
                p("CONS-RIBBON-WAX", "4811001000431", "Риббон WAX 110x74", "Расходники", 45, "0.31", "12", "12", "7", true),
                p("OFF-MARKER-PERM", "4811001000448", "Маркер перманентный черный", "Офис", 30, "0.03", "14", "2", "2", true),
                p("CBL-POWER-C13", "4811001000455", "Кабель питания C13 1.8 м", "Кабельная продукция", 50, "0.22", "20", "15", "4", true),
                p("NET-CABINET-18U", "4811001000462", "Шкаф телекоммуникационный 18U", "Сетевое оборудование", 1, "42.00", "80", "60", "100", true),
                p("TOOL-LADDER-3S", "4811001000479", "Стремянка алюминиевая 3 ступени", "Инструменты", 2, "4.80", "120", "45", "12", true),
                p("SRV-NAS-SYN-923", "4811001000486", "NAS Synology DS923+", "Серверное оборудование", 3, "2.24", "22", "20", "17", true),
                p("NET-FW-FORTI-60F", "4811001000493", "Межсетевой экран Fortinet 60F", "Сетевое оборудование", 4, "1.30", "22", "16", "4", true),
                p("OFF-SAFE-ARCHIVE", "4811001000509", "Короб архивный 100 мм", "Офис", 25, "0.24", "33", "25", "10", true),
                p("EL-WEBCAM-C920", "4811001000516", "Веб-камера Logitech C920", "Электроника", 10, "0.21", "10", "8", "4", true),
                p("CONS-ZIPBAG-A5", "4811001000523", "Zip-пакет A5 100 шт.", "Расходники", 40, "0.22", "24", "17", "4", true)
        );

        Map<String, Product> result = new LinkedHashMap<>();
        for (ProductSeed seed : seeds) {
            Product product = productRepository.findBySku(seed.sku()).orElseGet(Product::new);
            product.setSku(seed.sku());
            product.setBarcode(seed.barcode());
            product.setName(seed.name());
            product.setCategory(ProductCategory.fromLabel(seed.category()));
            product.setWeightPerUnitKg(seed.weightKg());
            product.setLengthCm(seed.lengthCm());
            product.setWidthCm(seed.widthCm());
            product.setHeightCm(seed.heightCm());
            product.setVolumePerUnitCm3(seed.lengthCm().multiply(seed.widthCm()).multiply(seed.heightCm()).setScale(3, RoundingMode.HALF_UP));
            product.setIsActive(seed.active());
            Product saved = productRepository.save(product);
            result.put(seed.sku(), saved);
            warehouses.values().forEach(warehouse -> upsertProductMinimum(saved, warehouse, seed.minStockLevel()));
        }
        return result;
    }

    private void upsertProductMinimum(Product product, Warehouse warehouse, int minStockLevel) {
        ProductWarehouseMinStock minimum = productWarehouseMinStockRepository
                .findByProduct_IdAndWarehouse_Id(product.getId(), warehouse.getId())
                .orElseGet(() -> {
                    ProductWarehouseMinStock created = new ProductWarehouseMinStock();
                    created.setProduct(product);
                    created.setWarehouse(warehouse);
                    return created;
                });
        minimum.setMinStockLevel(minStockLevel);
        productWarehouseMinStockRepository.save(minimum);
    }

    private Map<String, Counterparty> seedCounterparties() {
        List<CounterpartySeed> seeds = List.of(
                c("SUP-BELSOFT", "ООО \"БелСофтКомплект\"", CounterpartyType.SUPPLIER, "191234567", "4810001110012", "sales@belsoft.by", "+375 17 301-44-10", "г. Минск, ул. Сурганова, 24", "Екатерина Орлова, отдел поставок"),
                c("SUP-ITLINE", "ООО \"АйТи Лайн\"", CounterpartyType.SUPPLIER, "192345678", "4810001110029", "logistics@itline.by", "+375 17 388-21-55", "г. Минск, ул. Тимирязева, 67", "Дмитрий Савицкий"),
                c("SUP-ELECTROSET", "ЗАО \"ЭлектроСетьПоставка\"", CounterpartyType.SUPPLIER, "193456789", "4810001110036", "edi@electroset.by", "+375 17 290-18-40", "г. Минск, ул. Радиальная, 11", "EDI канал DESADV"),
                c("SUP-GRODNOTECH", "ООО \"ГродноТехИмпорт\"", CounterpartyType.SUPPLIER, "590123456", "4810001110043", "orders@grodnotech.by", "+375 152 62-18-90", "г. Гродно, ул. Домбровского, 55", "Алексей Богдан"),
                c("SUP-CABLEPRO", "ООО \"КабельПрофи\"", CounterpartyType.SUPPLIER, "191987654", "4810001110050", "supply@cablepro.by", "+375 17 256-77-12", "г. Минск, ул. Ваупшасова, 10", "Мария Бельская"),
                c("SUP-OFFICEPLUS", "ЧУП \"ОфисПлюс\"", CounterpartyType.SUPPLIER, "192876543", "4810001110067", "zakaz@officeplus.by", "+375 17 399-50-30", "г. Минск, ул. Кальварийская, 33", "Поставки офисных расходников"),
                c("SUP-SERVERROOM", "ООО \"Серверная Инфраструктура\"", CounterpartyType.SUPPLIER, "193765432", "4810001110074", "partners@serverroom.by", "+375 17 222-64-80", "г. Минск, ул. Купревича, 1/5", "Складской резерв 48 часов"),
                c("SUP-LOGITOOLS", "ООО \"ЛогиТулс\"", CounterpartyType.SUPPLIER, "191456789", "4810001110081", "info@logitools.by", "+375 17 270-45-66", "г. Минск, ул. Машиностроителей, 29", "Инструменты и маркировка"),
                c("SUP-PRINTMARK", "ООО \"ПринтМарк Сервис\"", CounterpartyType.SUPPLIER, "192567890", "4810001110098", "edi@printmark.by", "+375 17 375-12-12", "г. Минск, ул. Лещинского, 8", "Расходники для печати"),
                c("SUP-NETPRO", "ООО \"НетПро Дистрибуция\"", CounterpartyType.SUPPLIER, "193678901", "4810001110104", "b2b@netpro.by", "+375 17 336-08-91", "г. Минск, ул. Логойский тракт, 22", "Сетевое оборудование"),
                c("SUP-TECHRESERVE", "ООО \"ТехРезерв\"", CounterpartyType.SUPPLIER, "191789012", "4810001110111", "warehouse@techreserve.by", "+375 17 240-33-17", "г. Минск, ул. Монтажников, 6", "Резервные поставки"),
                c("SUP-EUROLOGIC", "ООО \"ЕвроЛогик\"", CounterpartyType.SUPPLIER, "192890123", "4810001110128", "orders@eurologic.by", "+375 17 305-90-10", "г. Минск, ул. Селицкого, 21", "Импортные комплектующие"),
                c("CUS-ALFARETAIL", "ООО \"Альфа Ритейл\"", CounterpartyType.CUSTOMER, "191111222", "4810002220017", "orders@alfaretail.by", "+375 17 211-80-20", "г. Минск, пр-т Победителей, 100", "Регулярные интернет-заказы"),
                c("CUS-MEDCENTER", "УП \"МедЦентр Сервис\"", CounterpartyType.CUSTOMER, "191222333", "4810002220024", "it@medcenter.by", "+375 17 245-65-41", "г. Минск, ул. Клары Цеткин, 5", "ИТ-отдел, Сергей Лапко"),
                c("CUS-BANKWEST", "ЗАО \"Банк Западный\"", CounterpartyType.CUSTOMER, "191333444", "4810002220031", "procurement@bankwest.by", "+375 17 299-11-90", "г. Минск, ул. Немига, 40", "Закупки по квартальному плану"),
                c("CUS-GRODNO-MALL", "ООО \"Гродно Молл\"", CounterpartyType.CUSTOMER, "590222333", "4810002220048", "admin@grodnomall.by", "+375 152 60-33-44", "г. Гродно, ул. Тавлая, 82", "Сервисная зона ТРЦ"),
                c("CUS-EDUTECH", "ГУО \"Центр образовательных технологий\"", CounterpartyType.CUSTOMER, "191444555", "4810002220055", "supply@edutech.by", "+375 17 260-71-30", "г. Минск, ул. Захарова, 59", "Поставки для классов"),
                c("CUS-LOGISTIC24", "ООО \"Логистик24\"", CounterpartyType.CUSTOMER, "191555666", "4810002220062", "support@logistic24.by", "+375 17 288-44-02", "Минский район, д. Колядичи, 15", "Интеграция по EDI ORDERS"),
                c("CUS-RETAILHUB", "ООО \"Ритейл Хаб\"", CounterpartyType.CUSTOMER, "191666777", "4810002220079", "edi@retailhub.by", "+375 17 365-15-70", "г. Минск, ул. Маяковского, 127", "EDI ORDERS/ORDRSP"),
                c("CUS-REGIONPLUS", "ООО \"РегионПлюс\"", CounterpartyType.CUSTOMER, "590333444", "4810002220086", "office@regionplus.by", "+375 152 68-40-15", "г. Гродно, ул. Курчатова, 18", "Отгрузки со склада Гродно"),
                c("CUS-CLOUDPARK", "ООО \"КлаудПарк\"", CounterpartyType.CUSTOMER, "191777888", "4810002220093", "dc@cloudpark.by", "+375 17 310-40-90", "г. Минск, ул. Академика Купревича, 3", "Серверное оборудование"),
                c("CUS-PRINTLINE", "ООО \"ПринтЛайн\"", CounterpartyType.CUSTOMER, "191888999", "4810002220109", "orders@printline.by", "+375 17 390-80-66", "г. Минск, ул. Передовая, 6", "Расходники и маркировка"),
                c("CUS-TECHUNION", "ООО \"ТехЮнион\"", CounterpartyType.CUSTOMER, "191999000", "4810002220116", "purchasing@techunion.by", "+375 17 224-18-19", "г. Минск, ул. Платонова, 20Б", "Проектные закупки"),
                c("CUS-EASYSHOP", "ООО \"ИзиШоп\"", CounterpartyType.CUSTOMER, "192000111", "4810002220123", "market@easyshop.by", "+375 29 640-20-40", "г. Минск, ул. Богдановича, 155", "Маркетплейс, ежедневная отгрузка")
        );

        Map<String, Counterparty> result = new LinkedHashMap<>();
        for (CounterpartySeed seed : seeds) {
            Counterparty counterparty = counterpartyRepository.findByCode(seed.code()).orElseGet(Counterparty::new);
            counterparty.setCode(seed.code());
            counterparty.setName(seed.name());
            counterparty.setType(seed.type());
            counterparty.setTaxId(seed.taxId());
            counterparty.setGln(seed.gln());
            counterparty.setEmail(seed.email());
            counterparty.setPhone(seed.phone());
            counterparty.setAddress(seed.address());
            counterparty.setContactInfo(seed.contactInfo());
            counterparty.setIsActive(true);
            result.put(seed.code(), counterpartyRepository.save(counterparty));
        }
        return result;
    }

    private void seedBalances(Map<String, Product> products, Map<String, StorageCell> cells) {
        List<Product> productList = new ArrayList<>(products.values());
        List<StorageCell> activeCells = cells.values().stream()
                .filter(cell -> Boolean.TRUE.equals(cell.getIsActive()))
                .sorted(Comparator.comparing(cell -> cell.getWarehouse().getCode() + cell.getCode()))
                .toList();
        Map<Long, CellLoad> loads = new LinkedHashMap<>();
        activeCells.forEach(cell -> loads.put(cell.getId(), new CellLoad(cell)));
        Set<Long> demoProductIds = productList.stream().map(Product::getId).collect(java.util.stream.Collectors.toSet());

        stockBalanceRepository.findAll().stream()
                .filter(balance -> loads.containsKey(balance.getCell().getId()))
                .filter(balance -> !demoProductIds.contains(balance.getProduct().getId()))
                .forEach(balance -> loads.get(balance.getCell().getId()).addExisting(balance));

        for (int i = 0; i < productList.size(); i++) {
            Product product = productList.get(i);
            if (i == 9 || i == 33 || i == 45 || i == 47) {
                continue;
            }
            int desired = desiredStock(product, i);
            int splits = i % 5 == 0 ? 3 : i % 3 == 0 ? 2 : 1;
            for (int part = 0; part < splits; part++) {
                int quantity = desired / splits + (part == 0 ? desired % splits : 0);
                if (quantity <= 0) {
                    continue;
                }
                StorageCell cell = findCellFor(product, quantity, activeCells, loads, i + part * 11);
                if (cell == null) {
                    continue;
                }
                upsertBalance(product, cell, quantity, i % 7 == 0 ? Math.max(0, quantity / 6) : 0);
                loads.get(cell.getId()).add(product, quantity);
            }
        }

        recalculateCellUtilization(cells.values().stream().toList());
    }

    private int desiredStock(Product product, int index) {
        if (index == 4 || index == 6 || index == 28 || index == 34 || index == 37 || index == 48) {
            return 1;
        }
        if (List.of(ProductCategory.CABLES, ProductCategory.CONSUMABLES, ProductCategory.OFFICE).contains(product.getCategory())) {
            return 80 + (index % 7) * 35;
        }
        if (product.getCategory() == ProductCategory.SERVER) {
            return 3 + (index % 4) * 2;
        }
        if (index < 12 || index == 39 || index == 40 || index == 41) {
            return 55 + (index % 5) * 25;
        }
        return 12 + (index % 6) * 9;
    }

    private StorageCell findCellFor(Product product, int quantity, List<StorageCell> cells, Map<Long, CellLoad> loads, int offset) {
        for (int i = 0; i < cells.size(); i++) {
            StorageCell cell = cells.get((i + offset) % cells.size());
            CellLoad load = loads.get(cell.getId());
            if (load.canAccept(product, quantity)) {
                return cell;
            }
        }
        return null;
    }

    private void upsertBalance(Product product, StorageCell cell, int quantity, int reservedQuantity) {
        StockBalance balance = stockBalanceRepository.findByProduct_IdAndCell_Id(product.getId(), cell.getId())
                .orElseGet(() -> new StockBalance(product, cell, 0));
        int normalizedReserved = Math.min(reservedQuantity, quantity);
        boolean isNew = balance.getUpdatedAt() == null;
        boolean changed = isNew
                || !Objects.equals(balance.getQuantity(), quantity)
                || !Objects.equals(balance.getReservedQuantity(), normalizedReserved);
        if (!changed) {
            return;
        }
        balance.setProduct(product);
        balance.setCell(cell);
        balance.setQuantity(quantity);
        balance.setReservedQuantity(normalizedReserved);
        stockBalanceRepository.save(balance);
    }

    private void recalculateCellUtilization(List<StorageCell> cells) {
        for (StorageCell cell : cells) {
            BigDecimal weight = BigDecimal.ZERO;
            BigDecimal volume = BigDecimal.ZERO;
            for (StockBalance balance : stockBalanceRepository.findByCell_Id(cell.getId())) {
                weight = weight.add(balance.getProduct().getWeightPerUnitKg().multiply(BigDecimal.valueOf(balance.getQuantity())));
                volume = volume.add(balance.getProduct().getVolumePerUnitCm3().multiply(BigDecimal.valueOf(balance.getQuantity())));
            }
            BigDecimal currentWeight = weight.min(cell.getMaxWeightKg()).setScale(3, RoundingMode.HALF_UP);
            BigDecimal currentVolume = volume.min(cell.getMaxVolumeCm3()).setScale(3, RoundingMode.HALF_UP);
            if (cell.getCurrentWeightKg().compareTo(currentWeight) != 0 || cell.getCurrentVolumeCm3().compareTo(currentVolume) != 0) {
                cell.setCurrentWeightKg(currentWeight);
                cell.setCurrentVolumeCm3(currentVolume);
                storageCellRepository.save(cell);
            }
        }
    }

    private void seedOperations(
            Map<String, User> users,
            Map<String, Warehouse> warehouses,
            Map<String, StorageCell> cells,
            Map<String, Product> products,
            Map<String, Counterparty> counterparties
    ) {
        if (operationRepository.existsByOperationNumber("DEMO-OUT-0001")) {
            normalizeExistingDemoOperations();
            return;
        }

        List<Product> productList = new ArrayList<>(products.values());
        List<StorageCell> cellList = cells.values().stream().filter(cell -> Boolean.TRUE.equals(cell.getIsActive())).toList();
        List<Counterparty> suppliers = counterparties.values().stream().filter(c -> c.getType() == CounterpartyType.SUPPLIER).toList();
        List<Counterparty> customers = counterparties.values().stream().filter(c -> c.getType() == CounterpartyType.CUSTOMER).toList();
        List<User> operators = List.of(users.get("warehouse_manager"), users.get("warehouse_worker_1"), users.get("warehouse_worker_2"), users.get("manager"));
        LocalDateTime base = LocalDateTime.now().minusDays(125);

        for (int i = 1; i <= 180; i++) {
            OperationType type = i % 10 == 0 ? OperationType.MOVE : i % 3 == 0 ? OperationType.INCOME : OperationType.OUTCOME;
            OperationStatus status = i % 17 == 0 ? OperationStatus.CANCELLED : i % 13 == 0 ? OperationStatus.DRAFT : OperationStatus.COMPLETED;
            Operation operation = new Operation();
            operation.setType(type);
            operation.setStatus(status);
            operation.setSource(type != OperationType.MOVE && i % 4 == 0 ? OperationSource.EDI : OperationSource.MANUAL);
            operation.setOperationNumber("DEMO-" + switch (type) {
                case INCOME -> "IN";
                case OUTCOME -> "OUT";
                case MOVE -> "MOV";
            } + "-%04d".formatted(i));
            Warehouse warehouse = List.of(warehouses.get("MSK-MAIN"), warehouses.get("GRD-REG"), warehouses.get("WEB-FULFILL")).get(i % 3);
            operation.setWarehouse(warehouse);
            operation.setCreatedBy(operators.get(i % operators.size()));
            operation.setCompletedBy(status == OperationStatus.COMPLETED ? operators.get((i + 1) % operators.size()) : null);
            operation.setCounterparty(type == OperationType.MOVE ? null : type == OperationType.INCOME ? suppliers.get(i % suppliers.size()) : customers.get(i % customers.size()));
            operation.setExternalDocumentNumber((operation.getSource() == OperationSource.EDI ? "EDI-" : "DOC-") + LocalDate.now().minusDays(125 - (i % 120)) + "-%04d".formatted(i));
            operation.setDocumentDate(LocalDate.now().minusDays(125 - (i % 120)));
            operation.setComment(commentFor(type, status, i));
            operation.setCreatedAt(base.plusDays(i % 120).plusHours((i * 3L) % 24));
            operation.setCompletedAt(status == OperationStatus.COMPLETED ? operation.getCreatedAt().plusHours(2 + i % 8) : null);

            int itemCount = 1 + i % 3;
            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                Product product = productList.get(Math.floorMod(i * 7 + itemIndex * 13, productList.size()));
                OperationItem item = new OperationItem();
                item.setOperation(operation);
                item.setProduct(product);
                item.setQuantity(quantityForOperation(product, i, itemIndex, type));
                item.setUnitPrice(priceFor(product, i));

                List<StorageCell> warehouseCells = cellList.stream()
                        .filter(cell -> Objects.equals(cell.getWarehouse().getId(), warehouse.getId()))
                        .toList();
                StorageCell from = warehouseCells.get(Math.floorMod(i + itemIndex, warehouseCells.size()));
                StorageCell to = warehouseCells.get(Math.floorMod(i + itemIndex + 5, warehouseCells.size()));
                if (type == OperationType.INCOME) {
                    item.setToCell(to);
                } else if (type == OperationType.OUTCOME) {
                    item.setFromCell(from);
                } else {
                    item.setFromCell(from);
                    item.setToCell(to.getId().equals(from.getId()) ? warehouseCells.get((warehouseCells.indexOf(to) + 1) % warehouseCells.size()) : to);
                }
                operation.getItems().add(item);
            }
            operationRepository.save(operation);
        }
    }

    private void normalizeExistingDemoOperations() {
        operationRepository.findAll().stream()
                .filter(operation -> operation.getOperationNumber() != null && operation.getOperationNumber().startsWith("DEMO-MOV-"))
                .filter(operation -> operation.getSource() == OperationSource.EDI)
                .forEach(operation -> {
                    operation.setSource(OperationSource.MANUAL);
                    operationRepository.save(operation);
                });
        operationRepository.findAll().stream()
                .filter(operation -> operation.getOperationNumber() != null && operation.getOperationNumber().startsWith("DEMO-"))
                .forEach(operation -> {
                    if (operation.getSource() == OperationSource.EDI && operation.getExternalDocumentNumber() != null
                            && operation.getExternalDocumentNumber().startsWith("EDI-")) {
                        operation.setExternalDocumentNumber(operation.getOperationNumber());
                    }
                    operationRepository.save(operation);
                });
    }

    private String commentFor(OperationType type, OperationStatus status, int i) {
        if (status == OperationStatus.CANCELLED) {
            return "Отменено: корректировка заявки после сверки с контрагентом";
        }
        if (status == OperationStatus.DRAFT) {
            return "Черновик ожидает подтверждения смены";
        }
        return switch (type) {
            case INCOME -> i % 9 == 0 ? "Сезонное пополнение склада перед пиком заказов" : "Плановая приемка от поставщика";
            case OUTCOME -> i % 11 == 0 ? "Срочная отгрузка по клиентскому заказу" : "Отгрузка клиенту";
            case MOVE -> "Внутреннее перемещение для оптимизации зон хранения";
        };
    }

    private int quantityForOperation(Product product, int i, int itemIndex, OperationType type) {
        int base = switch (product.getCategory()) {
            case CONSUMABLES, CABLES, OFFICE -> 12 + (i + itemIndex) % 35;
            case SERVER -> 1 + (i + itemIndex) % 3;
            default -> 2 + (i + itemIndex) % 12;
        };
        return type == OperationType.MOVE ? Math.max(1, base / 2) : base;
    }

    private BigDecimal priceFor(Product product, int i) {
        BigDecimal categoryBase = switch (product.getCategory()) {
            case SERVER -> bd("4500");
            case NETWORK -> bd("420");
            case COMPONENTS -> bd("180");
            case ELECTRONICS -> bd("760");
            case TOOLS -> bd("95");
            case CABLES -> bd("18");
            case OFFICE -> bd("9");
            default -> bd("6");
        };
        return categoryBase.add(bd(String.valueOf(i % 17)).multiply(bd("3.15"))).setScale(2, RoundingMode.HALF_UP);
    }

    private void seedEdi(Map<String, Warehouse> warehouses, Map<String, Product> products, Map<String, Counterparty> counterparties) {
        List<EdiPartner> partners = List.of(
                upsertEdiPartner("EDI-ELECTROSET", "ЭлектроСетьПоставка EDI", counterparties.get("SUP-ELECTROSET"), warehouses.get("MSK-MAIN"), true, false),
                upsertEdiPartner("EDI-PRINTMARK", "ПринтМарк Сервис EDI", counterparties.get("SUP-PRINTMARK"), warehouses.get("WEB-FULFILL"), true, false),
                upsertEdiPartner("EDI-RETAILHUB", "Ритейл Хаб EDI", counterparties.get("CUS-RETAILHUB"), warehouses.get("WEB-FULFILL"), true, true),
                upsertEdiPartner("EDI-LOGISTIC24", "Логистик24 EDI", counterparties.get("CUS-LOGISTIC24"), warehouses.get("MSK-MAIN"), true, true),
                upsertEdiPartner("EDI-NETPRO", "НетПро Дистрибуция EDI", counterparties.get("SUP-NETPRO"), warehouses.get("GRD-REG"), true, false)
        );

        List<Product> mappedProducts = products.values().stream().limit(22).toList();
        for (EdiPartner partner : partners) {
            List<EdiMessageType> supportedTypes = supportedMappingTypes(partner);
            deactivateUnsupportedMappings(partner, supportedTypes);
            for (int i = 0; i < mappedProducts.size(); i++) {
                Product product = mappedProducts.get(i);
                for (EdiMessageType type : supportedTypes) {
                    upsertDemoMapping(partner, type, product);
                }
            }
        }

        seedGrdEdiChains(warehouses.get("GRD-REG"), products, counterparties);
        for (int i = 1; i <= 30; i++) {
            String messageRef = "DEMO-EDI-MSG-%04d".formatted(i);
            EdiPartner partner = partners.get(i % partners.size());
            boolean supplier = partner.getCounterparty().getType() == CounterpartyType.SUPPLIER;
            EdiMessageType type = i % 5 == 0
                    ? EdiMessageType.ORDRSP
                    : supplier ? EdiMessageType.DESADV : EdiMessageType.ORDERS;
            EdiDirection direction = i % 5 == 0 ? EdiDirection.OUTBOUND : EdiDirection.INBOUND;
            EdiMessageStatus status = switch (i % 8) {
                case 0 -> EdiMessageStatus.FAILED;
                case 1 -> EdiMessageStatus.RECEIVED;
                case 2 -> EdiMessageStatus.NORMALIZED;
                case 3 -> EdiMessageStatus.PROCESSING;
                default -> EdiMessageStatus.PROCESSED;
            };
            Operation relatedOperation = status == EdiMessageStatus.PROCESSED
                    && direction == EdiDirection.INBOUND
                    && type != EdiMessageType.ORDRSP
                    ? findRelatedDemoOperation(type, partner) : null;
            String documentNumber = relatedOperation == null
                    ? "EDI-DOC-%04d".formatted(7000 + i)
                    : relatedOperation.getExternalDocumentNumber();
            EdiMessage message = ediMessageRepository.findByMessageRef(messageRef).orElseGet(EdiMessage::new);
            message.setMessageType(type);
            message.setDirection(direction);
            message.setStatus(status);
            message.setInterchangeRef("UNB-DEMO-%04d".formatted(i));
            message.setMessageRef(messageRef);
            message.setDocumentNumber(documentNumber);
            message.setPartner(partner);
            message.setRelatedOperation(relatedOperation);
            message.setReceivedAt(LocalDateTime.now().minusDays(45 - i).minusMinutes(i * 7L));
            message.setProcessedAt(status == EdiMessageStatus.PROCESSED || status == EdiMessageStatus.FAILED ? message.getReceivedAt().plusMinutes(8 + i) : null);
            message.setErrorMessage(status == EdiMessageStatus.FAILED ? "Не найден активный mapping для внешнего кода товара или некорректный GLN получателя" : null);
            message.setRawPayload(rawPayload(type, partner, i, documentNumber));
            message.setNormalizedPayload(normalizedPayload(type, partner, i, documentNumber, mappedProducts));
            EdiMessage saved = ediMessageRepository.save(message);
            seedQueue(saved, i, status);
            seedEdiAudit(saved, i, status);
        }
    }

    private List<EdiMessageType> supportedMappingTypes(EdiPartner partner) {
        if (partner.getCounterparty().getType() == CounterpartyType.SUPPLIER) {
            return List.of(EdiMessageType.DESADV, EdiMessageType.ORDRSP);
        }
        return List.of(EdiMessageType.ORDERS, EdiMessageType.ORDRSP);
    }

    private void deactivateUnsupportedMappings(EdiPartner partner, List<EdiMessageType> supportedTypes) {
        ediMappingConfigRepository.findAll().stream()
                .filter(mapping -> mapping.getPartner().getId().equals(partner.getId()))
                .filter(mapping -> !supportedTypes.contains(mapping.getMessageType()))
                .filter(mapping -> Boolean.TRUE.equals(mapping.getIsActive()))
                .forEach(mapping -> {
                    mapping.setIsActive(false);
                    ediMappingConfigRepository.save(mapping);
                });
    }

    private void upsertDemoMapping(EdiPartner partner, EdiMessageType type, Product product) {
        List<EdiMappingConfig> existingMappings = ediMappingConfigRepository
                .findAllByPartner_IdAndMessageTypeAndExternalProductCode(
                        partner.getId(),
                        type,
                        partner.getCode() + "-" + product.getSku()
                );
        EdiMappingConfig mapping = existingMappings.isEmpty() ? new EdiMappingConfig() : existingMappings.get(0);
        existingMappings.stream().skip(1).forEach(duplicate -> {
            duplicate.setIsActive(false);
            ediMappingConfigRepository.save(duplicate);
        });
        mapping.setPartner(partner);
        mapping.setMessageType(type);
        mapping.setExternalProductCode(partner.getCode() + "-" + product.getSku());
        mapping.setInternalProduct(product);
        mapping.setIsActive(true);
        ediMappingConfigRepository.save(mapping);
    }

    private Operation findRelatedDemoOperation(EdiMessageType messageType, EdiPartner partner) {
        OperationType expectedType = messageType == EdiMessageType.DESADV ? OperationType.INCOME : OperationType.OUTCOME;
        return operationRepository.findAll().stream()
                .filter(operation -> operation.getSource() == OperationSource.EDI)
                .filter(operation -> operation.getType() == expectedType)
                .filter(operation -> operation.getCounterparty() != null)
                .filter(operation -> operation.getCounterparty().getId().equals(partner.getCounterparty().getId()))
                .filter(operation -> operation.getStatus() == OperationStatus.DRAFT || operation.getStatus() == OperationStatus.COMPLETED)
                .filter(operation -> operation.getExternalDocumentNumber() != null)
                .findFirst()
                .orElse(null);
    }

    private EdiPartner upsertEdiPartner(String code, String name, Counterparty counterparty, Warehouse warehouse, boolean inbound, boolean outbound) {
        EdiPartner partner = ediPartnerRepository.findByCode(code).orElseGet(EdiPartner::new);
        partner.setCode(code);
        partner.setName(name);
        partner.setGln(counterparty.getGln());
        partner.setCounterparty(counterparty);
        partner.setDefaultWarehouse(warehouse);
        partner.setInboundEnabled(inbound);
        partner.setOutboundEnabled(outbound);
        partner.setIsActive(true);
        return ediPartnerRepository.save(partner);
    }

    private void seedGrdEdiChains(
            Warehouse warehouse,
            Map<String, Product> products,
            Map<String, Counterparty> counterparties
    ) {
        EdiPartner supplier = upsertEdiPartner("EDI-NETPRO", "NetPro Distribution EDI", counterparties.get("SUP-NETPRO"), warehouse, true, false);
        EdiPartner firstCustomer = upsertEdiPartner("EDI-GRODNO-MALL", "Grodno Mall EDI", counterparties.get("CUS-GRODNO-MALL"), warehouse, true, true);
        EdiPartner secondCustomer = upsertEdiPartner("EDI-REGIONPLUS", "RegionPlus EDI", counterparties.get("CUS-REGIONPLUS"), warehouse, true, true);

        List<Product> productList = products.values().stream().toList();
        List<StockBalance> availableBalances = stockBalanceRepository.findByCell_Warehouse_Id(warehouse.getId()).stream()
                .filter(balance -> balance.getQuantity() - balance.getReservedQuantity() >= 4)
                .sorted(Comparator.comparing(balance -> balance.getProduct().getSku()))
                .toList();
        if (availableBalances.size() < 4 || productList.size() < 4) {
            return;
        }

        List<EdiChainSeed> chains = List.of(
                new EdiChainSeed("DEMO-GRD-DESADV-8101", EdiMessageType.DESADV, supplier, OperationType.INCOME,
                        productList.get(0), productList.get(1), null, null, 8, 5, 1),
                new EdiChainSeed("DEMO-GRD-DESADV-8102", EdiMessageType.DESADV, supplier, OperationType.INCOME,
                        productList.get(2), productList.get(3), null, null, 6, 4, 2),
                new EdiChainSeed("DEMO-GRD-ORDERS-8201", EdiMessageType.ORDERS, firstCustomer, OperationType.OUTCOME,
                        availableBalances.get(0).getProduct(), availableBalances.get(1).getProduct(),
                        availableBalances.get(0), availableBalances.get(1), 4, 4, 3),
                new EdiChainSeed("DEMO-GRD-ORDERS-8202", EdiMessageType.ORDERS, secondCustomer, OperationType.OUTCOME,
                        availableBalances.get(2).getProduct(), availableBalances.get(3).getProduct(),
                        availableBalances.get(2), availableBalances.get(3), 4, 4, 4)
        );

        User createdBy = userRepository.findByUsername("warehouse_manager")
                .orElseGet(() -> userRepository.findByUsername("manager").orElseThrow());
        User completedBy = userRepository.findByUsername("warehouse_worker_1")
                .orElseGet(() -> userRepository.findByUsername("storekeeper").orElseThrow());
        List<StorageCell> activeCells = storageCellRepository.findByWarehouse_Id(warehouse.getId()).stream()
                .filter(cell -> Boolean.TRUE.equals(cell.getIsActive()))
                .sorted(Comparator.comparing(StorageCell::getCode))
                .toList();
        LocalDate baseDate = LocalDate.now().minusDays(10);
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);

        for (EdiChainSeed chain : chains) {
            upsertDemoMapping(chain.partner(), chain.messageType(), chain.firstProduct());
            upsertDemoMapping(chain.partner(), chain.messageType(), chain.secondProduct());
            upsertDemoMapping(chain.partner(), EdiMessageType.ORDRSP, chain.firstProduct());
            upsertDemoMapping(chain.partner(), EdiMessageType.ORDRSP, chain.secondProduct());

            Operation operation = upsertChainOperation(chain, warehouse, createdBy, completedBy, activeCells, baseDate, baseTime);
            EdiMessage message = upsertChainMessage(chain, operation, baseTime);
            seedQueue(message, 80 + chain.offset(), EdiMessageStatus.PROCESSED);
            seedEdiAudit(message, 80 + chain.offset(), EdiMessageStatus.PROCESSED);
            seedExecutionChain(operation, message);
        }
    }

    private Operation upsertChainOperation(
            EdiChainSeed chain,
            Warehouse warehouse,
            User createdBy,
            User completedBy,
            List<StorageCell> activeCells,
            LocalDate baseDate,
            LocalDateTime baseTime
    ) {
        Operation operation = operationRepository.findAll().stream()
                .filter(existing -> chain.documentNumber().equals(existing.getOperationNumber()))
                .findFirst()
                .orElseGet(Operation::new);
        operation.setOperationNumber(chain.documentNumber());
        operation.setType(chain.operationType());
        operation.setStatus(OperationStatus.COMPLETED);
        operation.setSource(OperationSource.EDI);
        operation.setWarehouse(warehouse);
        operation.setCounterparty(chain.partner().getCounterparty());
        operation.setExternalDocumentNumber(chain.documentNumber());
        operation.setDocumentDate(baseDate.plusDays(chain.offset()));
        operation.setCreatedBy(createdBy);
        operation.setCompletedBy(completedBy);
        operation.setComment("Full inbound EDI chain demo for " + chain.messageType());
        operation.setCreatedAt(baseTime.plusDays(chain.offset()));
        operation.setCompletedAt(operation.getCreatedAt().plusHours(3));
        operation.getItems().clear();
        operation.getItems().add(chainItem(operation, chain.firstProduct(), chain.firstQuantity(), chain, activeCells, 0));
        operation.getItems().add(chainItem(operation, chain.secondProduct(), chain.secondQuantity(), chain, activeCells, 1));
        return operationRepository.save(operation);
    }

    private OperationItem chainItem(
            Operation operation,
            Product product,
            int quantity,
            EdiChainSeed chain,
            List<StorageCell> activeCells,
            int itemIndex
    ) {
        OperationItem item = new OperationItem();
        item.setOperation(operation);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(priceFor(product, 90 + chain.offset() + itemIndex));
        if (chain.operationType() == OperationType.INCOME) {
            item.setToCell(activeCells.get(Math.floorMod(chain.offset() + itemIndex, activeCells.size())));
        } else {
            item.setFromCell(itemIndex == 0 ? chain.firstBalance().getCell() : chain.secondBalance().getCell());
        }
        return item;
    }

    private EdiMessage upsertChainMessage(EdiChainSeed chain, Operation operation, LocalDateTime baseTime) {
        String messageRef = chain.documentNumber() + "-MSG";
        EdiMessage message = ediMessageRepository.findByMessageRef(messageRef).orElseGet(EdiMessage::new);
        message.setMessageType(chain.messageType());
        message.setDirection(EdiDirection.INBOUND);
        message.setStatus(EdiMessageStatus.PROCESSED);
        message.setInterchangeRef(chain.documentNumber() + "-UNB");
        message.setMessageRef(messageRef);
        message.setDocumentNumber(chain.documentNumber());
        message.setPartner(chain.partner());
        message.setRelatedOperation(operation);
        message.setReceivedAt(baseTime.plusDays(chain.offset()).minusMinutes(45));
        message.setProcessedAt(baseTime.plusDays(chain.offset()).minusMinutes(20));
        message.setErrorMessage(null);
        message.setRawPayload(chainRawPayload(chain));
        message.setNormalizedPayload(chainNormalizedPayload(operation, chain.partner()));
        return ediMessageRepository.save(message);
    }

    private String chainRawPayload(EdiChainSeed chain) {
        return """
                {"syntax":"EDIFACT-DEMO","messageType":"%s","sender":"%s","documentNumber":"%s","lines":[{"externalProductCode":"%s-%s","quantity":%d},{"externalProductCode":"%s-%s","quantity":%d}]}
                """.formatted(
                chain.messageType(),
                chain.partner().getCode(),
                chain.documentNumber(),
                chain.partner().getCode(),
                chain.firstProduct().getSku(),
                chain.firstQuantity(),
                chain.partner().getCode(),
                chain.secondProduct().getSku(),
                chain.secondQuantity()
        ).trim();
    }

    private String chainNormalizedPayload(Operation operation, EdiPartner partner) {
        OperationItem first = operation.getItems().get(0);
        OperationItem second = operation.getItems().get(1);
        return """
                {"documentNumber":"%s","documentDate":"%s","partnerCode":"%s","warehouseId":%d,"warehouseCode":"%s","items":[%s,%s]}
                """.formatted(
                operation.getExternalDocumentNumber(),
                operation.getDocumentDate(),
                partner.getCode(),
                operation.getWarehouse().getId(),
                operation.getWarehouse().getCode(),
                chainNormalizedItem(first, partner),
                chainNormalizedItem(second, partner)
        ).trim();
    }

    private String chainNormalizedItem(OperationItem item, EdiPartner partner) {
        String cellPayload = item.getOperation().getType() == OperationType.INCOME
                ? ",\"toCellId\":" + item.getToCell().getId()
                : ",\"fromCellId\":" + item.getFromCell().getId();
        return """
                {"externalProductCode":"%s-%s","sku":"%s","quantity":%d,"unitPrice":%s%s}
                """.formatted(
                partner.getCode(),
                item.getProduct().getSku(),
                item.getProduct().getSku(),
                item.getQuantity(),
                item.getUnitPrice().toPlainString(),
                cellPayload
        ).trim();
    }

    private void seedExecutionChain(Operation operation, EdiMessage message) {
        upsertExecutionStep(operation, message, DocumentExecutionStage.EDI_RECEIVED, message.getReceivedAt(), "Inbound EDI document received");
        upsertExecutionStep(operation, message, DocumentExecutionStage.DRAFT_CREATED, operation.getCreatedAt(), "Draft operation created from EDI document");
        upsertExecutionStep(operation, message, DocumentExecutionStage.FACT_CHECK, operation.getCreatedAt().plusHours(1), "Fact check accepted");
        upsertExecutionStep(operation, message, DocumentExecutionStage.STOCK_POSTED, operation.getCompletedAt().minusMinutes(15), "Stock movement posted");
        upsertExecutionStep(operation, message, DocumentExecutionStage.COMPLETED, operation.getCompletedAt(), "EDI operation completed");
    }

    private void upsertExecutionStep(
            Operation operation,
            EdiMessage message,
            DocumentExecutionStage stage,
            LocalDateTime createdAt,
            String details
    ) {
        DocumentExecutionStep step = documentExecutionStepRepository.findByOperation_IdOrderByCreatedAtAsc(operation.getId()).stream()
                .filter(existing -> existing.getStage() == stage)
                .findFirst()
                .orElseGet(DocumentExecutionStep::new);
        step.setOperation(operation);
        step.setEdiMessage(message);
        step.setStage(stage);
        step.setStatus(DocumentExecutionStatus.DONE);
        step.setDetails(details);
        step.setCreatedBy("demo-seed");
        step.setCreatedAt(createdAt);
        documentExecutionStepRepository.save(step);
    }

    private void seedQueue(EdiMessage message, int i, EdiMessageStatus messageStatus) {
        EdiProcessingQueue queue = ediProcessingQueueRepository.findByEdiMessage_Id(message.getId()).orElseGet(EdiProcessingQueue::new);
        queue.setEdiMessage(message);
        queue.setStatus(switch (messageStatus) {
            case RECEIVED, NORMALIZED -> EdiQueueStatus.PENDING;
            case PROCESSING -> EdiQueueStatus.RUNNING;
            case FAILED -> EdiQueueStatus.FAILED;
            case PROCESSED, COMPLETED -> EdiQueueStatus.DONE;
        });
        queue.setAttemptCount(messageStatus == EdiMessageStatus.FAILED ? 3 : messageStatus == EdiMessageStatus.PROCESSING ? 1 : 0);
        queue.setScheduledAt(message.getReceivedAt().plusMinutes(2));
        queue.setStartedAt(messageStatus == EdiMessageStatus.PROCESSING || messageStatus == EdiMessageStatus.PROCESSED || messageStatus == EdiMessageStatus.FAILED ? message.getReceivedAt().plusMinutes(3) : null);
        queue.setFinishedAt(messageStatus == EdiMessageStatus.PROCESSED || messageStatus == EdiMessageStatus.FAILED ? message.getReceivedAt().plusMinutes(12) : null);
        queue.setLastError(messageStatus == EdiMessageStatus.FAILED ? message.getErrorMessage() : null);
        ediProcessingQueueRepository.save(queue);
    }

    private void seedEdiAudit(EdiMessage message, int i, EdiMessageStatus status) {
        List<String> stages = List.of("RECEIVE", "VALIDATE", "NORMALIZE", "MAP_PRODUCTS", "CREATE_OPERATION");
        for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
            EdiAuditLog logEntry = new EdiAuditLog();
            logEntry.setEdiMessage(message);
            logEntry.setStage(stages.get(stageIndex));
            boolean failedStage = status == EdiMessageStatus.FAILED && stageIndex >= 3;
            if (ediAuditLogRepository.existsByEdiMessage_IdAndStage(message.getId(), logEntry.getStage())) {
                if (failedStage) {
                    break;
                }
                continue;
            }
            logEntry.setStatus(failedStage ? EdiAuditStatus.FAILED : stageIndex == 4 && status == EdiMessageStatus.RECEIVED ? EdiAuditStatus.SKIPPED : EdiAuditStatus.SUCCESS);
            logEntry.setDetails(failedStage ? "Ошибка сопоставления внешнего товара с внутренним SKU" : "Демо-аудит этапа обработки EDI сообщения");
            logEntry.setCreatedAt(message.getReceivedAt().plusMinutes(i + stageIndex));
            ediAuditLogRepository.save(logEntry);
            if (failedStage) {
                break;
            }
        }
    }

    private String rawPayload(EdiMessageType type, EdiPartner partner, int i, String documentNumber) {
        return """
                {"syntax":"EDIFACT-DEMO","messageType":"%s","sender":"%s","documentNumber":"%s","lines":[{"externalProductCode":"%s-SKU-%02d","quantity":%d}]}
                """.formatted(type, partner.getCode(), documentNumber, partner.getCode(), i % 20, 3 + i % 14).trim();
    }

    private String normalizedPayload(EdiMessageType type, EdiPartner partner, int i, String documentNumber, List<Product> products) {
        int firstQuantity = 2 + i % 9;
        int secondQuantity = 1 + i % 7;
        Warehouse payloadWarehouse = ediPayloadWarehouse(type, partner, products, Math.max(firstQuantity, secondQuantity));
        Product first = productForEdiLine(type, payloadWarehouse, products, i, firstQuantity);
        Product second = productForEdiLine(type, payloadWarehouse, products, i + 5, secondQuantity);
        String firstCell = ediCellPayload(type, payloadWarehouse, first, firstQuantity);
        String secondCell = ediCellPayload(type, payloadWarehouse, second, secondQuantity);
        return """
                {"documentNumber":"%s","documentDate":"%s","partnerCode":"%s","warehouseId":%d,"warehouseCode":"%s","items":[{"externalProductCode":"%s-%s","sku":"%s","quantity":%d,"unitPrice":%s%s},{"externalProductCode":"%s-%s","sku":"%s","quantity":%d,"unitPrice":%s%s}]}
                """.formatted(
                documentNumber,
                LocalDate.now().minusDays(i % 30),
                partner.getCode(),
                payloadWarehouse.getId(),
                payloadWarehouse.getCode(),
                partner.getCode(),
                first.getSku(),
                first.getSku(),
                firstQuantity,
                priceFor(first, i).toPlainString(),
                firstCell,
                partner.getCode(),
                second.getSku(),
                second.getSku(),
                secondQuantity,
                priceFor(second, i + 3).toPlainString(),
                secondCell
        ).trim();
    }

    private Warehouse ediPayloadWarehouse(EdiMessageType type, EdiPartner partner, List<Product> products, int quantity) {
        if (type != EdiMessageType.ORDERS) {
            return partner.getDefaultWarehouse();
        }
        boolean defaultWarehouseHasStock = products.stream().anyMatch(product -> hasAvailableStock(product, partner.getDefaultWarehouse(), quantity));
        if (defaultWarehouseHasStock) {
            return partner.getDefaultWarehouse();
        }
        return products.stream()
                .flatMap(product -> stockBalanceRepository.findByProduct_Id(product.getId()).stream())
                .filter(balance -> balance.getQuantity() - balance.getReservedQuantity() >= quantity)
                .findFirst()
                .map(balance -> balance.getCell().getWarehouse())
                .orElse(partner.getDefaultWarehouse());
    }

    private Product productForEdiLine(EdiMessageType type, Warehouse warehouse, List<Product> products, int offset, int quantity) {
        if (type != EdiMessageType.ORDERS) {
            return products.get(Math.floorMod(offset, products.size()));
        }
        for (int index = 0; index < products.size(); index++) {
            Product candidate = products.get(Math.floorMod(offset + index, products.size()));
            if (hasAvailableStock(candidate, warehouse, quantity)) {
                return candidate;
            }
        }
        return products.get(Math.floorMod(offset, products.size()));
    }

    private boolean hasAvailableStock(Product product, Warehouse warehouse, int quantity) {
        return stockBalanceRepository.findByProduct_Id(product.getId()).stream()
                .filter(balance -> balance.getCell().getWarehouse().getId().equals(warehouse.getId()))
                .anyMatch(balance -> balance.getQuantity() - balance.getReservedQuantity() >= quantity);
    }

    private String ediCellPayload(EdiMessageType type, Warehouse warehouse, Product product, int quantity) {
        if (type == EdiMessageType.DESADV) {
            Long cellId = storageCellRepository.findByWarehouse_Id(warehouse.getId()).stream()
                    .filter(cell -> Boolean.TRUE.equals(cell.getIsActive()))
                    .findFirst()
                    .map(StorageCell::getId)
                    .orElse(null);
            return cellId == null ? "" : ",\"toCellId\":" + cellId;
        }
        if (type == EdiMessageType.ORDERS) {
            Long cellId = stockBalanceRepository.findByProduct_Id(product.getId()).stream()
                    .filter(balance -> balance.getCell().getWarehouse().getId().equals(warehouse.getId()))
                    .filter(balance -> balance.getQuantity() - balance.getReservedQuantity() >= quantity)
                    .findFirst()
                    .map(balance -> balance.getCell().getId())
                    .orElse(null);
            return cellId == null ? "" : ",\"fromCellId\":" + cellId;
        }
        return "";
    }

    private void seedAuditTrail(Map<String, User> users) {
        if (auditLogRepository.existsByEntityNameAndEntityIdAndAction(DEMO_MARKER_ENTITY, DEMO_MARKER_ID, "SEEDED")) {
            return;
        }
        List<String> usernames = List.of("admin", "manager", "warehouse_manager", "warehouse_worker_1", "warehouse_worker_2");
        for (int i = 1; i <= 70; i++) {
            AuditLog logEntry = new AuditLog();
            logEntry.setEntityName(i % 4 == 0 ? "StockBalance" : i % 5 == 0 ? "EdiMessage" : "Operation");
            logEntry.setEntityId("DEMO-AUD-%04d".formatted(i));
            logEntry.setAction(i % 4 == 0 ? "CHANGED" : i % 5 == 0 ? "PROCESSED" : i % 7 == 0 ? "CANCELLED" : "COMPLETED");
            logEntry.setUsername(usernames.get(i % usernames.size()));
            logEntry.setOccurredAt(LocalDateTime.now().minusDays(60 - i % 55).minusHours(i % 12));
            logEntry.setDetailsJson("{\"source\":\"demo-seed\",\"shift\":\"" + (i % 2 == 0 ? "day" : "evening") + "\",\"operatorActive\":" + users.containsKey(logEntry.getUsername()) + "}");
            auditLogRepository.save(logEntry);
        }
        AuditLog marker = new AuditLog();
        marker.setEntityName(DEMO_MARKER_ENTITY);
        marker.setEntityId(DEMO_MARKER_ID);
        marker.setAction("SEEDED");
        marker.setUsername("system");
        marker.setDetailsJson("{\"version\":\"1\",\"description\":\"Warehouse ERP demo dataset marker\"}");
        auditLogRepository.save(marker);
    }

    private static ProductSeed p(String sku, String barcode, String name, String category, int minStockLevel, String weightKg, String lengthCm, String widthCm, String heightCm, boolean active) {
        return new ProductSeed(sku, barcode, name, category, minStockLevel, bd(weightKg), bd(lengthCm), bd(widthCm), bd(heightCm), active);
    }

    private static CounterpartySeed c(String code, String name, CounterpartyType type, String taxId, String gln, String email, String phone, String address, String contactInfo) {
        return new CounterpartySeed(code, name, type, taxId, gln, email, phone, address, contactInfo);
    }

    private static String key(Warehouse warehouse, String cellCode) {
        return warehouse.getCode() + ":" + cellCode;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record ProductSeed(
            String sku,
            String barcode,
            String name,
            String category,
            int minStockLevel,
            BigDecimal weightKg,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            boolean active
    ) {
    }

    private record CounterpartySeed(
            String code,
            String name,
            CounterpartyType type,
            String taxId,
            String gln,
            String email,
            String phone,
            String address,
            String contactInfo
    ) {
    }

    private record EdiChainSeed(
            String documentNumber,
            EdiMessageType messageType,
            EdiPartner partner,
            OperationType operationType,
            Product firstProduct,
            Product secondProduct,
            StockBalance firstBalance,
            StockBalance secondBalance,
            int firstQuantity,
            int secondQuantity,
            int offset
    ) {
    }

    private static final class CellLoad {
        private final StorageCell cell;
        private int units;
        private BigDecimal weight = BigDecimal.ZERO;
        private BigDecimal volume = BigDecimal.ZERO;

        private CellLoad(StorageCell cell) {
            this.cell = cell;
        }

        private void addExisting(StockBalance balance) {
            add(balance.getProduct(), balance.getQuantity());
        }

        private boolean canAccept(Product product, int quantity) {
            if (product.getLengthCm().compareTo(cell.getLengthCm()) > 0
                    || product.getWidthCm().compareTo(cell.getWidthCm()) > 0
                    || product.getHeightCm().compareTo(cell.getHeightCm()) > 0) {
                return false;
            }
            BigDecimal addWeight = product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(quantity));
            BigDecimal addVolume = product.getVolumePerUnitCm3().multiply(BigDecimal.valueOf(quantity));
            return units + quantity <= cell.getCapacityUnits()
                    && weight.add(addWeight).compareTo(cell.getMaxWeightKg()) <= 0
                    && volume.add(addVolume).compareTo(cell.getMaxVolumeCm3()) <= 0;
        }

        private void add(Product product, int quantity) {
            units += quantity;
            weight = weight.add(product.getWeightPerUnitKg().multiply(BigDecimal.valueOf(quantity)));
            volume = volume.add(product.getVolumePerUnitCm3().multiply(BigDecimal.valueOf(quantity)));
        }
    }
}
