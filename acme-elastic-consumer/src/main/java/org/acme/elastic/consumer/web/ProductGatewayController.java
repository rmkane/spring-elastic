package org.acme.elastic.consumer.web;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.acme.elastic.consumer.dto.ProductJson;
import org.acme.elastic.consumer.service.ProductService;
import org.acme.elastic.consumer.util.StringHelper;

@Slf4j
@RestController
@RequestMapping("/api/products")
@Validated
@RequiredArgsConstructor
@Tag(name = "Products", description = "Proxied to the upstream Elasticsearch API (same paths and behavior)")
public class ProductGatewayController {

    private final ProductService productService;

    @Operation(summary = "List all products")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "All products (empty if index does not exist yet)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductJson.class))))
    })
    @GetMapping
    public List<ProductJson> listProducts() {
        log.info("Consumer gateway: list products (upstream)");
        return productService.listProducts();
    }

    @Operation(summary = "Find products by category id(s)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Products whose categoryIds contain any of the given ids",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductJson.class)))),
            @ApiResponse(responseCode = "400", description = "No category ids provided")
    })
    @GetMapping("/by-category")
    public List<ProductJson> findByCategory(
            @Parameter(
                    description = "Comma-separated category ids (match if product lists any of them)",
                    example = "electronics,books")
            @RequestParam String categoryIds) {
        List<String> ids =
                Arrays.stream(categoryIds.split(",")).map(StringHelper::trimToNull).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryIds must contain at least one id");
        }
        log.info("Consumer gateway: find by category, idCount={}", ids.size());
        return productService.findByCategory(categoryIds);
    }

    @Operation(
            summary = "Map categories to product ids",
            description =
                    "Returns one entry per requested category id (map keys sorted alphabetically). Each value is a sorted "
                            + "distinct set of product ids whose categoryIds contain that category. Proxied to the upstream API.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sorted category id → sorted distinct product ids (JSON arrays); empty sets if no match"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g. more than 10 ids)")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(type = "string"), maxItems = 10),
                            examples = @ExampleObject(value = "[\"electronics\",\"books\"]")))
    @PostMapping("/category-product-ids")
    public Map<String, Set<String>> categoryToProductIds(@RequestBody @Size(max = 10) List<String> categoryIds) {
        log.info("Consumer gateway: category to product ids, rawCount={}", categoryIds.size());
        return productService.categoryToProductIds(categoryIds);
    }

    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Found",
                    content = @Content(schema = @Schema(implementation = ProductJson.class))),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductJson> getProduct(@PathVariable String id) {
        log.info("Consumer gateway: get product, id={}", id);
        return productService.getProduct(id);
    }

    @Operation(summary = "Create or update a product (body id optional; generated if absent)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created (no id in body)",
                    content = @Content(schema = @Schema(implementation = ProductJson.class))),
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated or saved with client id",
                    content = @Content(schema = @Schema(implementation = ProductJson.class)))
    })
    @PostMapping
    public ResponseEntity<ProductJson> saveProduct(@RequestBody ProductJson product) {
        boolean create = StringHelper.trimToNull(product.id()) == null;
        log.info("Consumer gateway: save product, create={}, id={}", create, product.id());
        return productService.saveProduct(product);
    }

    @Operation(summary = "Delete a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Delete attempted (no-op if index or id missing)")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        log.info("Consumer gateway: delete product, id={}", id);
        return productService.deleteProduct(id);
    }
}
