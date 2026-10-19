package com.artagram.chat.controller;

import com.artagram.chat.common.JwtUserResolver;
import com.artagram.chat.dto.EnviarMensajeComunidadRequest;
import com.artagram.chat.dto.MensajeComunidadDto;
import com.artagram.chat.service.ComunidadChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Chat de Comunidad", description = "Historial y envío de mensajes en salas de comunidad")
public class ComunidadChatController {

    private final ComunidadChatService comunidadChatService;
    private final JwtUserResolver userResolver;

    public ComunidadChatController(ComunidadChatService comunidadChatService, JwtUserResolver userResolver) {
        this.comunidadChatService = comunidadChatService;
        this.userResolver = userResolver;
    }

    @GetMapping("/api/comunidades/{comunidadId}/chat")
    public List<MensajeComunidadDto> historial(@PathVariable UUID comunidadId,
                                               @RequestParam(required = false) Integer dias,
                                               HttpServletRequest request) {
        var user = userResolver.resolver(request);
        return comunidadChatService.historial(comunidadId, user.id(), user.premium(), dias);
    }

    @PostMapping("/api/comunidades/{comunidadId}/chat")
    public ResponseEntity<MensajeComunidadDto> enviar(@PathVariable UUID comunidadId,
                                                      @Valid @RequestBody EnviarMensajeComunidadRequest req,
                                                      HttpServletRequest request) {
        var user = userResolver.resolver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comunidadChatService.enviar(comunidadId, user.id(), user.nombreArtista(), req));
    }
}
