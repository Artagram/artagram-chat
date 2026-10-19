package com.artagram.chat.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificacionRepository extends MongoRepository<NotificacionDocument, UUID> {

    List<NotificacionDocument> findTop50ByDestinatarioIdOrderByCreadoEnDesc(UUID destinatarioId);

    long countByDestinatarioIdAndLeidaFalse(UUID destinatarioId);
}
