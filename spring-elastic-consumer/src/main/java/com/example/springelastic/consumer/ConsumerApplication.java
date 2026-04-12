package com.example.springelastic.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.springelastic.consumer.config.ElasticApiProperties;

@SpringBootApplication
@EnableConfigurationProperties(ElasticApiProperties.class)
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
