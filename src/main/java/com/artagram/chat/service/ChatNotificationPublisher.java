package com.artagram.chat.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publica hacia artagram-notifications (dueño del centro de notificaciones)
 * el evento de mensaje directo recibido, en vez de emitirlo localmente por un
 * SSE propio de chat. La routing key es nueva y distinta de
 * {@code personal.message.sent} (esa es la de persistencia/tiempo real que
 * consume ChatEventListener desde canvas) para no mezclar la responsabilidad
 * de notificar con la de transmitir el mensaje en vivo.
 */
@Component
public class ChatNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public ChatNotificationPublisher(RabbitTemplate rabbitTemplate,
                                      @Value("${app.rabbitmq.exchange:artagram.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    public void publicarMensajeDirecto(UUID destinatarioId, UUID remitenteId, String remitenteNombre,
                                        UUID mensajeId, String contenido) {
        Map<String, Object> payload = Map.of(
                "destinatarioId", destinatarioId.toString(),
                "actorId", remitenteId.toString(),
                "actorNombre", remitenteNombre,
                "referenciaId", mensajeId.toString(),
                "contenido", contenido,
                "timestamp", Instant.now().toString()
        );
        try {
            rabbitTemplate.convertAndSend(exchange, "personal.message.notification", payload);
        } catch (Exception e) {
            log.warn("No se pudo publicar personal.message.notification: {}", e.getMessage());
        }
    }
}
