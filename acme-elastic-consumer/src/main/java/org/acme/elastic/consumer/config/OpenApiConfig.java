package org.acme.elastic.consumer.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final Comparator<String> PATH_KEY_ORDER = Comparator.comparingInt(OpenApiConfig::pathGroup)
            .thenComparingInt(OpenApiConfig::pathRankInGroup)
            .thenComparing(Comparator.naturalOrder());

    @Bean
    public OpenApiCustomizer sortOpenApiPaths() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null || paths.isEmpty()) {
                return;
            }
            List<String> keys = new ArrayList<>(paths.keySet());
            keys.sort(PATH_KEY_ORDER);
            Paths reordered = new Paths();
            for (String key : keys) {
                reordered.put(key, paths.get(key));
            }
            openApi.setPaths(reordered);
        };
    }

    private static int pathGroup(String path) {
        if (path.startsWith("/api/documents")) {
            return 0;
        }
        if (path.startsWith("/api/products")) {
            return 1;
        }
        return 2;
    }

    private static int pathRankInGroup(String path) {
        if (path.startsWith("/api/documents")) {
            return switch (path) {
            case "/api/documents" -> 0;
            case "/api/documents/by-ids" -> 1;
            case "/api/documents/search" -> 2;
            case "/api/documents/{id}" -> 3;
            case "/api/documents/upload" -> 4;
            case "/api/documents/index" -> 5;
            default -> 50;
            };
        }
        if (path.startsWith("/api/products")) {
            return switch (path) {
            case "/api/products" -> 0;
            case "/api/products/by-category" -> 1;
            case "/api/products/category-product-ids" -> 2;
            case "/api/products/{id}" -> 3;
            default -> 50;
            };
        }
        return 0;
    }

    @Bean
    public OpenAPI consumerGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Elastic Consumer (gateway)")
                        .description(
                                "Same routes as the Elasticsearch API, proxied to the upstream service "
                                        + "(elastic-api.base-url, default http://localhost:8885). "
                                        + "Start the API before using Try it out.")
                        .version("0.1.0-SNAPSHOT")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server().url("/").description("This gateway (see server.port)"));
    }
}
