package org.acme.elastic.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.elastic.model.DocumentModel;
import org.acme.elastic.repository.DocumentRepository;
import org.acme.elastic.util.ContentTypeHelper;
import org.acme.elastic.util.StringHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public DocumentModel saveDocument(MultipartFile file) throws IOException {
        String content = new String(file.getBytes());

        DocumentModel document = new DocumentModel();
        document.setId(UUID.randomUUID().toString());
        document.setFileName(file.getOriginalFilename());
        document.setContent(content);
        document.setFileSize(file.getSize());
        document.setContentType(ContentTypeHelper.resolveContentType(file));
        document.setUploadedAt(Instant.now());

        log.info(
                "Service: save document to Elasticsearch, id={}, fileName={}, uploadedAt={}",
                document.getId(),
                document.getFileName(),
                document.getUploadedAt());
        DocumentModel saved = documentRepository.save(document);
        log.info("Service: document indexed, id={}", saved.getId());
        return saved;
    }

    public List<DocumentModel> findAllDocuments() {
        log.info("Service: find all documents (sorted by uploadedAt desc)");
        if (!currentWeeklyIndexExists()) {
            log.info("Service: find all skipped, weekly index does not exist yet");
            return List.of();
        }
        List<DocumentModel> list = StreamSupport.stream(
                documentRepository
                        .findAll(Sort.by(Sort.Direction.DESC, "uploadedAt"))
                        .spliterator(),
                false)
                .toList();
        log.info("Service: find all complete, count={}", list.size());
        return list;
    }

    public Optional<DocumentModel> findDocumentById(String id) {
        if (!currentWeeklyIndexExists()) {
            log.info("Service: find by id skipped, index missing, id={}", id);
            return Optional.empty();
        }
        Optional<DocumentModel> result = documentRepository.findById(id);
        log.info("Service: find by id {}, id={}", result.isPresent() ? "hit" : "miss", id);
        return result;
    }

    public List<DocumentModel> findDocumentsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            log.info("Service: find by ids skipped, empty list");
            return List.of();
        }
        if (!currentWeeklyIndexExists()) {
            log.info("Service: find by ids skipped, index missing");
            return List.of();
        }
        log.info("Service: find documents by ids, requestedCount={}", ids.size());
        List<DocumentModel> found = StreamSupport.stream(documentRepository.findAllById(ids).spliterator(), false)
                .toList();
        log.info("Service: find by ids complete, foundCount={}", found.size());
        return found;
    }

    public List<DocumentModel> search(String fileNameFragment, String contentTypeFragment) {
        String name = StringHelper.trimToNull(fileNameFragment);
        String type = StringHelper.trimToNull(contentTypeFragment);
        boolean byName = name != null;
        boolean byType = type != null;
        if (!byName && !byType) {
            return List.of();
        }
        if (!currentWeeklyIndexExists()) {
            log.info("Service: search skipped, index missing");
            return List.of();
        }
        Criteria criteria;
        if (byName && byType) {
            log.info("Service: search by fileName and contentType, fileName={}, contentType={}", name, type);
            criteria = Criteria.where("fileName.keyword")
                    .contains(name)
                    .and(Criteria.where("contentType.keyword").contains(type));
        } else if (byName) {
            log.info("Service: search by fileName, fragment={}", name);
            criteria = Criteria.where("fileName.keyword").contains(name);
        } else {
            log.info("Service: search by contentType, fragment={}", type);
            criteria = Criteria.where("contentType.keyword").contains(type);
        }
        SearchHits<DocumentModel> hits = elasticsearchOperations.search(new CriteriaQuery(criteria),
                DocumentModel.class);
        List<DocumentModel> matches = hits.getSearchHits().stream().map(SearchHit::getContent).toList();
        log.info("Service: search complete, matchCount={}", matches.size());
        return matches;
    }

    public void purgeCurrentWeeklyIndex() {
        String indexName = elasticsearchOperations.getIndexCoordinatesFor(DocumentModel.class).getIndexName();
        IndexOperations indexOps = elasticsearchOperations.indexOps(DocumentModel.class);
        log.info("Service: purge index requested, indexName={}, exists={}", indexName, indexOps.exists());
        if (indexOps.exists()) {
            indexOps.delete();
            log.info("Service: index deleted, indexName={}", indexName);
        } else {
            log.info("Service: purge no-op, index absent, indexName={}", indexName);
        }
    }

    private boolean currentWeeklyIndexExists() {
        return elasticsearchOperations.indexOps(DocumentModel.class).exists();
    }
}
