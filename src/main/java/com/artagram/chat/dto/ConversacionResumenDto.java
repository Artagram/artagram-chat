package com.artagram.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversacionResumenDto(
        UUID contactoId,
        String contactoNombre,
        String ultimoMensaje,
        Instant ultimoMensajeEn,
        boolean entregado
) {
}
