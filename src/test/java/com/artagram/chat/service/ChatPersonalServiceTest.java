package com.artagram.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.chat.common.ApiException;
import com.artagram.chat.domain.MensajePersonalDocument;
import com.artagram.chat.domain.MensajePersonalRepository;
import com.artagram.chat.domain.UsuarioDirectorioDocument;
import com.artagram.chat.domain.UsuarioDirectorioRepository;
import com.artagram.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.chat.dto.MensajePersonalDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatPersonalServiceTest {

    @Mock
    private MensajePersonalRepository mensajeRepository;

    @Mock
    private UsuarioDirectorioRepository directorioRepository;

    @Mock
    private NotificacionSseService sseService;

    private ChatPersonalService service;

    @BeforeEach
    void setUp() {
        service = new ChatPersonalService(mensajeRepository, directorioRepository, sseService);
    }

    @Test
    void noPermiteEnviarMensajeAUnoMismo() {
        UUID mismoId = UUID.randomUUID();
        EnviarMensajePersonalRequest req = new EnviarMensajePersonalRequest(mismoId, "Hola yo");

        assertThatThrownBy(() -> service.enviar(mismoId, "usuario1", req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No puedes enviarte mensajes a ti mismo");
    }

    @Test
    void enviaMensajeCorrectamenteYEmitiraNotificacion() {
        UUID remitenteId = UUID.randomUUID();
        UUID destinatarioId = UUID.randomUUID();
        EnviarMensajePersonalRequest req = new EnviarMensajePersonalRequest(destinatarioId, "Hola que tal!");

        when(directorioRepository.findById(destinatarioId))
                .thenReturn(Optional.of(new UsuarioDirectorioDocument(destinatarioId, "marcos_oleo", "marcos@artagram.local", false)));

        when(mensajeRepository.save(any(MensajePersonalDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MensajePersonalDto dto = service.enviar(remitenteId, "elena_acuarela", req);

        assertThat(dto).isNotNull();
        assertThat(dto.remitenteId()).isEqualTo(remitenteId);
        assertThat(dto.destinatarioId()).isEqualTo(destinatarioId);
        assertThat(dto.contenido()).isEqualTo("Hola que tal!");
        verify(sseService).emitir(any());
    }
}
