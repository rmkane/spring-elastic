package com.example.springelastic.consumer.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.springelastic.consumer.cache.ConsumerCacheNames;
import com.example.springelastic.consumer.client.ElasticUpstreamRestClient;
import com.example.springelastic.consumer.dto.ProductJson;
import com.example.springelastic.consumer.service.ProductService;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ElasticUpstreamRestClient rest;

    private ProductServiceImpl self;

    @Autowired
    @Lazy
    void setSelf(ProductServiceImpl self) {
        this.self = self;
    }

    @Override
    @Cacheable(cacheNames = ConsumerCacheNames.PRODUCTS_LIST)
    public List<ProductJson> listProducts() {
        return rest.listProducts();
    }

    @Override
    @Cacheable(cacheNames = ConsumerCacheNames.PRODUCTS_BY_CATEGORY, key = "#categoryIdsQuery")
    public List<ProductJson> findByCategory(String categoryIdsQuery) {
        return rest.findByCategory(categoryIdsQuery);
    }

    @Override
    @Cacheable(
            cacheNames = ConsumerCacheNames.CATEGORY_PRODUCT_IDS,
            key = "T(com.example.springelastic.consumer.cache.ConsumerCacheKeys).categoryIds(#categoryIds)")
    public Map<String, Set<String>> categoryToProductIds(List<String> categoryIds) {
        return rest.categoryToProductIds(categoryIds);
    }

    @Override
    public ResponseEntity<ProductJson> getProduct(String id) {
        ProductJson body = self.getProductBodyOrNull(id);
        return body != null ? ResponseEntity.ok(body) : ResponseEntity.notFound().build();
    }

    @Cacheable(cacheNames = ConsumerCacheNames.PRODUCT_BY_ID, key = "#id", unless = "#result == null")
    public ProductJson getProductBodyOrNull(String id) {
        ResponseEntity<ProductJson> r = rest.getProduct(id);
        if (r.getStatusCode() != HttpStatus.OK || r.getBody() == null) {
            return null;
        }
        return r.getBody();
    }

    @Override
    @Caching(
            evict = {
                @CacheEvict(
                        cacheNames = ConsumerCacheNames.PRODUCT_BY_ID,
                        key = "#result.body.id",
                        condition =
                                "#result.statusCode.is2xxSuccessful() && #result.body != null && #result.body.id != null && !#result.body.id.isEmpty()"),
                @CacheEvict(cacheNames = ConsumerCacheNames.PRODUCTS_LIST, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.PRODUCTS_BY_CATEGORY, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.CATEGORY_PRODUCT_IDS, allEntries = true)
            })
    public ResponseEntity<ProductJson> saveProduct(ProductJson product) {
        return rest.saveProduct(product);
    }

    @Override
    @Caching(
            evict = {
                @CacheEvict(cacheNames = ConsumerCacheNames.PRODUCT_BY_ID, key = "#id"),
                @CacheEvict(cacheNames = ConsumerCacheNames.PRODUCTS_LIST, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.PRODUCTS_BY_CATEGORY, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.CATEGORY_PRODUCT_IDS, allEntries = true)
            })
    public ResponseEntity<Void> deleteProduct(String id) {
        return rest.deleteProduct(id);
    }
}
