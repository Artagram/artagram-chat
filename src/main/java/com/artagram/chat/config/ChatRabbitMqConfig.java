package com.artagram.chat.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sin este bean, el @RabbitListener de ChatEventListener usa el converter por
 * defecto de Spring Boot (no sabe deserializar JSON a Map), y cae con
 * "Cannot convert from [B] to [java.util.Map]" en CUALQUIER evento entrante
 * (afectaba a chat.room.message desde siempre, no solo a la cola nueva de
 * chat personal). Mismo patron que el resto de los servicios del workspace.
 */
@Configuration
public class ChatRabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
