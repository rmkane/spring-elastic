package com.example.springelastic.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.example.springelastic.model.Product;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {

    /** Products whose {@code categoryIds} contain at least one of the given values. */
    List<Product> findByCategoryIdsIn(Collection<String> categoryIds);
}
