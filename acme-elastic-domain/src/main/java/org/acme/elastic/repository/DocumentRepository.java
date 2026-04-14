package org.acme.elastic.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import org.acme.elastic.model.DocumentModel;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<DocumentModel, String> {
}
