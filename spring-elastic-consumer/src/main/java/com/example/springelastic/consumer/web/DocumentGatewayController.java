package com.example.springelastic.consumer.web;

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

import com.example.springelastic.consumer.dto.DocumentJson;
import com.example.springelastic.consumer.service.DocumentService;
import com.example.springelastic.consumer.util.StringHelper;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(
        name = "Documents",
        description = "Proxied to the upstream Elasticsearch API (same paths and behavior)")
public class DocumentGatewayController {

    private final DocumentService documentService;

    @Operation(summary = "List documents in the current weekly index")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documents in the index for the current week",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentJson.class))))
    })
    @GetMapping
    public List<DocumentJson> listDocuments() {
        log.info("Consumer gateway: list documents");
        return documentService.listDocuments();
    }

    @Operation(summary = "Get documents by id (comma-separated)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Documents found (omits unknown ids)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentJson.class))))
    })
    @GetMapping("/by-ids")
    public List<DocumentJson> getDocumentsByIds(
            @Parameter(description = "Comma-separated Elasticsearch document ids", example = "uuid1,uuid2")
            @RequestParam String ids) {
        List<String> idList = Arrays.stream(ids.split(","))
                .map(StringHelper::trimToNull)
                .filter(Objects::nonNull)
                .toList();
        if (idList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids must contain at least one id");
        }
        log.info("Consumer gateway: get documents by ids, requestedCount={}", idList.size());
        return documentService.getDocumentsByIds(ids);
    }

    @Operation(summary = "Search by file name and/or content type (substring match on analyzed text fields)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Matches in the current weekly index",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentJson.class)))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Both fileName and contentType missing or blank (provide at least one)")
    })
    @GetMapping("/search")
    public List<DocumentJson> search(
            @Parameter(description = "Substring of the original file name", example = "sample")
            @RequestParam(required = false) String fileName,
            @Parameter(description = "Substring of Content-Type (e.g. text or json)", example = "text/plain")
            @RequestParam(required = false) String contentType) {
        String nameFragment = StringHelper.trimToNull(fileName);
        String typeFragment = StringHelper.trimToNull(contentType);
        if (nameFragment == null && typeFragment == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Provide fileName and/or contentType query parameter");
        }
        log.info(
                "Consumer gateway: search documents, fileNameFragment={}, contentTypeFragment={}",
                nameFragment,
                typeFragment);
        return documentService.searchDocuments(fileName, contentType);
    }

    @Operation(summary = "Get a document by id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Found",
                    content = @Content(schema = @Schema(implementation = DocumentJson.class))),
            @ApiResponse(responseCode = "404", description = "Not found in the current weekly index")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentJson> getDocument(@PathVariable String id) {
        log.info("Consumer gateway: get document, id={}", id);
        return documentService.getDocument(id);
    }

    @Operation(summary = "Upload a file")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Indexed in Elasticsearch (upstream)",
                    content = @Content(schema = @Schema(implementation = DocumentJson.class))),
            @ApiResponse(responseCode = "400", description = "Empty file"),
            @ApiResponse(responseCode = "500", description = "Failed to read or index file (upstream)")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentJson> uploadDocument(
            @Parameter(
                    description = "File to store",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info(
                "Consumer gateway: upload document, originalFilename={}, size={}",
                file.getOriginalFilename(),
                file.getSize());
        return documentService.uploadDocument(file);
    }

    @Operation(
            summary = "Purge the current weekly index",
            description =
                    "Deletes the entire Elasticsearch index for documents for the current week upstream. "
                            + "Other weekly indices are not affected.")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Index deleted or already absent"))
    @DeleteMapping("/index")
    public ResponseEntity<Void> purgeCurrentWeeklyIndex() {
        log.info("Consumer gateway: purge documents index");
        return documentService.purgeDocumentsIndex();
    }
}
