package br.com.nandesk.shared.json;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Colunas JSON ({@code @JdbcTypeCode(SqlTypes.JSON)}) serializadas com o mesmo Jackson 3 da API, para não
 * misturar Jackson 2 e 3 (especificação 8.5).
 */
@Configuration(proxyBeanMethods = false)
public class JsonColumnsConfig {

    @Bean
    HibernatePropertiesCustomizer jsonFormatMapperCustomizer(JsonMapper jsonMapper) {
        return properties ->
                properties.put(MappingSettings.JSON_FORMAT_MAPPER, new Jackson3JsonFormatMapper(jsonMapper));
    }
}
