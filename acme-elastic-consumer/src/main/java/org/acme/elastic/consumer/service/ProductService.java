package org.acme.elastic.consumer.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import org.acme.elastic.consumer.dto.ProductJson;

public interface ProductService {

    List<ProductJson> listProducts();

    List<ProductJson> findByCategory(String categoryIdsQuery);

    Map<String, Set<String>> categoryToProductIds(List<String> categoryIds);

    ResponseEntity<ProductJson> getProduct(String id);

    ResponseEntity<ProductJson> saveProduct(ProductJson product);

    ResponseEntity<Void> deleteProduct(String id);
}
