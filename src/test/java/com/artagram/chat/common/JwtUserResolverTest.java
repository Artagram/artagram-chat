package com.artagram.chat.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtUserResolverTest {

    private static KeyPair par;
    private static String pemPublico;

    @TempDir
    Path dir;

    @BeforeAll
    static void generarLlaves() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        par = gen.generateKeyPair();
        pemPublico = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(par.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private static String token(UUID id, String claimNombre, String nombre, String claimRol, String rol, Boolean premium) {
        var builder = Jwts.builder().subject(id.toString()).expiration(new Date(System.currentTimeMillis() + 60_000));
        if (nombre != null) builder.claim(claimNombre, nombre);
        if (rol != null) builder.claim(claimRol, rol);
        if (premium != null) builder.claim("premium", premium);
        return builder.signWith(par.getPrivate()).compact();
    }

    private final JwtUserResolver resolver = new JwtUserResolver("", pemPublico);

    @Test
    void usaLasCabecerasDelGatewaySiVienen() {
        UUID id = UUID.randomUUID();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", " " + id + " ");
        req.addHeader("X-User-Name", "lucia");
        req.addHeader("X-User-Role", "ADMIN");
        req.addHeader("X-User-Premium", "true");

        var usuario = resolver.resolver(req);

        assertThat(usuario).isEqualTo(new JwtUserResolver.UsuarioAutenticado(id, "lucia", "ADMIN", true));

        MockHttpServletRequest sinNombre = new MockHttpServletRequest();
        sinNombre.addHeader("X-User-Id", id.toString());
        assertThat(resolver.resolver(sinNombre).nombreArtista()).isEqualTo("artista");
        assertThat(resolver.resolver(sinNombre).rolGlobal()).isEqualTo("USUARIO");
    }

    @Test
    void validaElBearerConLaLlaveRsa() {
        UUID id = UUID.randomUUID();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "no-es-uuid");
        req.addHeader("Authorization", "Bearer " + token(id, "nombreArtista", "sam", "rolGlobal", "USUARIO", true));

        var usuario = resolver.resolver(req);

        assertThat(usuario.id()).isEqualTo(id);
        assertThat(usuario.nombreArtista()).isEqualTo("sam");
        assertThat(usuario.premium()).isTrue();
    }

    @Test
    void aceptaElTokenPorQueryParamYClaimsAlternativos() {
        UUID id = UUID.randomUUID();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("token", token(id, "nombre", "noa", "rol", "ADMIN", null));

        var usuario = resolver.resolver(req);

        assertThat(usuario.nombreArtista()).isEqualTo("noa");
        assertThat(usuario.rolGlobal()).isEqualTo("ADMIN");
        assertThat(usuario.premium()).isFalse();

        assertThat(resolver.validarToken(token(id, "x", null, "y", null, null)).orElseThrow())
                .extracting(JwtUserResolver.UsuarioAutenticado::nombreArtista, JwtUserResolver.UsuarioAutenticado::rolGlobal)
                .containsExactly("artista", "USUARIO");
    }

    @Test
    void sinCredencialesOConTokenInvalidoEs401() {
        assertThatThrownBy(() -> resolver.resolver(new MockHttpServletRequest()))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("NO_AUTENTICADO");

        MockHttpServletRequest malo = new MockHttpServletRequest();
        malo.addHeader("Authorization", "Bearer basura");
        assertThatThrownBy(() -> resolver.resolver(malo))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void cargaLaLlaveDesdeArchivoYSinLlaveRechazaTodo() throws Exception {
        Path archivo = Files.writeString(dir.resolve("pub.pem"), pemPublico);
        UUID id = UUID.randomUUID();
        String jwt = token(id, "nombreArtista", "x", "rolGlobal", "USUARIO", false);

        assertThat(new JwtUserResolver(archivo.toString(), "").validarToken(jwt)).isPresent();
        assertThat(new JwtUserResolver(dir.resolve("no-existe.pem").toString(), "").validarToken(jwt)).isEmpty();
        assertThat(new JwtUserResolver("", "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----").validarToken(jwt))
                .isEmpty();
    }
}
