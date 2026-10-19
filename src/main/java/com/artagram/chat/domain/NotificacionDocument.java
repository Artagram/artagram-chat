package com.artagram.chat.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("notificaciones")
@CompoundIndex(name = "idx_dest_creado", def = "{'destinatarioId': 1, 'creadoEn': -1}")
public class NotificacionDocument {

    @Id
    private UUID id;
    private UUID destinatarioId;
    private String tipo;
    private UUID actorId;
    private String actorNombre;
    private String referenciaId;
    private String mensaje;
    private boolean leida;
    private Instant creadoEn;

    public NotificacionDocument() {
    }

    public NotificacionDocument(UUID id, UUID destinatarioId, String tipo, UUID actorId,
                                String actorNombre, String referenciaId, String mensaje) {
        this.id = id != null ? id : UUID.randomUUID();
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.actorId = actorId;
        this.actorNombre = actorNombre;
        this.referenciaId = referenciaId;
        this.mensaje = mensaje;
        this.leida = false;
        this.creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public UUID getActorId() { return actorId; }
    public String getActorNombre() { return actorNombre; }
    public String getReferenciaId() { return referenciaId; }
    public String getMensaje() { return mensaje; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public Instant getCreadoEn() { return creadoEn; }
}
