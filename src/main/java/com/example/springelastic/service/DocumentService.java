package com.example.springelastic.service;

import com.example.springelastic.config.IndexNameProvider;
import com.example.springelastic.model.DocumentModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final ElasticsearchOperations elasticsearchOperations;
    private final IndexNameProvider indexNameProvider;
    
    public DocumentModel saveDocument(MultipartFile file) throws IOException {
        String content = new String(file.getBytes());
        
        DocumentModel document = new DocumentModel();
        document.setId(UUID.randomUUID().toString());
        document.setFileName(file.getOriginalFilename());
        document.setContent(content);
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setUploadedAt(LocalDateTime.now());
        
        String indexName = indexNameProvider.getIndexName();
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        
        return elasticsearchOperations.save(document, indexCoordinates);
    }
}

