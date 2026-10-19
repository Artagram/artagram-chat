package com.artagram.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MensajePersonalDto(
        UUID id,
        UUID remitenteId,
        String remitenteNombre,
        UUID destinatarioId,
        String contenido,
        boolean entregado,
        Instant creadoEn
) {
}
