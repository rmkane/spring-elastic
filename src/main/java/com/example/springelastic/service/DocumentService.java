package com.example.springelastic.service;

import com.example.springelastic.model.DocumentModel;
import com.example.springelastic.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    
    public DocumentModel saveDocument(MultipartFile file) throws IOException {
        String content = new String(file.getBytes());
        
        DocumentModel document = new DocumentModel();
        document.setId(UUID.randomUUID().toString());
        document.setFileName(file.getOriginalFilename());
        document.setContent(content);
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setUploadedAt(LocalDateTime.now());
        
        // Index name is determined dynamically via SpEL in @Document annotation
        return documentRepository.save(document);
    }
}

