package com.example.springelastic.repository;

import com.example.springelastic.model.DocumentModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<DocumentModel, String> {
}

