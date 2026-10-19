package com.artagram.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MensajeComunidadDto(
        UUID id,
        UUID remitenteId,
        String remitenteNombre,
        String contenido,
        Instant creadoEn
) {
}
