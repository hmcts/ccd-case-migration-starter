package uk.gov.hmcts.reform.migration.ccd;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class HttpMessageConverterConfiguration {
    
    @Bean
    public JacksonJsonHttpMessageConverter jacksonConverter(JsonMapper jsonMapper) {
        return new JacksonJsonHttpMessageConverter(jsonMapper);
    }
}
