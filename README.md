# Warehouse ERP Demo Data

Локальная база может наполняться демонстрационными данными через `DemoDataInitializer`.
Initializer работает только с профилем `dev` и включен по умолчанию для этого профиля.

## Как включить

Запустите backend с профилем `dev`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

или укажите переменную окружения:

```bash
SPRING_PROFILES_ACTIVE=dev
```

## Как отключить

Для профиля `dev` можно отключить seed:

```properties
warehouse.demo-data.enabled=false
```

или запустить приложение без профиля `dev`.

## Demo users

- `admin` / `admin123` / `ADMIN`
- `manager` / `manager123` / `MANAGER`
- `storekeeper` / `storekeeper123` / `STOREKEEPER`
- `warehouse_admin` / `admin123` / `ADMIN`
- `warehouse_manager` / `manager123` / `MANAGER`
- `warehouse_worker_1` / `worker123` / `STOREKEEPER`
- `warehouse_worker_2` / `worker123` / `STOREKEEPER`

## Что создается

- 3 склада: `MSK-MAIN`, `GRD-REG`, `WEB-FULFILL`.
- 45 ячеек хранения, включая активные, почти заполненные и одну неактивную.
- 52 товара по категориям: электроника, комплектующие, кабели, инструменты, офис, расходники, сетевое и серверное оборудование.
- 24 контрагента: 12 поставщиков и 12 клиентов.
- Реалистичные остатки по ячейкам с пересчетом веса и объема.
- 180 складских операций за последние месяцы: `INCOME`, `OUTCOME`, `MOVE`, статусы `COMPLETED`, `DRAFT`, `CANCELLED`, источники `MANUAL` и `EDI`.
- EDI demo data: 5 партнеров, mapping-и, 30 сообщений, queue entries и audit entries.
- Audit log записи для отчетов и dashboard.

Данные идемпотентны: справочники обновляются по уникальным кодам/SKU, операции и EDI-сообщения создаются по фиксированным `DEMO-*` номерам и не дублируются при повторном запуске.
