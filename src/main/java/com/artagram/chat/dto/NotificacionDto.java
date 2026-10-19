package com.artagram.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificacionDto(
        UUID id,
        UUID destinatarioId,
        String tipo,
        UUID actorId,
        String actorNombre,
        String referenciaId,
        String mensaje,
        boolean leida,
        Instant creadoEn
) {
}
