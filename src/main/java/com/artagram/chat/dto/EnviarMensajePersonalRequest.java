package com.artagram.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record EnviarMensajePersonalRequest(
        @NotNull UUID destinatarioId,
        @NotBlank @Size(max = 2000) String contenido
) {
}
