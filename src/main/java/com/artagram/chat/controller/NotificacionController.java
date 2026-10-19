package com.artagram.chat.controller;

import com.artagram.chat.common.JwtUserResolver;
import com.artagram.chat.domain.NotificacionRepository;
import com.artagram.chat.dto.NotificacionDto;
import com.artagram.chat.service.NotificacionSseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Tag(name = "Notificaciones", description = "Centro de notificaciones y stream SSE en vivo")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionSseService sseService;
    private final JwtUserResolver userResolver;

    public NotificacionController(NotificacionRepository notificacionRepository,
                                  NotificacionSseService sseService,
                                  JwtUserResolver userResolver) {
        this.notificacionRepository = notificacionRepository;
        this.sseService = sseService;
        this.userResolver = userResolver;
    }

    @GetMapping("/api/notificaciones")
    public List<NotificacionDto> listar(HttpServletRequest request) {
        var user = userResolver.resolver(request);
        return notificacionRepository.findTop50ByDestinatarioIdOrderByCreadoEnDesc(user.id()).stream()
                .map(n -> new NotificacionDto(
                        n.getId(), n.getDestinatarioId(), n.getTipo(), n.getActorId(),
                        n.getActorNombre(), n.getReferenciaId(), n.getMensaje(),
                        n.isLeida(), n.getCreadoEn()
                ))
                .toList();
    }

    @PostMapping("/api/notificaciones/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable UUID id, HttpServletRequest request) {
        var user = userResolver.resolver(request);
        notificacionRepository.findById(id).ifPresent(n -> {
            if (n.getDestinatarioId().equals(user.id())) {
                n.setLeida(true);
                notificacionRepository.save(n);
            }
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/api/notificaciones/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request) {
        var user = userResolver.resolver(request);
        return sseService.registrar(user.id());
    }
}
