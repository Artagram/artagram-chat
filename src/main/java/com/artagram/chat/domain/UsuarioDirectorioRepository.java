package com.artagram.chat.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsuarioDirectorioRepository extends MongoRepository<UsuarioDirectorioDocument, UUID> {

    Optional<UsuarioDirectorioDocument> findByNombreArtistaIgnoreCase(String nombreArtista);
}
