# Warehouse System

Дипломный проект: информационная система для автоматизации складского учета, движения товаров и работы с EDI-документами.

Проект состоит из Spring Boot backend, React/Vite frontend и PostgreSQL. Backend предоставляет REST API с JWT-авторизацией, применяет миграции Flyway и может автоматически заполнить базу демонстрационными данными.

## Возможности

- Авторизация пользователей и разграничение доступа по ролям `ADMIN`, `MANAGER`, `STOREKEEPER`.
- Панель управления с KPI, очередями работ, предупреждениями по остаткам и заполненности ячеек.
- Справочники товаров, складов, ячеек хранения и контрагентов.
- Складские операции: приход, расход, перемещение, статусы документов, проведение и отмена операций.
- Учет остатков по складам и ячейкам, минимальные остатки по товарам.
- EDI-модуль: сообщения, партнеры, очередь обработки, аудит, симулятор входящих документов.
- Отчеты, графики и экспорт через backend.
- Swagger UI для проверки REST API.

## Технологии

| Часть | Технологии |
| --- | --- |
| Backend | Java 22, Spring Boot, Spring Security, Spring Data JPA, Flyway, Hibernate Envers |
| Frontend | React, TypeScript, Vite, MUI, React Query |
| Database | PostgreSQL |
| Deployment | Docker, Docker Compose, nginx для frontend |

## Быстрое развертывание в Docker

Требования: установленный Docker Desktop или Docker Engine с Docker Compose.

Проверка:

```bash
docker --version
docker compose version
```

Запуск из корня проекта:

```bash
docker compose up --build -d
```

После запуска доступны:

- приложение: `http://localhost`
- backend API: `http://localhost:8080`
- Swagger UI: `http://localhost/swagger-ui.html`

Docker Compose поднимает три сервиса:

| Сервис | Назначение |
| --- | --- |
| `postgres` | база данных PostgreSQL |
| `backend` | Spring Boot REST API |
| `frontend` | собранный React-интерфейс на nginx |

Данные PostgreSQL хранятся в volume `postgres-data`, поэтому не удаляются при обычной остановке контейнеров.

## Тестовые пользователи

При первом запуске создаются базовые учетные записи:

| Логин | Пароль | Роль |
| --- | --- | --- |
| `admin` | `admin123` | Администратор |
| `manager` | `manager123` | Менеджер |
| `storekeeper` | `storekeeper123` | Кладовщик |

## Заполнение тестовыми данными

В проекте есть расширенный демо-набор данных: склады, ячейки, товары, контрагенты, остатки, операции, EDI-партнеры, сообщения, очередь обработки и аудит. Он создается классом `DemoDataInitializer` при активном профиле `dev`.

Чтобы заполнить базу демо-данными в Docker:

1. Откройте `docker-compose.yml`.
2. У сервиса `backend` замените значение:

```yaml
SPRING_PROFILES_ACTIVE: docker
```

на:

```yaml
SPRING_PROFILES_ACTIVE: docker,dev
```

3. Запустите проект:

```bash
docker compose up --build -d
```

Если база уже была создана без демо-данных, можно пересоздать ее с чистого состояния:

```bash
docker compose down -v
docker compose up --build -d
```

Команда `docker compose down -v` удаляет volume с PostgreSQL, поэтому все ранее внесенные данные будут потеряны.

Отключить автоматическое заполнение при активном профиле `dev` можно параметром:

```properties
warehouse.demo-data.enabled=false
```

Например, для локального запуска backend:

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--warehouse.demo-data.enabled=false
```

## Локальный запуск для разработки

Backend можно запустить отдельно, если доступен PostgreSQL с базой `warehouse_db`, пользователем `postgres` и паролем `1234`:

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Vite dev server будет доступен на `http://localhost:5173`, а запросы `/api` проксируются на backend.

## Полезные команды

Посмотреть логи:

```bash
docker compose logs -f
```

Остановить контейнеры без удаления базы:

```bash
docker compose down
```

Запустить повторно:

```bash
docker compose up -d
```

Полностью удалить контейнеры и базу:

```bash
docker compose down -v
```

Проверить backend:

```bash
.\mvnw.cmd test
```

Проверить frontend:

```bash
cd frontend
npm run typecheck
npm run lint
npm run build
```

## Если порт занят

По умолчанию используются порты:

- `80` - frontend;
- `8080` - backend;
- `5432` - PostgreSQL.

Если занят порт `80`, измените публикацию порта у сервиса `frontend` в `docker-compose.yml`, например:

```yaml
ports:
  - "8088:80"
```

После этого приложение будет доступно по адресу `http://localhost:8088`.

Если порт `5432` занят локальным PostgreSQL, можно убрать публикацию:

```yaml
- "5432:5432"
```

Backend внутри Docker продолжит подключаться к базе по внутреннему имени сервиса `postgres`.

## Структура проекта

```text
.
├── src/                 # backend Spring Boot
├── frontend/            # frontend React/Vite
├── docker-compose.yml   # запуск PostgreSQL, backend и frontend
├── Dockerfile           # сборка backend
├── DOCKER.md            # дополнительные заметки по Docker
└── README.md            # краткая документация проекта
```
