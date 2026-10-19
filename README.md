# artagram-chat

Microservicio de Chat Personal, Chat de Comunidad y Notificaciones Server-Sent Events (SSE) para ArtaGram.

## Tecnologías
- Java 21, Spring Boot 3.3.4
- Spring Data MongoDB (base de datos `db_chat`)
- Spring AMQP (RabbitMQ: consume eventos de notificaciones `chat.*`)
- Server-Sent Events (SSE) para stream de notificaciones en tiempo real
- WebClient con CircuitBreaker para consultar membresía a `artagram-lienzo`

## Endpoints
- `GET /api/mensajes/conversaciones`
- `GET /api/mensajes/{otroUsuarioId}`
- `POST /api/mensajes`
- `GET /api/comunidades/{comunidadId}/chat`
- `POST /api/comunidades/{comunidadId}/chat`
- `GET /api/notificaciones`
- `GET /api/notificaciones/stream` (SSE)
- `PUT /api/notificaciones/{id}/leida`
- `PUT /api/notificaciones/leer-todas`

## Variables de Entorno
Ver `.env.example`.
