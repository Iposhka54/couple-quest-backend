# Couple Quest Frontend

Пример фронтенда на React + Vite для вашего backend-проекта.

## Что реализовано

- регистрация и вход
- обязательное подтверждение email
- `accessToken` хранится в `localStorage`
- `refresh_token` используется только через `http-only cookie`
- все API-запросы отправляют `Authorization: Bearer <accessToken>`
- при `401` фронтенд пытается обновить access token через `/api/auth/refresh`
- без подтвержденного email доступны только главная и страница подтверждения

## Запуск

```bash
cd frontend
npm install
npm run dev
```

По умолчанию Vite проксирует `/api` на `http://localhost:8080`, то есть на ваш gateway.

## Важно

- для refresh cookie фронтенд использует `credentials: 'include'`
- logout сейчас очищает только клиентское состояние и `localStorage`, так как backend endpoint для удаления refresh cookie пока не добавлен