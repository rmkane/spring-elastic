package org.acme.elastic.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient elasticApiRestClient(RestClient.Builder restClientBuilder, ElasticApiProperties props) {
        String base = props.baseUrl().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return restClientBuilder.baseUrl(base).build();
    }
}
