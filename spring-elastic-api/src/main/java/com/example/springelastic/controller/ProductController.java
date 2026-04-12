package com.example.springelastic.controller;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.springelastic.model.Product;
import com.example.springelastic.service.ProductService;
import com.example.springelastic.util.StringHelper;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "CRUD and category search for products in Elasticsearch")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "List all products")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "All products (empty if index does not exist yet)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class))))
    })
    @GetMapping
    public List<Product> listProducts() {
        log.info("Action: list all products");
        return productService.findAllProducts();
    }

    @Operation(summary = "Find products by category id(s)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Products whose categoryIds contain any of the given ids",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Product.class)))),
            @ApiResponse(responseCode = "400", description = "No category ids provided")
    })
    @GetMapping("/by-category")
    public List<Product> findByCategory(
            @Parameter(
                    description = "Comma-separated category ids (match if product lists any of them)",
                    example = "electronics,books")
            @RequestParam String categoryIds) {
        List<String> ids =
                Arrays.stream(categoryIds.split(",")).map(StringHelper::trimToNull).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            log.warn("Action: find by category rejected, empty categoryIds");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryIds must contain at least one id");
        }
        log.info("Action: find products by category, idCount={}", ids.size());
        return productService.findProductsByCategoryIds(ids);
    }

    @Operation(
            summary = "Map categories to product ids",
            description =
                    "Returns one entry per requested category id (map keys sorted alphabetically). Each value is a sorted "
                            + "distinct set of product ids whose categoryIds contain that category. Uses the same Elasticsearch "
                            + "query as find-by-category, then groups in memory. Request body is a JSON array of category id "
                            + "strings (max 10 entries).")
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
        log.info("Action: category to product ids, rawCount={}", categoryIds.size());
        return productService.mapCategoryToProductIds(categoryIds);
    }

    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Found",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        log.info("Action: get product by id, id={}", id);
        return productService.findProductById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create or update a product (body id optional; generated if absent)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created (no id in body)",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated or saved with client id",
                    content = @Content(schema = @Schema(implementation = Product.class)))
    })
    @PostMapping
    public ResponseEntity<Product> saveProduct(@RequestBody Product product) {
        boolean create = StringHelper.trimToNull(product.getId()) == null;
        log.info("Action: save product, create={}, incomingId={}", create, product.getId());
        Product saved = productService.saveProduct(product);
        return ResponseEntity.status(create ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
    }

    @Operation(summary = "Delete a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Delete attempted (no-op if index or id missing)")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        log.info("Action: delete product, id={}", id);
        productService.deleteProductById(id);
        return ResponseEntity.noContent().build();
    }
}
