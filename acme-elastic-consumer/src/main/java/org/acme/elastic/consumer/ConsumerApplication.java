package org.acme.elastic.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.acme.elastic.consumer.config.ElasticApiProperties;

@SpringBootApplication
@EnableConfigurationProperties(ElasticApiProperties.class)
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
