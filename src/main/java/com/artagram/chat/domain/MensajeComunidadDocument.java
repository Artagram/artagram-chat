package com.artagram.chat.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("mensajes_comunidad")
@CompoundIndex(name = "idx_comunidad_creado", def = "{'comunidadId': 1, 'creadoEn': 1}")
public class MensajeComunidadDocument {

    @Id
    private UUID id;
    private UUID comunidadId;
    private UUID remitenteId;
    private String remitenteNombre;
    private String contenido;
    private Instant creadoEn;

    public MensajeComunidadDocument() {
    }

    public MensajeComunidadDocument(UUID id, UUID comunidadId, UUID remitenteId, String remitenteNombre, String contenido) {
        this.id = id != null ? id : UUID.randomUUID();
        this.comunidadId = comunidadId;
        this.remitenteId = remitenteId;
        this.remitenteNombre = remitenteNombre;
        this.contenido = contenido;
        this.creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getComunidadId() { return comunidadId; }
    public UUID getRemitenteId() { return remitenteId; }
    public String getRemitenteNombre() { return remitenteNombre; }
    public String getContenido() { return contenido; }
    public Instant getCreadoEn() { return creadoEn; }
}
