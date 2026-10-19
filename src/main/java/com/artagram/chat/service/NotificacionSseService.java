package com.artagram.chat.service;

import com.artagram.chat.domain.NotificacionDocument;
import com.artagram.chat.domain.NotificacionRepository;
import com.artagram.chat.dto.NotificacionDto;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificacionSseService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionSseService.class);
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutos

    private final Map<UUID, List<SseEmitter>> subscriptores = new ConcurrentHashMap<>();
    private final NotificacionRepository notificacionRepository;

    public NotificacionSseService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public SseEmitter registrar(UUID usuarioId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        subscriptores.computeIfAbsent(usuarioId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remover(usuarioId, emitter));
        emitter.onTimeout(() -> remover(usuarioId, emitter));
        emitter.onError(e -> remover(usuarioId, emitter));

        try {
            emitter.send(SseEmitter.event().name("init").data(Map.of("estado", "CONECTADO")));
        } catch (IOException e) {
            remover(usuarioId, emitter);
        }
        return emitter;
    }

    public void emitir(NotificacionDocument doc) {
        notificacionRepository.save(doc);
        List<SseEmitter> emitters = subscriptores.get(doc.getDestinatarioId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        NotificacionDto dto = new NotificacionDto(
                doc.getId(), doc.getDestinatarioId(), doc.getTipo(), doc.getActorId(),
                doc.getActorNombre(), doc.getReferenciaId(), doc.getMensaje(),
                doc.isLeida(), doc.getCreadoEn()
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notificacion").data(dto));
            } catch (Exception e) {
                remover(doc.getDestinatarioId(), emitter);
            }
        }
    }

    @Scheduled(fixedRate = 25000)
    public void enviarHeartbeat() {
        subscriptores.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (Exception e) {
                    remover(userId, emitter);
                }
            }
        });
    }

    private void remover(UUID usuarioId, SseEmitter emitter) {
        List<SseEmitter> list = subscriptores.get(usuarioId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                subscriptores.remove(usuarioId);
            }
        }
    }
}
