package com.artagram.chat.service;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LienzoClient {

    private static final Logger log = LoggerFactory.getLogger(LienzoClient.class);

    private final RestTemplate restTemplate;
    private final String lienzoInternalUrl;

    public LienzoClient(RestTemplateBuilder builder,
                        @Value("${app.lienzo.internal-url:http://localhost:8084}") String lienzoInternalUrl) {
        this.restTemplate = builder.build();
        this.lienzoInternalUrl = lienzoInternalUrl;
    }

    public boolean esMiembroActivo(UUID comunidadId, UUID usuarioId) {
        String url = lienzoInternalUrl + "/internal/communities/" + comunidadId + "/members/" + usuarioId;
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("miembroActivo"));
            }
        } catch (Exception e) {
            log.warn("No se pudo consultar membresía en artagram-lienzo ({}), asumiendo acceso permitido en dev", e.getMessage());
            return true; // En desarrollo local permite continuar si lienzo aún está iniciando
        }
        return false;
    }
}
