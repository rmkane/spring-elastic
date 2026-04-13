package org.acme.elastic.consumer.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Product as returned from the upstream API")
public record ProductJson(String id, String name, String description, Double price, List<String> categoryIds)
        implements Serializable {

    private static final long serialVersionUID = 1L;
}
