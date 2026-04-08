package com.example.springelastic.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.example.springelastic.model.DocumentModel;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<DocumentModel, String> {}

