package com.artagram.chat.service;

import com.artagram.chat.domain.UsuarioDirectorioRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ChatEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatEventListener.class);

    private final ComunidadChatService comunidadChatService;
    private final ChatPersonalService chatPersonalService;
    private final UsuarioDirectorioRepository directorioRepository;

    public ChatEventListener(ComunidadChatService comunidadChatService,
                             ChatPersonalService chatPersonalService,
                             UsuarioDirectorioRepository directorioRepository) {
        this.comunidadChatService = comunidadChatService;
        this.chatPersonalService = chatPersonalService;
        this.directorioRepository = directorioRepository;
    }

    @RabbitListener(queues = "chat.room.message")
    public void onRoomMessage(Map<String, Object> payload) {
        try {
            UUID id = UUID.fromString(String.valueOf(payload.get("id")));
            UUID comunidadId = UUID.fromString(String.valueOf(payload.get("comunidadId")));
            UUID autorId = UUID.fromString(String.valueOf(payload.get("autorId")));
            String autorNombre = String.valueOf(payload.getOrDefault("autorNombre", "artista"));
            String contenido = String.valueOf(payload.getOrDefault("contenido", ""));
            Instant creadoEn = payload.get("creadoEn") != null
                    ? Instant.parse(String.valueOf(payload.get("creadoEn")))
                    : Instant.now();

            comunidadChatService.guardarDesdeStomp(id, comunidadId, autorId, autorNombre, contenido, creadoEn);
        } catch (Exception e) {
            log.error("Error consumiendo room.message: {}", e.getMessage());
        }
    }

    /**
     * A diferencia de las demas colas de este listener (pre-provisionadas en
     * artagram-infra/rabbitmq/definitions.json), esta se auto-declara: es la
     * unica cola nueva que agrega la migracion del chat personal por STOMP
     * desde artagram-canvas, y declararla inline evita depender de un cambio
     * de infra para que el consumidor arranque.
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "chat.personal.message", durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = "artagram.dlx"),
                            @Argument(name = "x-dead-letter-routing-key", value = "chat.personal.message.dlq")
                    }),
            exchange = @Exchange(value = "artagram.events", type = "topic", durable = "true"),
            key = "personal.message.sent"
    ))
    public void onPersonalMessage(Map<String, Object> payload) {
        try {
            UUID id = UUID.fromString(String.valueOf(payload.get("id")));
            UUID remitenteId = UUID.fromString(String.valueOf(payload.get("remitenteId")));
            String remitenteNombre = String.valueOf(payload.getOrDefault("remitenteNombre", "artista"));
            UUID destinatarioId = UUID.fromString(String.valueOf(payload.get("destinatarioId")));
            String contenido = String.valueOf(payload.getOrDefault("contenido", ""));

            chatPersonalService.guardarDesdeStomp(id, remitenteId, remitenteNombre, destinatarioId, contenido);
        } catch (Exception e) {
            log.error("Error consumiendo personal.message.sent: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "chat.user.deleted")
    public void onUserDeleted(Map<String, Object> payload) {
        try {
            UUID usuarioId = UUID.fromString(String.valueOf(payload.get("usuarioId")));
            directorioRepository.deleteById(usuarioId);
            log.info("Usuario eliminado del directorio de chat: {}", usuarioId);
        } catch (Exception e) {
            log.error("Error consumiendo user.deleted: {}", e.getMessage());
        }
    }
}
