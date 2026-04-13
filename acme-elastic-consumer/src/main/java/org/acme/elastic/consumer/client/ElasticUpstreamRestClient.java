package org.acme.elastic.consumer.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import org.acme.elastic.consumer.dto.DocumentJson;
import org.acme.elastic.consumer.dto.ProductJson;

/** Raw HTTP to the Elasticsearch API (no cache). Each call is logged once here. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticUpstreamRestClient {

    private final RestClient elasticApiRestClient;

    public List<ProductJson> listProducts() {
        log.info("Upstream HTTP: GET /api/products");
        return elasticApiRestClient
                .get()
                .uri("/api/products")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductJson>>() {});
    }

    public List<ProductJson> findByCategory(String categoryIdsQuery) {
        log.info("Upstream HTTP: GET /api/products/by-category");
        return elasticApiRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/products/by-category")
                        .queryParam("categoryIds", categoryIdsQuery)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductJson>>() {});
    }

    public Map<String, Set<String>> categoryToProductIds(List<String> categoryIds) {
        log.info("Upstream HTTP: POST /api/products/category-product-ids");
        return elasticApiRestClient
                .post()
                .uri("/api/products/category-product-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .body(categoryIds)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Set<String>>>() {});
    }

    public ResponseEntity<ProductJson> getProduct(String id) {
        log.info("Upstream HTTP: GET /api/products/{}", id);
        return elasticApiRestClient
                .get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .toEntity(ProductJson.class);
    }

    public ResponseEntity<ProductJson> saveProduct(ProductJson product) {
        log.info("Upstream HTTP: POST /api/products");
        return elasticApiRestClient
                .post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(product)
                .retrieve()
                .toEntity(ProductJson.class);
    }

    public ResponseEntity<Void> deleteProduct(String id) {
        log.info("Upstream HTTP: DELETE /api/products/{}", id);
        return elasticApiRestClient.delete().uri("/api/products/{id}", id).retrieve().toBodilessEntity();
    }

    public List<DocumentJson> listDocuments() {
        log.info("Upstream HTTP: GET /api/documents");
        return elasticApiRestClient
                .get()
                .uri("/api/documents")
                .retrieve()
                .body(new ParameterizedTypeReference<List<DocumentJson>>() {});
    }

    public List<DocumentJson> getDocumentsByIds(String ids) {
        log.info("Upstream HTTP: GET /api/documents/by-ids");
        return elasticApiRestClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/documents/by-ids").queryParam("ids", ids).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<DocumentJson>>() {});
    }

    public List<DocumentJson> searchDocuments(String fileName, String contentType) {
        log.info("Upstream HTTP: GET /api/documents/search");
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/api/documents/search");
        if (fileName != null) {
            b.queryParam("fileName", fileName);
        }
        if (contentType != null) {
            b.queryParam("contentType", contentType);
        }
        return elasticApiRestClient
                .get()
                .uri(b.build().encode().toUriString())
                .retrieve()
                .body(new ParameterizedTypeReference<List<DocumentJson>>() {});
    }

    public ResponseEntity<Void> purgeDocumentsIndex() {
        log.info("Upstream HTTP: DELETE /api/documents/index");
        return elasticApiRestClient.delete().uri("/api/documents/index").retrieve().toBodilessEntity();
    }

    public ResponseEntity<DocumentJson> getDocument(String id) {
        log.info("Upstream HTTP: GET /api/documents/{}", id);
        return elasticApiRestClient
                .get()
                .uri("/api/documents/{id}", id)
                .retrieve()
                .toEntity(DocumentJson.class);
    }

    public ResponseEntity<DocumentJson> uploadDocument(MultipartFile file) throws IOException {
        log.info("Upstream HTTP: POST /api/documents/upload");
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
            }
        };
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", resource);
        return elasticApiRestClient
                .post()
                .uri("/api/documents/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .toEntity(DocumentJson.class);
    }
}
