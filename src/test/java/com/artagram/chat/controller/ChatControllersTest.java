package com.artagram.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artagram.chat.common.ApiException;
import com.artagram.chat.common.GlobalExceptionHandler;
import com.artagram.chat.common.JwtUserResolver;
import com.artagram.chat.dto.ConversacionResumenDto;
import com.artagram.chat.dto.MensajeComunidadDto;
import com.artagram.chat.dto.MensajePersonalDto;
import com.artagram.chat.service.ChatPersonalService;
import com.artagram.chat.service.ComunidadChatService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllersTest {

    private final UUID yo = UUID.randomUUID();
    private final UUID otro = UUID.randomUUID();
    private final UUID comunidad = UUID.randomUUID();

    private ChatPersonalService personal;
    private ComunidadChatService comunidadService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        personal = mock(ChatPersonalService.class);
        comunidadService = mock(ComunidadChatService.class);
        JwtUserResolver resolver = new JwtUserResolver("", "");
        mvc = MockMvcBuilders.standaloneSetup(
                        new PersonalChatController(personal, resolver),
                        new ComunidadChatController(comunidadService, resolver))
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void conversacionesHistorialYEnvioPersonal() throws Exception {
        Instant ahora = Instant.now();
        when(personal.listarConversaciones(yo)).thenReturn(List.of(new ConversacionResumenDto(otro, "sam", "hola", ahora, false)));
        when(personal.historial(yo, otro)).thenReturn(List.of(new MensajePersonalDto(UUID.randomUUID(), otro, "sam", yo, "hola", true, ahora)));
        when(personal.enviar(eq(yo), eq("lucia"), any())).thenReturn(new MensajePersonalDto(UUID.randomUUID(), yo, "lucia", otro, "hey", false, ahora));

        mvc.perform(get("/api/mensajes/conversaciones").header("X-User-Id", yo.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].contactoNombre").value("sam"));
        mvc.perform(get("/api/mensajes/con/" + otro).header("X-User-Id", yo.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].contenido").value("hola"));
        mvc.perform(post("/api/mensajes").header("X-User-Id", yo.toString()).header("X-User-Name", "lucia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatarioId\":\"" + otro + "\",\"contenido\":\"hey\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contenido").value("hey"));
    }

    @Test
    void chatDeComunidad() throws Exception {
        Instant ahora = Instant.now();
        when(comunidadService.historial(comunidad, yo, true, 7))
                .thenReturn(List.of(new MensajeComunidadDto(UUID.randomUUID(), otro, "sam", "hola", ahora)));
        when(comunidadService.enviar(eq(comunidad), eq(yo), eq("lucia"), any()))
                .thenReturn(new MensajeComunidadDto(UUID.randomUUID(), yo, "lucia", "hey", ahora));

        mvc.perform(get("/api/comunidades/" + comunidad + "/chat").param("dias", "7")
                        .header("X-User-Id", yo.toString()).header("X-User-Premium", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].remitenteNombre").value("sam"));
        mvc.perform(post("/api/comunidades/" + comunidad + "/chat").header("X-User-Id", yo.toString())
                        .header("X-User-Name", "lucia").contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"hey\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contenido").value("hey"));
    }

    @Test
    void erroresSeTraducenAlFormatoComun() throws Exception {
        when(comunidadService.historial(any(), any(), eq(false), any()))
                .thenThrow(ApiException.forbidden("NO_ERES_MIEMBRO", "no"));
        when(personal.listarConversaciones(yo)).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "CAIDO", "x"));
        when(personal.historial(any(), any())).thenThrow(new IllegalStateException("boom"));

        mvc.perform(get("/api/comunidades/" + comunidad + "/chat").header("X-User-Id", yo.toString()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NO_ERES_MIEMBRO"));
        mvc.perform(get("/api/mensajes/conversaciones").header("X-User-Id", yo.toString()))
                .andExpect(status().isBadGateway());
        mvc.perform(get("/api/mensajes/con/" + otro).header("X-User-Id", yo.toString()))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("ERROR_INTERNO"));
        mvc.perform(get("/api/mensajes/conversaciones"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        mvc.perform(post("/api/mensajes").header("X-User-Id", yo.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACION"));
    }
}
