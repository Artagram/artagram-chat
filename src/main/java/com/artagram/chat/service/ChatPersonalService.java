package com.artagram.chat.service;

import com.artagram.chat.common.ApiException;
import com.artagram.chat.domain.MensajePersonalDocument;
import com.artagram.chat.domain.MensajePersonalRepository;
import com.artagram.chat.domain.NotificacionDocument;
import com.artagram.chat.domain.UsuarioDirectorioDocument;
import com.artagram.chat.domain.UsuarioDirectorioRepository;
import com.artagram.chat.dto.ConversacionResumenDto;
import com.artagram.chat.dto.EnviarMensajePersonalRequest;
import com.artagram.chat.dto.MensajePersonalDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChatPersonalService {

    private final MensajePersonalRepository mensajeRepository;
    private final UsuarioDirectorioRepository directorioRepository;
    private final NotificacionSseService sseService;

    public ChatPersonalService(MensajePersonalRepository mensajeRepository,
                               UsuarioDirectorioRepository directorioRepository,
                               NotificacionSseService sseService) {
        this.mensajeRepository = mensajeRepository;
        this.directorioRepository = directorioRepository;
        this.sseService = sseService;
    }

    public List<ConversacionResumenDto> listarConversaciones(UUID usuarioId) {
        List<MensajePersonalDocument> todos = mensajeRepository.findByRemitenteIdOrDestinatarioIdOrderByCreadoEnDesc(usuarioId, usuarioId);
        Map<UUID, ConversacionResumenDto> resumenes = new LinkedHashMap<>();

        for (MensajePersonalDocument m : todos) {
            UUID otroId = m.getRemitenteId().equals(usuarioId) ? m.getDestinatarioId() : m.getRemitenteId();
            if (!resumenes.containsKey(otroId)) {
                String otroNombre = m.getRemitenteId().equals(usuarioId) ? m.getDestinatarioNombre() : m.getRemitenteNombre();
                if (otroNombre == null || otroNombre.isBlank()) {
                    otroNombre = directorioRepository.findById(otroId)
                            .map(UsuarioDirectorioDocument::getNombreArtista)
                            .orElse("artista");
                }
                resumenes.put(otroId, new ConversacionResumenDto(
                        otroId, otroNombre, m.getContenido(), m.getCreadoEn(), m.isLeido()
                ));
            }
        }
        return new ArrayList<>(resumenes.values());
    }

    public List<MensajePersonalDto> historial(UUID usuarioId, UUID otroId) {
        String key = MensajePersonalDocument.calcularClave(usuarioId, otroId);
        List<MensajePersonalDocument> lista = mensajeRepository.findByConversacionKeyOrderByCreadoEnAsc(key);
        for (MensajePersonalDocument m : lista) {
            if (m.getDestinatarioId().equals(usuarioId) && !m.isLeido()) {
                m.setLeido(true);
                mensajeRepository.save(m);
            }
        }
        return lista.stream().map(this::aDto).toList();
    }

    public MensajePersonalDto enviar(UUID remitenteId, String remitenteNombre, EnviarMensajePersonalRequest req) {
        if (remitenteId.equals(req.destinatarioId())) {
            throw ApiException.badRequest("DESTINATARIO_INVALIDO", "No puedes enviarte mensajes a ti mismo");
        }

        String destNombre = directorioRepository.findById(req.destinatarioId())
                .map(UsuarioDirectorioDocument::getNombreArtista)
                .orElse("artista");

        MensajePersonalDocument doc = new MensajePersonalDocument(
                UUID.randomUUID(), remitenteId, remitenteNombre, req.destinatarioId(), destNombre, req.contenido()
        );
        doc = mensajeRepository.save(doc);

        sseService.emitir(new NotificacionDocument(
                UUID.randomUUID(), req.destinatarioId(), "MENSAJE_DIRECTO", remitenteId,
                remitenteNombre, doc.getId().toString(), "Te envió un mensaje: " + req.contenido()
        ));

        return aDto(doc);
    }

    /**
     * Persiste un mensaje que ya se transmitio en vivo por STOMP en
     * artagram-canvas (ver {@code personal.message.sent} en
     * ChatEventListener) — artagram-chat sigue siendo el unico dueño de la
     * persistencia, canvas no guarda copia local. Mantiene la misma
     * notificacion SSE que el envio via REST para que el comportamiento sea
     * idéntico sin importar el canal de entrada.
     */
    public MensajePersonalDto guardarDesdeStomp(UUID id, UUID remitenteId, String remitenteNombre,
                                                 UUID destinatarioId, String contenido) {
        if (mensajeRepository.existsById(id)) {
            return aDto(mensajeRepository.findById(id).orElseThrow());
        }
        String destNombre = directorioRepository.findById(destinatarioId)
                .map(UsuarioDirectorioDocument::getNombreArtista)
                .orElse("artista");

        MensajePersonalDocument doc = new MensajePersonalDocument(
                id, remitenteId, remitenteNombre, destinatarioId, destNombre, contenido
        );
        doc = mensajeRepository.save(doc);

        sseService.emitir(new NotificacionDocument(
                UUID.randomUUID(), destinatarioId, "MENSAJE_DIRECTO", remitenteId,
                remitenteNombre, doc.getId().toString(), "Te envió un mensaje: " + contenido
        ));

        return aDto(doc);
    }

    private MensajePersonalDto aDto(MensajePersonalDocument doc) {
        return new MensajePersonalDto(
                doc.getId(), doc.getRemitenteId(), doc.getRemitenteNombre(),
                doc.getDestinatarioId(), doc.getContenido(), doc.isLeido(), doc.getCreadoEn()
        );
    }
}
