# Запуск Warehouse System в Docker

Этот вариант запуска поднимает все, что нужно приложению:

- `postgres` - база PostgreSQL с постоянным volume;
- `backend` - Spring Boot API;
- `frontend` - собранный React-интерфейс на nginx, который проксирует `/api` в backend.

## Что нужно на новой машине

Установить только Docker Desktop или Docker Engine с Docker Compose.

Проверка:

```bash
docker --version
docker compose version
```

## Первый запуск

В папке проекта выполните:

```bash
docker compose up --build
```

После запуска откройте:

- приложение: http://localhost
- backend API: http://localhost:8080
- Swagger UI: http://localhost/swagger-ui.html

Начальные пользователи:

- `admin` / `admin123`
- `manager` / `manager123`
- `storekeeper` / `storekeeper123`

## Запуск в фоне

```bash
docker compose up --build -d
```

Логи:

```bash
docker compose logs -f
```

Остановка:

```bash
docker compose down
```

## Полный сброс данных

Обычная остановка не удаляет базу: данные лежат в Docker volume `postgres-data`.

Чтобы удалить контейнеры и базу:

```bash
docker compose down -v
```

После этого следующий запуск создаст базу заново и применит миграции Flyway.

## Перенос на другое устройство

1. Скопируйте папку проекта на новое устройство.
2. Установите Docker.
3. В папке проекта выполните `docker compose up --build -d`.
4. Откройте http://localhost.

Если порт `80` уже занят, измените в `docker-compose.yml` строку `80:80`, например на `8088:80`, и открывайте http://localhost:8088.

Если порт `5432` занят локальным PostgreSQL, можно убрать публикацию порта `5432:5432` у сервиса `postgres`: backend внутри Docker все равно подключается к базе по внутреннему имени `postgres`.
