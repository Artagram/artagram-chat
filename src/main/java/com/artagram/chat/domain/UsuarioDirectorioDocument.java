package com.artagram.chat.domain;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("usuarios_directorio")
public class UsuarioDirectorioDocument {

    @Id
    private UUID id;
    private String nombreArtista;
    private String correo;
    private boolean premium;

    public UsuarioDirectorioDocument() {
    }

    public UsuarioDirectorioDocument(UUID id, String nombreArtista, String correo, boolean premium) {
        this.id = id;
        this.nombreArtista = nombreArtista;
        this.correo = correo;
        this.premium = premium;
    }

    public UUID getId() { return id; }
    public String getNombreArtista() { return nombreArtista; }
    public void setNombreArtista(String nombreArtista) { this.nombreArtista = nombreArtista; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }
}
