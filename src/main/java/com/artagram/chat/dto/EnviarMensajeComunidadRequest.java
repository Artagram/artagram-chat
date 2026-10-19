package com.artagram.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeComunidadRequest(
        @NotBlank @Size(max = 2000) String contenido
) {
}
