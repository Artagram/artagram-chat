package com.artagram.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.artagram.chat.common.ApiException;
import com.artagram.chat.domain.MensajeComunidadDocument;
import com.artagram.chat.domain.MensajeComunidadRepository;
import com.artagram.chat.domain.MensajePersonalDocument;
import com.artagram.chat.domain.MensajePersonalRepository;
import com.artagram.chat.domain.UsuarioDirectorioDocument;
import com.artagram.chat.domain.UsuarioDirectorioRepository;
import com.artagram.chat.dto.EnviarMensajeComunidadRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

class ServiciosChatTest {

    private final UUID yo = UUID.randomUUID();
    private final UUID otro = UUID.randomUUID();
    private final UUID comunidad = UUID.randomUUID();

    // --- ComunidadChatService ---

    private final MensajeComunidadRepository comunidadRepo = mock(MensajeComunidadRepository.class);
    private final LienzoClient lienzo = mock(LienzoClient.class);
    private final ComunidadChatService comunidadService = new ComunidadChatService(comunidadRepo, lienzo);

    @Test
    void historialDeComunidadLimitaLosDiasSegunElPlan() {
        when(lienzo.esMiembroActivo(comunidad, yo)).thenReturn(true);
        MensajeComunidadDocument doc = new MensajeComunidadDocument(null, comunidad, otro, "sam", "hola");
        when(comunidadRepo.findByComunidadIdAndCreadoEnAfterOrderByCreadoEnAsc(eq(comunidad), any())).thenReturn(List.of(doc));

        assertThat(comunidadService.historial(comunidad, yo, false, 90)).singleElement()
                .extracting("contenido").isEqualTo("hola");
        comunidadService.historial(comunidad, yo, false, null);
        comunidadService.historial(comunidad, yo, true, null);
        comunidadService.historial(comunidad, yo, true, 400);

        ArgumentCaptor<Instant> desde = ArgumentCaptor.forClass(Instant.class);
        verify(comunidadRepo, org.mockito.Mockito.times(4))
                .findByComunidadIdAndCreadoEnAfterOrderByCreadoEnAsc(eq(comunidad), desde.capture());
        List<Long> dias = desde.getAllValues().stream()
                .map(d -> Math.round(ChronoUnit.HOURS.between(d, Instant.now()) / 24.0)).toList();
        assertThat(dias.get(0)).isEqualTo(30L);
        assertThat(dias.get(1)).isEqualTo(30L);
        assertThat(dias.get(2)).isEqualTo(365L);
        assertThat(dias.get(3)).isEqualTo(400L);
    }

    @Test
    void noMiembrosNoLeenNiEscriben() {
        when(lienzo.esMiembroActivo(comunidad, yo)).thenReturn(false);

        assertThatThrownBy(() -> comunidadService.historial(comunidad, yo, true, 1))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("NO_ERES_MIEMBRO");
        assertThatThrownBy(() -> comunidadService.enviar(comunidad, yo, "lucia", new EnviarMensajeComunidadRequest("x")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SIN_PERMISO");
    }

    @Test
    void enviarYGuardarDesdeStompEnComunidad() {
        when(lienzo.esMiembroActivo(comunidad, yo)).thenReturn(true);
        when(comunidadRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID repetido = UUID.randomUUID();
        when(comunidadRepo.existsById(repetido)).thenReturn(true);

        assertThat(comunidadService.enviar(comunidad, yo, "lucia", new EnviarMensajeComunidadRequest("hey")).remitenteNombre())
                .isEqualTo("lucia");
        comunidadService.guardarDesdeStomp(UUID.randomUUID(), comunidad, yo, "lucia", "en vivo", Instant.now());
        comunidadService.guardarDesdeStomp(repetido, comunidad, yo, "lucia", "duplicado", Instant.now());

        verify(comunidadRepo, org.mockito.Mockito.times(2)).save(any());
    }

    // --- ChatPersonalService ---

    private final MensajePersonalRepository personalRepo = mock(MensajePersonalRepository.class);
    private final UsuarioDirectorioRepository directorio = mock(UsuarioDirectorioRepository.class);
    private final ChatNotificationPublisher notificador = mock(ChatNotificationPublisher.class);
    private final ChatPersonalService personal = new ChatPersonalService(personalRepo, directorio, notificador);

    @Test
    void listarConversacionesAgrupaPorContactoYCompletaElNombre() {
        UUID tercero = UUID.randomUUID();
        var recibido = new MensajePersonalDocument(UUID.randomUUID(), otro, "sam", yo, "lucia", "último de sam");
        var enviadoSinNombre = new MensajePersonalDocument(UUID.randomUUID(), yo, "lucia", tercero, "", "para el tercero");
        var viejoDeSam = new MensajePersonalDocument(UUID.randomUUID(), yo, "lucia", otro, "sam", "viejo");
        when(personalRepo.findByRemitenteIdOrDestinatarioIdOrderByCreadoEnDesc(yo, yo))
                .thenReturn(List.of(recibido, enviadoSinNombre, viejoDeSam));
        when(directorio.findById(tercero)).thenReturn(Optional.of(new UsuarioDirectorioDocument(tercero, "noa", "n@c.com", false)));

        var resumen = personal.listarConversaciones(yo);

        assertThat(resumen).hasSize(2);
        assertThat(resumen.get(0).contactoNombre()).isEqualTo("sam");
        assertThat(resumen.get(0).ultimoMensaje()).isEqualTo("último de sam");
        assertThat(resumen.get(1).contactoNombre()).isEqualTo("noa");
    }

    @Test
    void historialMarcaComoLeidosLosMensajesRecibidos() {
        var recibido = new MensajePersonalDocument(UUID.randomUUID(), otro, "sam", yo, "lucia", "hola");
        var enviado = new MensajePersonalDocument(UUID.randomUUID(), yo, "lucia", otro, "sam", "qué tal");
        when(personalRepo.findByConversacionKeyOrderByCreadoEnAsc(MensajePersonalDocument.calcularClave(yo, otro)))
                .thenReturn(List.of(recibido, enviado));

        var historial = personal.historial(yo, otro);

        assertThat(historial).hasSize(2);
        assertThat(recibido.isLeido()).isTrue();
        assertThat(enviado.isLeido()).isFalse();
        verify(personalRepo).save(recibido);
        verify(personalRepo, never()).save(enviado);
    }

    @Test
    void guardarDesdeStompEsIdempotenteYNotifica() {
        UUID nuevo = UUID.randomUUID();
        UUID existente = UUID.randomUUID();
        var guardado = new MensajePersonalDocument(existente, yo, "lucia", otro, "sam", "ya estaba");
        when(personalRepo.existsById(existente)).thenReturn(true);
        when(personalRepo.findById(existente)).thenReturn(Optional.of(guardado));
        when(personalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(directorio.findById(otro)).thenReturn(Optional.empty());

        assertThat(personal.guardarDesdeStomp(existente, yo, "lucia", otro, "x").contenido()).isEqualTo("ya estaba");
        assertThat(personal.guardarDesdeStomp(nuevo, yo, "lucia", otro, "nuevo").id()).isEqualTo(nuevo);

        verify(notificador).publicarMensajeDirecto(otro, yo, "lucia", nuevo, "nuevo");
        verify(notificador, never()).publicarMensajeDirecto(any(), any(), any(), eq(existente), any());
    }

    // --- ChatNotificationPublisher ---

    @Test
    @SuppressWarnings("unchecked")
    void publicaLaNotificacionYToleraQueRabbitFalle() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        ChatNotificationPublisher publisher = new ChatNotificationPublisher(rabbit, "artagram.events");
        UUID msg = UUID.randomUUID();

        publisher.publicarMensajeDirecto(otro, yo, "lucia", msg, "hola");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq("artagram.events"), eq("personal.message.notification"), payload.capture());
        assertThat((Map<String, Object>) payload.getValue()).containsEntry("referenciaId", msg.toString());

        doThrow(new AmqpException("caido")).when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
        publisher.publicarMensajeDirecto(otro, yo, "lucia", msg, "hola");
    }

    // --- ChatEventListener ---

    @Test
    void elListenerReenviaLosEventosYToleraPayloadsInvalidos() {
        ComunidadChatService com = mock(ComunidadChatService.class);
        ChatPersonalService per = mock(ChatPersonalService.class);
        ChatEventListener listener = new ChatEventListener(com, per, directorio);
        UUID id = UUID.randomUUID();
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");

        listener.onRoomMessage(Map.of("id", id.toString(), "comunidadId", comunidad.toString(), "autorId", yo.toString(),
                "autorNombre", "lucia", "contenido", "hola", "creadoEn", creado.toString()));
        listener.onRoomMessage(Map.of("id", id.toString(), "comunidadId", comunidad.toString(), "autorId", yo.toString()));
        listener.onRoomMessage(Map.of("id", "mal"));
        listener.onPersonalMessage(Map.of("id", id.toString(), "remitenteId", yo.toString(), "remitenteNombre", "lucia",
                "destinatarioId", otro.toString(), "contenido", "hey"));
        listener.onPersonalMessage(Map.of("id", "mal"));
        listener.onUserDeleted(Map.of("usuarioId", otro.toString()));
        listener.onUserDeleted(Map.of("usuarioId", "mal"));

        verify(com).guardarDesdeStomp(id, comunidad, yo, "lucia", "hola", creado);
        verify(com).guardarDesdeStomp(eq(id), eq(comunidad), eq(yo), eq("artista"), eq(""), any(Instant.class));
        verify(per).guardarDesdeStomp(id, yo, "lucia", otro, "hey");
        verify(directorio).deleteById(otro);
    }

    // --- LienzoClient ---

    @Test
    void lienzoClientConsultaLaMembresia() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        LienzoClient client = new LienzoClient(new RestTemplateBuilder(customizer), "http://lienzo");
        MockRestServiceServer server = customizer.getServer();
        String base = "http://lienzo/internal/communities/" + comunidad + "/members/";

        server.expect(requestTo(base + yo)).andRespond(withSuccess("{\"miembroActivo\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(base + otro)).andRespond(withSuccess("{\"miembroActivo\":false}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(base + yo)).andRespond(withSuccess());
        server.expect(requestTo(base + yo)).andRespond(withServerError());

        assertThat(client.esMiembroActivo(comunidad, yo)).isTrue();
        assertThat(client.esMiembroActivo(comunidad, otro)).isFalse();
        assertThat(client.esMiembroActivo(comunidad, yo)).isFalse();
        assertThat(client.esMiembroActivo(comunidad, yo)).isTrue();
        server.verify();
    }

    // --- ChatDataSeeder ---

    @Test
    void elSeederSoloCreaLoQueFalta() {
        MensajeComunidadRepository comRepo = mock(MensajeComunidadRepository.class);
        when(directorio.existsById(any())).thenReturn(false);
        when(personalRepo.findByConversacionKeyOrderByCreadoEnAsc(anyString())).thenReturn(List.of());
        when(comRepo.findByComunidadIdOrderByCreadoEnAsc(any())).thenReturn(List.of());

        new ChatDataSeeder(directorio, personalRepo, comRepo).run();

        verify(directorio, org.mockito.Mockito.times(12)).save(any());
        verify(personalRepo, org.mockito.Mockito.times(2)).save(any());
        verify(comRepo, org.mockito.Mockito.times(3)).save(any());

        MensajeComunidadRepository lleno = mock(MensajeComunidadRepository.class);
        UsuarioDirectorioRepository dirLleno = mock(UsuarioDirectorioRepository.class);
        MensajePersonalRepository perLleno = mock(MensajePersonalRepository.class);
        when(dirLleno.existsById(any())).thenReturn(true);
        var msg = new MensajePersonalDocument(UUID.randomUUID(), yo, "a", otro, "b", "c");
        when(perLleno.findByConversacionKeyOrderByCreadoEnAsc(anyString())).thenReturn(List.of(msg));
        when(lleno.findByComunidadIdOrderByCreadoEnAsc(any())).thenReturn(List.of(new MensajeComunidadDocument()));
        new ChatDataSeeder(dirLleno, perLleno, lleno).run();
        verify(dirLleno, never()).save(any());
        verify(lleno, never()).save(any());
    }

    @Test
    void documentosExponenSusCampos() {
        var dir = new UsuarioDirectorioDocument();
        dir.setNombreArtista("lucia");
        dir.setCorreo("l@c.com");
        dir.setPremium(true);
        assertThat(dir.getNombreArtista()).isEqualTo("lucia");
        assertThat(dir.getCorreo()).isEqualTo("l@c.com");
        assertThat(dir.isPremium()).isTrue();

        var com = new MensajeComunidadDocument(null, comunidad, yo, "lucia", "hola");
        assertThat(com.getId()).isNotNull();
        assertThat(com.getComunidadId()).isEqualTo(comunidad);
        assertThat(com.getRemitenteId()).isEqualTo(yo);
        assertThat(com.getCreadoEn()).isNotNull();

        var per = new MensajePersonalDocument(UUID.randomUUID(), yo, "lucia", otro, "sam", "x");
        assertThat(per.getConversacionKey()).isEqualTo(MensajePersonalDocument.calcularClave(otro, yo));
        assertThat(per.getDestinatarioNombre()).isEqualTo("sam");
        assertThat(new MensajePersonalDocument().getId()).isNull();
    }
}
