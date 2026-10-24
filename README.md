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

## Diseño y Arquitectura

```mermaid
classDiagram
    class PersonalChatController
    class ComunidadChatController
    class NotificacionController

    class ChatPersonalService {
        +listarConversaciones(usuarioId)
        +historial(usuarioId, otroId)
        +enviar(remitenteId, nombre, req)
    }
    class ComunidadChatService {
        +historial(comunidadId, usuarioId, premium, dias)
        +enviar(comunidadId, remitenteId, nombre, req)
        +guardarDesdeStomp(id, comunidadId, autorId, nombre, contenido, creadoEn)
    }
    class NotificacionSseService {
        -subscriptores Map~UUID,List~SseEmitter~~
        +registrar(usuarioId) SseEmitter
        +emitir(NotificacionDocument)
        +enviarHeartbeat()
    }
    class ChatEventListener {
        +onRoomMessage(payload)
        +onFollowCreated(payload)
        +onLikeCreated(payload)
        +onCommentCreated(payload)
        +onSubscriptionUpdated(payload)
        +onAiCompleted(payload)
        +onUserDeleted(payload)
    }
    class LienzoClient {
        +esMiembroActivo(comunidadId, usuarioId) boolean
    }
    class JwtUserResolver {
        -rsaPublicKey RSAPublicKey
        +resolver(HttpServletRequest) UsuarioAutenticado
        +validarToken(String) Optional~UsuarioAutenticado~
    }
    class ApiException
    class GlobalExceptionHandler

    class MensajePersonalDocument
    class MensajeComunidadDocument
    class NotificacionDocument
    class UsuarioDirectorioDocument
    class MensajePersonalRepository
    class MensajeComunidadRepository
    class NotificacionRepository
    class UsuarioDirectorioRepository

    PersonalChatController --> ChatPersonalService
    PersonalChatController --> JwtUserResolver
    ComunidadChatController --> ComunidadChatService
    ComunidadChatController --> JwtUserResolver
    NotificacionController --> NotificacionSseService
    NotificacionController --> NotificacionRepository

    ChatPersonalService --> MensajePersonalRepository
    ChatPersonalService --> UsuarioDirectorioRepository
    ChatPersonalService --> NotificacionSseService
    ChatPersonalService ..> ApiException

    ComunidadChatService --> MensajeComunidadRepository
    ComunidadChatService --> LienzoClient
    ComunidadChatService ..> ApiException

    ChatEventListener --> ComunidadChatService
    ChatEventListener --> NotificacionSseService
    ChatEventListener --> UsuarioDirectorioRepository

    NotificacionSseService --> NotificacionRepository
    GlobalExceptionHandler ..> ApiException

    MensajePersonalRepository --> MensajePersonalDocument
    MensajeComunidadRepository --> MensajeComunidadDocument
    NotificacionRepository --> NotificacionDocument
    UsuarioDirectorioRepository --> UsuarioDirectorioDocument
```

### Descripción

`artagram-chat` sigue una arquitectura en capas clásica (`controller` → `service` → `domain`/Mongo), con tres flujos funcionales independientes que comparten infraestructura:

- **Chat personal** (`ChatPersonalService`): mensajería 1:1, con un directorio local de usuarios (`UsuarioDirectorioDocument`) para resolver nombres sin llamar a otro servicio.
- **Chat de comunidad** (`ComunidadChatService`): persiste tanto lo escrito por REST como lo recibido por STOMP desde `artagram-lienzo` (`guardarDesdeStomp`, idempotente por `existsById`), y valida membresía contra `LienzoClient`.
- **Notificaciones** (`NotificacionSseService`): un *hub* en memoria de `SseEmitter` por usuario; cualquier flujo (mensajes directos, o los 6 tipos de evento que procesa `ChatEventListener`) empuja notificaciones sin conocer los detalles de SSE.

### Análisis SOLID

- **SRP — violación concreta en `JwtUserResolver`**: la clase mezcla tres responsabilidades que en otros servicios del mismo workspace (p. ej. `artagram-canvas`) están separadas en `JwtService` (carga de llave + validación) y `JwtUserResolver` (extracción desde el request): acá una sola clase carga el PEM desde disco, verifica la firma RS256 y además resuelve el principal desde headers `X-User-*` como atajo cuando el gateway ya validó. Difícil de testear la validación JWT de forma aislada.
- **SRP — tensión en `ChatEventListener`**: siete `@RabbitListener` no relacionados (mensajes de sala, follows, likes, comentarios, suscripción, IA, borrado de usuario) en una sola clase. Cada handler es simple y independiente, pero la clase tiene siete razones distintas para cambiar. Aceptable a esta escala; en una próxima iteración podría dividirse por dominio (p. ej. un listener por tipo de evento, o un `NotificationDispatcher` con *strategy* por `tipo`).
- **DIP — violación en `ComunidadChatService`/`ChatEventListener`**: dependen directamente de `LienzoClient`, una clase concreta atada a `RestTemplate`, no de una interfaz (`MembershipChecker` o similar). Cambiar el transporte (gRPC, llamada local si `community` se migra) obliga a tocar el cliente y, potencialmente, sus consumidores.
- **OCP — bien resuelto en `NotificacionSseService`**: nuevas fuentes de notificación solo necesitan construir un `NotificacionDocument` y llamar a `emitir(...)`; no hay que modificar el servicio SSE para añadir un tipo de notificación nuevo (eso ya pasó 6 veces en `ChatEventListener` sin tocar `NotificacionSseService`).
- **LSP/ISP**: no aplican de forma relevante — no hay jerarquías de herencia de dominio ni interfaces anchas que algún cliente implemente parcialmente.
- **Nota aparte**: el README original afirmaba "WebClient con CircuitBreaker" para `LienzoClient`, pero el código usa `RestTemplate` bloqueante con un `catch` que devuelve `true` (acceso permitido) ante cualquier fallo — no hay disyuntor real, solo *fail-open*. Correcto para desarrollo local, pero es un riesgo de disponibilidad/seguridad en producción (una caída prolongada de la membresía deja el chat de comunidad abierto a cualquiera) que vale la pena resolver antes de producción.

### Extensibilidad

- Nuevo tipo de notificación → agregar un `@RabbitListener` en `ChatEventListener` (o una clase nueva) que llame a `NotificacionSseService.emitir(...)`; cero cambios en el resto del sistema.
- Nuevo canal de chat (p. ej. chat de equipo) → replicar el patrón `Controller` + `Service` + `Document`/`Repository` ya usado por personal/comunidad; `JwtUserResolver` y `GlobalExceptionHandler` se reutilizan sin cambios.
- El directorio local de usuarios (`UsuarioDirectorioRepository`) permite resolver nombres sin acoplarse a `artagram-users`, a costa de datos potencialmente desactualizados — es una proyección, igual que `UsuarioProyeccion` en `artagram-canvas`.

### Preparación para AWS

- **Datastore**: `db_chat` (MongoDB) → **Amazon DocumentDB** (compatible con el driver de Mongo sin cambios de código) o **MongoDB Atlas** si se prefiere multi-cloud. Revisar que el driver soporte el modo de replica set que exponga DocumentDB (algunas operaciones de agregación difieren).
- **Secretos**: la llave pública JWT se carga desde un archivo montado (`app.jwt.public-key-path`) o desde una variable con el PEM inline (`app.jwt.public-key-pem`). En AWS, inyectar el PEM completo como secreto de **Secrets Manager** (o **SSM Parameter Store**, SecureString) hacia esa variable de entorno evita depender de un volumen montado — ya hay un *fallback* a variable de entorno en el propio código, listo para usarse así.
- **SSE y balanceo de carga**: `NotificacionSseService` mantiene el estado de suscriptores **en memoria del proceso**. Detrás de un *Application Load Balancer* con más de una tarea/instancia, un usuario puede conectar su SSE a la tarea A y un evento (p. ej. `like.created`) puede procesarse en la tarea B, que no tiene su `SseEmitter` — la notificación no llega. Opciones: (a) *sticky sessions* a nivel de ALB (atan al usuario a una tarea, reduce elasticidad), o (b) la misma estrategia que ya usa `RealtimeBroadcaster` en `artagram-canvas` (relay vía Redis Pub/Sub entre instancias) adaptada a SSE. Esto es un bloqueante real para escalar horizontalmente este servicio en AWS, no solo un detalle.
- **RabbitMQ**: `chat.*` son colas propias con *dead-letter* — migran directo a **Amazon MQ for RabbitMQ** sin cambiar el código (mismo protocolo AMQP 0.9.1); alternativa nativa de AWS sería SNS/SQS, pero exige reescribir el binding por tipo de evento.
- **Cómputo**: sin estado persistente en disco (más allá del PEM) y con *health checks* de Actuator ya expuestos → apto para **ECS Fargate** o **EKS** detrás del ALB, siempre que se resuelva el punto de SSE multi-instancia arriba.
