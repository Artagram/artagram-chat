package com.artagram.chat.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MensajeComunidadRepository extends MongoRepository<MensajeComunidadDocument, UUID> {

    List<MensajeComunidadDocument> findByComunidadIdAndCreadoEnAfterOrderByCreadoEnAsc(UUID comunidadId, Instant creadoEn);

    List<MensajeComunidadDocument> findByComunidadIdOrderByCreadoEnAsc(UUID comunidadId);
}
