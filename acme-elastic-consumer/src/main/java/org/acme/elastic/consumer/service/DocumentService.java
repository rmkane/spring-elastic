package org.acme.elastic.consumer.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import org.acme.elastic.consumer.dto.DocumentJson;

public interface DocumentService {

    List<DocumentJson> listDocuments();

    List<DocumentJson> getDocumentsByIds(String ids);

    List<DocumentJson> searchDocuments(String fileName, String contentType);

    ResponseEntity<DocumentJson> getDocument(String id);

    ResponseEntity<DocumentJson> uploadDocument(MultipartFile file) throws IOException;

    ResponseEntity<Void> purgeDocumentsIndex();
}
