package com.artagram.chat.controller;

import com.artagram.chat.common.JwtUserResolver;
import com.artagram.chat.dto.ConversacionResumenDto;
import com.artagram.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.chat.dto.MensajePersonalDto;
import com.artagram.chat.service.ChatPersonalService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Chat Personal", description = "Mensajes directos entre artistas y resumen de conversaciones")
public class PersonalChatController {

    private final ChatPersonalService chatPersonalService;
    private final JwtUserResolver userResolver;

    public PersonalChatController(ChatPersonalService chatPersonalService, JwtUserResolver userResolver) {
        this.chatPersonalService = chatPersonalService;
        this.userResolver = userResolver;
    }

    @GetMapping("/api/mensajes/conversaciones")
    public List<ConversacionResumenDto> conversaciones(HttpServletRequest request) {
        var user = userResolver.resolver(request);
        return chatPersonalService.listarConversaciones(user.id());
    }

    @GetMapping("/api/mensajes/con/{otroUsuarioId}")
    public List<MensajePersonalDto> historialCon(@PathVariable UUID otroUsuarioId, HttpServletRequest request) {
        var user = userResolver.resolver(request);
        return chatPersonalService.historial(user.id(), otroUsuarioId);
    }

    @PostMapping("/api/mensajes")
    public ResponseEntity<MensajePersonalDto> enviar(@Valid @RequestBody EnviarMensajePersonalRequest req,
                                                     HttpServletRequest request) {
        var user = userResolver.resolver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatPersonalService.enviar(user.id(), user.nombreArtista(), req));
    }
}
