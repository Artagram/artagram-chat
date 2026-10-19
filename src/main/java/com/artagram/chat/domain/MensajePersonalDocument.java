package com.artagram.chat.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("mensajes_personales")
@CompoundIndex(name = "idx_conv_creado", def = "{'conversacionKey': 1, 'creadoEn': 1}")
@CompoundIndex(name = "idx_dest_leido", def = "{'destinatarioId': 1, 'leido': 1}")
public class MensajePersonalDocument {

    @Id
    private UUID id;
    private String conversacionKey;
    private UUID remitenteId;
    private String remitenteNombre;
    private UUID destinatarioId;
    private String destinatarioNombre;
    private String contenido;
    private boolean leido;
    private Instant creadoEn;

    public MensajePersonalDocument() {
    }

    public MensajePersonalDocument(UUID id, UUID remitenteId, String remitenteNombre,
                                   UUID destinatarioId, String destinatarioNombre, String contenido) {
        this.id = id != null ? id : UUID.randomUUID();
        this.conversacionKey = calcularClave(remitenteId, destinatarioId);
        this.remitenteId = remitenteId;
        this.remitenteNombre = remitenteNombre;
        this.destinatarioId = destinatarioId;
        this.destinatarioNombre = destinatarioNombre;
        this.contenido = contenido;
        this.leido = false;
        this.creadoEn = Instant.now();
    }

    public static String calcularClave(UUID u1, UUID u2) {
        return u1.compareTo(u2) < 0 ? u1 + ":" + u2 : u2 + ":" + u1;
    }

    public UUID getId() { return id; }
    public String getConversacionKey() { return conversacionKey; }
    public UUID getRemitenteId() { return remitenteId; }
    public String getRemitenteNombre() { return remitenteNombre; }
    public UUID getDestinatarioId() { return destinatarioId; }
    public String getDestinatarioNombre() { return destinatarioNombre; }
    public String getContenido() { return contenido; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
    public Instant getCreadoEn() { return creadoEn; }
}
