# Warehouse ERP Frontend

React/Vite frontend для локальной складской ERP-системы. Интерфейс работает с реальным Spring Boot backend через REST API и JWT.

## Установка

```bash
cd frontend
npm install
```

## Запуск backend

Из корня репозитория:

```bash
docker compose up --build
```

Backend будет доступен на `http://localhost:8080`. Swagger: `http://localhost:8080/swagger-ui/index.html`.

## Запуск frontend

```bash
cd frontend
npm run dev
```

Frontend: `http://localhost:5173`.

## API и proxy

В `.env.example` задано:

```env
VITE_API_BASE_URL=/api
```

Vite proxy перенаправляет `/api` на `http://localhost:8080`, поэтому CORS-настройки backend не менялись.

## Тестовые пользователи

- `admin / admin123` — ADMIN
- `manager / manager123` — MANAGER
- `storekeeper / storekeeper123` — STOREKEEPER

Роль загружается через `GET /api/auth/me`. Login остался совместимым и возвращает `{ "token": "..." }`.

## Реализованные разделы

- авторизация, хранение JWT, Bearer token, logout, обработка 401/403;
- панель управления с KPI, предупреждениями и графиком заполненности;
- справочники: товары, склады, ячейки хранения, контрагенты;
- операции: список с фильтрами, мастер создания INCOME/OUTCOME/MOVE, просмотр, complete/cancel для ADMIN/MANAGER;
- остатки по складам и ячейкам;
- EDI: сообщения, inbound JSON-прием, очередь, аудит, партнеры, маппинги;
- отчеты, графики, PDF/XLSX экспорт через backend;
- пользователи только для ADMIN;
- role-based UI для ADMIN, MANAGER, STOREKEEPER.

## Проверка

```bash
npm run typecheck
npm run lint
npm run build
```
