# Warehouse System

Дипломный проект: информационная система для автоматизации складского учета и движения товаров.

Проект включает:

- backend на Spring Boot;
- frontend на React;
- базу данных PostgreSQL;
- Docker Compose-конфигурацию для запуска всего проекта одной командой.

## Требования для запуска

На компьютере должен быть установлен Docker:

- Windows: Docker Desktop;
- macOS: Docker Desktop;
- Linux: Docker Engine и Docker Compose Plugin.

Проверить установку можно командами:

```bash
docker --version
docker compose version
```

Для первого запуска потребуется доступ к интернету, чтобы Docker скачал базовые образы и зависимости проекта.

## Как запустить проект из архива

1. Распакуйте архив с проектом в любую папку.
2. Откройте терминал в корневой папке проекта, где находится файл `docker-compose.yml`.
3. Выполните команду:

```bash
docker compose up --build -d
```

Docker автоматически:

- соберет backend-приложение;
- соберет frontend-приложение;
- запустит PostgreSQL;
- применит миграции базы данных;
- создаст начальных пользователей.

## Как открыть приложение

После успешного запуска откройте в браузере:

```text
http://localhost
```

Backend API также доступен по адресу:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost/swagger-ui.html
```

## Тестовые пользователи

Для входа в систему можно использовать:

| Логин | Пароль | Роль |
| --- | --- | --- |
| `admin` | `admin123` | Администратор |
| `manager` | `manager123` | Менеджер |
| `storekeeper` | `storekeeper123` | Кладовщик |

## Полезные команды

Посмотреть логи приложения:

```bash
docker compose logs -f
```

Остановить приложение:

```bash
docker compose down
```

Запустить повторно:

```bash
docker compose up -d
```

Полностью удалить контейнеры и базу данных:

```bash
docker compose down -v
```

После команды `docker compose down -v` все данные в базе будут удалены. При следующем запуске база создастся заново.

## Если порт занят

По умолчанию приложение использует порты:

- `80` - frontend;
- `8080` - backend;
- `5432` - PostgreSQL.

Если порт `80` уже занят, откройте файл `docker-compose.yml` и замените:

```yaml
ports:
  - "80:80"
```

например на:

```yaml
ports:
  - "8088:80"
```

После этого приложение будет доступно по адресу:

```text
http://localhost:8088
```

Если порт `5432` занят установленным PostgreSQL, можно удалить или закомментировать строку:

```yaml
- "5432:5432"
```

Backend внутри Docker продолжит подключаться к базе данных по внутреннему имени контейнера `postgres`.

## Состав Docker-запуска

В Docker Compose поднимаются три сервиса:

| Сервис | Назначение |
| --- | --- |
| `postgres` | База данных PostgreSQL |
| `backend` | Spring Boot REST API |
| `frontend` | React-приложение на nginx |

Данные PostgreSQL сохраняются в Docker volume `postgres-data`, поэтому они не удаляются при обычной остановке контейнеров.

## Структура проекта

```text
.
├── src/                 # backend Spring Boot
├── frontend/            # frontend React
├── docker-compose.yml   # запуск всего проекта
├── Dockerfile           # сборка backend
└── README.md            # инструкция по запуску
```

## Кратко для проверки комиссией

```bash
docker compose up --build -d
```

Затем открыть:

```text
http://localhost
```

Войти:

```text
admin / admin123
```
