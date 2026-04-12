package com.example.springelastic.consumer.cache;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/** Redis cache regions for the consumer gateway; TTL via {@code app.cache.ttl-by-name}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConsumerCacheNames {

    public static final String DOCUMENTS_LIST = "documentsList";
    public static final String DOCUMENTS_BY_IDS = "documentsByIds";
    public static final String DOCUMENTS_SEARCH = "documentsSearch";
    public static final String DOCUMENT_BY_ID = "documentById";

    public static final String PRODUCTS_LIST = "productsList";
    public static final String PRODUCTS_BY_CATEGORY = "productsByCategory";
    public static final String CATEGORY_PRODUCT_IDS = "categoryProductIds";
    public static final String PRODUCT_BY_ID = "productById";
}
