package org.acme.elastic.consumer.service.impl;

import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.acme.elastic.consumer.cache.ConsumerCacheNames;
import org.acme.elastic.consumer.client.ElasticUpstreamRestClient;
import org.acme.elastic.consumer.dto.DocumentJson;
import org.acme.elastic.consumer.service.DocumentService;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final ElasticUpstreamRestClient rest;

    private DocumentServiceImpl self;

    @Autowired
    @Lazy
    void setSelf(DocumentServiceImpl self) {
        this.self = self;
    }

    @Override
    @Cacheable(cacheNames = ConsumerCacheNames.DOCUMENTS_LIST)
    public List<DocumentJson> listDocuments() {
        return rest.listDocuments();
    }

    @Override
    @Cacheable(cacheNames = ConsumerCacheNames.DOCUMENTS_BY_IDS, key = "#ids")
    public List<DocumentJson> getDocumentsByIds(String ids) {
        return rest.getDocumentsByIds(ids);
    }

    @Override
    @Cacheable(
            cacheNames = ConsumerCacheNames.DOCUMENTS_SEARCH,
            key = "T(org.acme.elastic.consumer.cache.ConsumerCacheKeys).searchDocuments(#fileName, #contentType)")
    public List<DocumentJson> searchDocuments(String fileName, String contentType) {
        return rest.searchDocuments(fileName, contentType);
    }

    @Override
    public ResponseEntity<DocumentJson> getDocument(String id) {
        DocumentJson body = self.getDocumentBodyOrNull(id);
        return body != null ? ResponseEntity.ok(body) : ResponseEntity.notFound().build();
    }

    @Cacheable(cacheNames = ConsumerCacheNames.DOCUMENT_BY_ID, key = "#id", unless = "#result == null")
    public DocumentJson getDocumentBodyOrNull(String id) {
        ResponseEntity<DocumentJson> r = rest.getDocument(id);
        if (r.getStatusCode() != HttpStatus.OK || r.getBody() == null) {
            return null;
        }
        return r.getBody();
    }

    @Override
    @Caching(
            evict = {
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENT_BY_ID, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENTS_LIST, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENTS_BY_IDS, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENTS_SEARCH, allEntries = true)
            })
    public ResponseEntity<DocumentJson> uploadDocument(MultipartFile file) throws IOException {
        return rest.uploadDocument(file);
    }

    @Override
    @Caching(
            evict = {
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENT_BY_ID, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENTS_LIST, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENTS_BY_IDS, allEntries = true),
                @CacheEvict(cacheNames = ConsumerCacheNames.DOCUMENTS_SEARCH, allEntries = true)
            })
    public ResponseEntity<Void> purgeDocumentsIndex() {
        return rest.purgeDocumentsIndex();
    }
}
