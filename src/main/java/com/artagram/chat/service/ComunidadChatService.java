package com.artagram.chat.service;

import com.artagram.chat.common.ApiException;
import com.artagram.chat.domain.MensajeComunidadDocument;
import com.artagram.chat.domain.MensajeComunidadRepository;
import com.artagram.chat.dto.EnviarMensajeComunidadRequest;
import com.artagram.chat.dto.MensajeComunidadDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ComunidadChatService {

    private final MensajeComunidadRepository mensajeRepository;
    private final LienzoClient lienzoClient;

    public ComunidadChatService(MensajeComunidadRepository mensajeRepository,
                                LienzoClient lienzoClient) {
        this.mensajeRepository = mensajeRepository;
        this.lienzoClient = lienzoClient;
    }

    public List<MensajeComunidadDto> historial(UUID comunidadId, UUID usuarioId, boolean premium, Integer dias) {
        if (!lienzoClient.esMiembroActivo(comunidadId, usuarioId)) {
            throw ApiException.forbidden("NO_ERES_MIEMBRO", "No perteneces a esta comunidad");
        }

        int maxDias = premium ? (dias != null ? dias : 365) : Math.min(dias != null ? dias : 30, 30);
        Instant desde = Instant.now().minus(maxDias, ChronoUnit.DAYS);

        return mensajeRepository.findByComunidadIdAndCreadoEnAfterOrderByCreadoEnAsc(comunidadId, desde).stream()
                .map(this::aDto)
                .toList();
    }

    public MensajeComunidadDto enviar(UUID comunidadId, UUID remitenteId, String remitenteNombre, EnviarMensajeComunidadRequest req) {
        if (!lienzoClient.esMiembroActivo(comunidadId, remitenteId)) {
            throw ApiException.forbidden("SIN_PERMISO", "No perteneces a esta comunidad (o te expulsaron)");
        }

        MensajeComunidadDocument doc = new MensajeComunidadDocument(
                UUID.randomUUID(), comunidadId, remitenteId, remitenteNombre, req.contenido()
        );
        doc = mensajeRepository.save(doc);
        return aDto(doc);
    }

    public void guardarDesdeStomp(UUID id, UUID comunidadId, UUID autorId, String autorNombre, String contenido, Instant creadoEn) {
        if (mensajeRepository.existsById(id)) {
            return;
        }
        MensajeComunidadDocument doc = new MensajeComunidadDocument(id, comunidadId, autorId, autorNombre, contenido);
        mensajeRepository.save(doc);
    }

    private MensajeComunidadDto aDto(MensajeComunidadDocument doc) {
        return new MensajeComunidadDto(
                doc.getId(), doc.getRemitenteId(), doc.getRemitenteNombre(), doc.getContenido(), doc.getCreadoEn()
        );
    }
}
