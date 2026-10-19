package com.artagram.chat.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class JwtUserResolver {

    private static final Logger log = LoggerFactory.getLogger(JwtUserResolver.class);

    private final RSAPublicKey rsaPublicKey;

    public JwtUserResolver(
            @Value("${app.jwt.public-key-path:../artagram-infra/secrets-dev/jwt-public.pem}") String publicKeyPath,
            @Value("${app.jwt.public-key-pem:}") String publicKeyPem) {
        this.rsaPublicKey = cargarLlavePublicaRsa(publicKeyPath, publicKeyPem);
    }

    public UsuarioAutenticado resolver(HttpServletRequest request) {
        String xUserId = request.getHeader("X-User-Id");
        if (xUserId != null && !xUserId.isBlank()) {
            try {
                UUID id = UUID.fromString(xUserId.trim());
                String nombre = request.getHeader("X-User-Name");
                String rol = request.getHeader("X-User-Role");
                boolean premium = Boolean.parseBoolean(request.getHeader("X-User-Premium"));
                return new UsuarioAutenticado(id, nombre != null ? nombre : "artista", rol != null ? rol : "USUARIO", premium);
            } catch (IllegalArgumentException ignored) {}
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (request.getParameter("token") != null) {
            token = request.getParameter("token").trim();
        }

        if (token != null && !token.isBlank()) {
            return validarToken(token).orElseThrow(() -> ApiException.unauthorized("TOKEN_INVALIDO", "Token inválido o expirado"));
        }

        throw ApiException.unauthorized("NO_AUTENTICADO", "Debes iniciar sesión para continuar");
    }

    public Optional<UsuarioAutenticado> validarToken(String token) {
        if (rsaPublicKey == null) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(rsaPublicKey).build().parseSignedClaims(token).getPayload();
            UUID id = UUID.fromString(claims.getSubject());
            String nombre = claims.get("nombreArtista", String.class);
            if (nombre == null) nombre = claims.get("nombre", String.class);
            String rol = claims.get("rolGlobal", String.class);
            if (rol == null) rol = claims.get("rol", String.class);
            boolean premium = Boolean.TRUE.equals(claims.get("premium", Boolean.class));
            return Optional.of(new UsuarioAutenticado(id, nombre != null ? nombre : "artista", rol != null ? rol : "USUARIO", premium));
        } catch (Exception e) {
            log.debug("Fallo validación JWT RS256 en chat: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static RSAPublicKey cargarLlavePublicaRsa(String pathStr, String pemStr) {
        try {
            String pem = pemStr;
            if ((pem == null || pem.isBlank()) && pathStr != null && !pathStr.isBlank()) {
                Path p = Path.of(pathStr);
                if (Files.exists(p)) {
                    pem = Files.readString(p);
                }
            }
            if (pem != null && !pem.isBlank()) {
                String limpio = pem.replaceAll("-----BEGIN [A-Z ]+-----", "").replaceAll("-----END [A-Z ]+-----", "").replaceAll("\\s+", "");
                byte[] der = Base64.getDecoder().decode(limpio);
                return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar clave pública RSA en chat ({})", e.getMessage());
        }
        return null;
    }

    public record UsuarioAutenticado(UUID id, String nombreArtista, String rolGlobal, boolean premium) {}
}
