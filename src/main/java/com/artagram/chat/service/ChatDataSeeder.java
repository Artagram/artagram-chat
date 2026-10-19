package com.artagram.chat.service;

import com.artagram.chat.domain.MensajeComunidadDocument;
import com.artagram.chat.domain.MensajeComunidadRepository;
import com.artagram.chat.domain.MensajePersonalDocument;
import com.artagram.chat.domain.MensajePersonalRepository;
import com.artagram.chat.domain.NotificacionDocument;
import com.artagram.chat.domain.NotificacionRepository;
import com.artagram.chat.domain.UsuarioDirectorioDocument;
import com.artagram.chat.domain.UsuarioDirectorioRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("seed")
public class ChatDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ChatDataSeeder.class);

    private final UsuarioDirectorioRepository directorioRepo;
    private final MensajePersonalRepository personalRepo;
    private final MensajeComunidadRepository comunidadRepo;
    private final NotificacionRepository notifRepo;

    public ChatDataSeeder(UsuarioDirectorioRepository directorioRepo,
                          MensajePersonalRepository personalRepo,
                          MensajeComunidadRepository comunidadRepo,
                          NotificacionRepository notifRepo) {
        this.directorioRepo = directorioRepo;
        this.personalRepo = personalRepo;
        this.comunidadRepo = comunidadRepo;
        this.notifRepo = notifRepo;
    }

    @Override
    public void run(String... args) {
        log.info("Iniciando seed de datos para artagram-chat con usuarios oficiales...");

        List<UsuarioDirectorioDocument> usuarios = List.of(
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001"), "lucia_acuarela", "lucia.mora@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000002"), "sam_pixel", "sam.rivera@correo.com", true),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000003"), "noa_tinta", "noa.delgado@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000004"), "ivan_urbano", "ivan.rueda@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000005"), "maria_paz_art", "maria.paz@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000006"), "diego_grabado", "diego.leon@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000007"), "pincel_artagram", "prueba.artagram@correo.com", true),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0001-4000-8000-000000000001"), "camila_oleo", "camila.oleo@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0002-4000-8000-000000000002"), "mateo_3d", "mateo.3d@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0003-4000-8000-000000000003"), "valeria_botanica", "valeria.botanica@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0004-4000-8000-000000000004"), "andres_concept", "andres.concept@correo.com", false),
            new UsuarioDirectorioDocument(UUID.fromString("a1b2c3d4-0005-4000-8000-000000000005"), "elena_collage", "elena.collage@correo.com", false)
        );

        for (var u : usuarios) {
            if (!directorioRepo.existsById(u.getId())) {
                directorioRepo.save(u);
            }
        }

        UUID luciaId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001");
        UUID samId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000002");
        UUID noaId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000003");

        String convKey = MensajePersonalDocument.calcularClave(luciaId, samId);
        if (personalRepo.findByConversacionKeyOrderByCreadoEnAsc(convKey).isEmpty()) {
            personalRepo.save(new MensajePersonalDocument(UUID.randomUUID(), luciaId, "lucia_acuarela", samId, "sam_pixel", "¡Hola Sam! Me encantó la paleta de tu última obra en 8-bit."));
            personalRepo.save(new MensajePersonalDocument(UUID.randomUUID(), samId, "sam_pixel", luciaId, "lucia_acuarela", "¡Muchas gracias Lucía! Tu acuarela sobre papel tiene una luz increíble."));
        }

        UUID comunidadJardines = UUID.fromString("c0ffee00-0000-4000-8000-000000000001");
        if (comunidadRepo.findByComunidadIdOrderByCreadoEnAsc(comunidadJardines).isEmpty()) {
            comunidadRepo.save(new MensajeComunidadDocument(UUID.randomUUID(), comunidadJardines, luciaId, "lucia_acuarela", "¡Bienvenidos! Cuelguen lo que estén pintando esta semana."));
        }

        UUID comunidadPixel = UUID.fromString("c0ffee00-0000-4000-8000-000000000002");
        if (comunidadRepo.findByComunidadIdOrderByCreadoEnAsc(comunidadPixel).isEmpty()) {
            comunidadRepo.save(new MensajeComunidadDocument(UUID.randomUUID(), comunidadPixel, samId, "sam_pixel", "Practicando paleta limitada, cualquier feedback sirve"));
        }

        UUID comunidadTinta = UUID.fromString("c0ffee00-0000-4000-8000-000000000003");
        if (comunidadRepo.findByComunidadIdOrderByCreadoEnAsc(comunidadTinta).isEmpty()) {
            comunidadRepo.save(new MensajeComunidadDocument(UUID.randomUUID(), comunidadTinta, noaId, "noa_tinta", "Nueva plancha de grabado lista para probar"));
        }

        if (notifRepo.findByUsuarioIdOrderByCreadoEnDesc(luciaId).isEmpty()) {
            notifRepo.save(new NotificacionDocument(UUID.randomUUID(), luciaId, "SEGUIDOR", "sam_pixel comenzó a seguirte.", "/artistas/sam_pixel"));
        }

        log.info("Seed de datos para artagram-chat completado exitosamente.");
    }
}
