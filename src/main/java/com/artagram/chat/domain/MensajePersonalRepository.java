package com.artagram.chat.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MensajePersonalRepository extends MongoRepository<MensajePersonalDocument, UUID> {

    List<MensajePersonalDocument> findByConversacionKeyOrderByCreadoEnAsc(String conversacionKey);

    List<MensajePersonalDocument> findByRemitenteIdOrDestinatarioIdOrderByCreadoEnDesc(UUID rId, UUID dId);

    List<MensajePersonalDocument> findByDestinatarioIdAndLeidoFalse(UUID destinatarioId);

    long countByConversacionKeyAndDestinatarioIdAndLeidoFalse(String conversacionKey, UUID destinatarioId);
}
