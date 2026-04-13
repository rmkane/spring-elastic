package org.acme.elastic.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import org.acme.elastic.model.Product;
import org.acme.elastic.repository.ProductRepository;
import org.acme.elastic.util.StringHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public List<Product> findAllProducts() {
        if (!productsIndexExists()) {
            log.info("Service: find all products skipped, index missing");
            return List.of();
        }
        // Do not sort by "id" in the ES query: many indices lack a doc_values mapping for id, which
        // causes search_phase_execution_exception. Sort in memory instead.
        List<Product> list =
                StreamSupport.stream(productRepository.findAll().spliterator(), false)
                        .sorted(Comparator.comparing(Product::getId, Comparator.nullsLast(String::compareTo)))
                        .toList();
        log.info("Service: find all products, count={}", list.size());
        return list;
    }

    public Optional<Product> findProductById(String id) {
        if (!productsIndexExists()) {
            log.info("Service: find product by id skipped, index missing, id={}", id);
            return Optional.empty();
        }
        Optional<Product> result = productRepository.findById(id);
        log.info("Service: find product by id {}, id={}", result.isPresent() ? "hit" : "miss", id);
        return result;
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            product.setId(UUID.randomUUID().toString());
        }
        if (product.getCategoryIds() == null) {
            product.setCategoryIds(new ArrayList<>());
        }
        log.info(
                "Service: save product, id={}, name={}, categoryCount={}",
                product.getId(),
                product.getName(),
                product.getCategoryIds().size());
        Product saved = productRepository.save(product);
        log.info("Service: product indexed, id={}", saved.getId());
        return saved;
    }

    public void deleteProductById(String id) {
        if (!productsIndexExists()) {
            log.info("Service: delete product skipped, index missing, id={}", id);
            return;
        }
        productRepository.deleteById(id);
        log.info("Service: delete product, id={}", id);
    }

    public List<Product> findProductsByCategoryIds(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        if (!productsIndexExists()) {
            log.info("Service: find products by category skipped, index missing");
            return List.of();
        }
        log.info("Service: find products by categoryIds, requestedCount={}", categoryIds.size());
        List<Product> found = productRepository.findByCategoryIdsIn(categoryIds);
        log.info("Service: find by category complete, matchCount={}", found.size());
        return found;
    }

    /**
     * For each requested category id, distinct product ids that reference that category. Map keys are sorted
     * ({@link TreeMap}, natural {@link String} order after trim/dedupe); values are {@link TreeSet}s.
     */
    public Map<String, Set<String>> mapCategoryToProductIds(List<String> categoryIds) {
        List<String> requested =
                categoryIds.stream()
                        .map(StringHelper::trimToNull)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (requested.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> byCategory =
                requested.stream()
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        c -> new TreeSet<>(),
                                        (a, b) -> a,
                                        TreeMap::new));
        if (!productsIndexExists()) {
            log.info("Service: map category to product ids skipped, index missing");
            return byCategory;
        }
        List<Product> products = productRepository.findByCategoryIdsIn(requested);
        log.info(
                "Service: map category to product ids, categoryCount={}, productMatchCount={}",
                requested.size(),
                products.size());
        products.stream()
                .filter(p -> p.getId() != null && p.getCategoryIds() != null)
                .forEach(
                        p -> {
                            String pid = p.getId();
                            p.getCategoryIds().stream()
                                    .map(byCategory::get)
                                    .filter(Objects::nonNull)
                                    .forEach(bucket -> bucket.add(pid));
                        });
        return byCategory;
    }

    private boolean productsIndexExists() {
        return elasticsearchOperations.indexOps(Product.class).exists();
    }
}
