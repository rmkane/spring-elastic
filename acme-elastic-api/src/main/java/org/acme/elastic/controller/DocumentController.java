package org.acme.elastic.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.acme.elastic.model.DocumentModel;
import org.acme.elastic.service.DocumentService;
import org.acme.elastic.util.StringHelper;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload and retrieve documents indexed in Elasticsearch")
public class DocumentController {
    
    private final DocumentService documentService;
    
    @Operation(summary = "List documents in the current weekly index")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documents in the index for the current week",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentModel.class))))
    })
    @GetMapping
    public List<DocumentModel> listDocuments() {
        log.info("Action: list all documents in current weekly index");
        return documentService.findAllDocuments();
    }
    
    @Operation(summary = "Get documents by id (comma-separated)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documents found (omits unknown ids)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentModel.class))))
    })
    @GetMapping("/by-ids")
    public List<DocumentModel> getDocumentsByIds(
            @Parameter(description = "Comma-separated Elasticsearch document ids", example = "uuid1,uuid2")
            @RequestParam String ids) {
        List<String> idList = Arrays.stream(ids.split(","))
                .map(StringHelper::trimToNull)
                .filter(Objects::nonNull)
                .toList();
        if (idList.isEmpty()) {
            log.warn("Action: get by ids rejected, empty id list");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids must contain at least one id");
        }
        log.info("Action: get documents by ids, requestedCount={}", idList.size());
        return documentService.findDocumentsByIds(idList);
    }
    
    @Operation(summary = "Search by file name and/or content type (substring match on analyzed text fields)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Matches in the current weekly index",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentModel.class)))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Both fileName and contentType missing or blank (provide at least one)")
    })
    @GetMapping("/search")
    public List<DocumentModel> search(
            @Parameter(description = "Substring of the original file name", example = "sample")
            @RequestParam(required = false) String fileName,
            @Parameter(description = "Substring of Content-Type (e.g. text or json)", example = "text/plain")
            @RequestParam(required = false) String contentType) {
        String nameFragment = StringHelper.trimToNull(fileName);
        String typeFragment = StringHelper.trimToNull(contentType);
        if (nameFragment == null && typeFragment == null) {
            log.warn("Action: search rejected, missing fileName and contentType");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Provide fileName and/or contentType query parameter");
        }
        log.info(
                "Action: search documents, fileNameFragment={}, contentTypeFragment={}",
                nameFragment,
                typeFragment);
        return documentService.search(nameFragment, typeFragment);
    }

    @Operation(summary = "Get a document by id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Found",
                    content = @Content(schema = @Schema(implementation = DocumentModel.class))),
            @ApiResponse(responseCode = "404", description = "Not found in the current weekly index")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentModel> getDocument(@PathVariable String id) {
        log.info("Action: get document by id, id={}", id);
        return documentService.findDocumentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Upload a file")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Indexed in Elasticsearch",
                    content = @Content(schema = @Schema(implementation = DocumentModel.class))),
            @ApiResponse(responseCode = "400", description = "Empty file"),
            @ApiResponse(responseCode = "500", description = "Failed to read or index file")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentModel> uploadDocument(
            @Parameter(
                    description = "File to store",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("Action: upload rejected, empty file");
            return ResponseEntity.badRequest().build();
        }

        log.info(
                "Action: upload document, originalFilename={}, size={}, contentType={}",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType());
        try {
            DocumentModel savedDocument = documentService.saveDocument(file);
            log.info("Action: upload complete, documentId={}", savedDocument.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDocument);
        } catch (IOException e) {
            log.error("Action: upload failed reading multipart file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Purge the current weekly index",
            description =
                    "Deletes the entire Elasticsearch index for DocumentModel for the current week (SpEL name). "
                            + "Other weekly indices are not affected. Idempotent if the index does not exist.")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Index deleted or already absent"))
    @DeleteMapping("/index")
    public ResponseEntity<Void> purgeCurrentWeeklyIndex() {
        log.info("Action: purge current weekly index requested");
        documentService.purgeCurrentWeeklyIndex();
        log.info("Action: purge current weekly index finished");
        return ResponseEntity.noContent().build();
    }
}

