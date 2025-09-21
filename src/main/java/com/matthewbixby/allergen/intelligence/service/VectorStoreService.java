package com.matthewbixby.allergen.intelligence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;

    @Value("${cache.chemical.ttl-days:30}")
    private int cacheTtlDays;

    @Value("${cache.chemical.similarity-threshold:0.98}")
    private double similarityThreshold;

    /**
     * Check if we already have cached PubChem compound data
     */
    public Optional<String> getCachedChemical(String chemicalName) {
        return getCachedData(chemicalName, "pubchem_compound");
    }

    /**
     * Check if we already have cached PubChem synonyms data
     */
    public Optional<String> getCachedSynonyms(Long cid) {
        return getCachedData("CID_" + cid, "pubchem_synonyms");
    }

    /**
     * Store PubChem compound response for future use
     */
    public void cacheChemicalData(String chemicalName, String jsonData) {
        cacheData(chemicalName, jsonData, "pubchem_compound");
    }

    /**
     * Store PubChem synonyms response for future use
     */
    public void cacheSynonymsData(Long cid, String jsonData) {
        cacheData("CID_" + cid, jsonData, "pubchem_synonyms");
    }

    /**
     *  method to retrieve cached data using metadata-based exact match instead of relying on vector similarity alone
     */
    private Optional<String> getCachedData(String key, String dataType) {
        try {
            String searchKey = dataType + ":" + key.toLowerCase().trim();

            // Use a very low similarity threshold since we're matching on metadata
            SearchRequest request = SearchRequest.builder()
                    .query(searchKey)
                    .topK(10)  // Get more results
                    .similarityThreshold(0.5)  // Much lower threshold
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            // Filter by exact metadata match
            for (Document doc : results) {
                String storedKey = (String) doc.getMetadata().get("cache_key");
                if (storedKey != null && storedKey.equals(searchKey)) {
                    // Found exact match!
                    log.info("Cache hit for: {} (type: {})", key, dataType);
                    String content = doc.getText();
                    int newlineIndex = content.indexOf('\n');
                    if (newlineIndex > 0) {
                        return Optional.of(content.substring(newlineIndex + 1));
                    }
                    return Optional.of(content);
                }
            }

            log.info("Cache miss for: {} (type: {})", key, dataType);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error retrieving cached data for: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Generic method to cache data with metadata
     */
    private void cacheData(String key, String jsonData, String dataType) {
        try {
            String cacheKey = dataType + ":" + key.toLowerCase().trim();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("cache_key", cacheKey);  // Store exact key for verification
            metadata.put("original_key", key);     // Store original for debugging
            metadata.put("cached_at", LocalDateTime.now().toString());
            metadata.put("type", dataType);
            metadata.put("ttl_days", cacheTtlDays);

            // Create document with the cache key as prefix for better exact matching
            String content = cacheKey + "\n" + jsonData;
            Document doc = new Document(content, metadata);

            vectorStore.add(List.of(doc));

            log.info("Cached data for: {} (type: {})", key, dataType);
        } catch (Exception e) {
            log.error("Error caching data for: {} (type: {})", key, dataType, e);
        }
    }

    /**
     * Check if cached data has exceeded TTL
     */
    private boolean isCacheExpired(String cachedAtStr) {
        try {
            LocalDateTime cachedAt = LocalDateTime.parse(cachedAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long daysSinceCached = ChronoUnit.DAYS.between(cachedAt, LocalDateTime.now());
            return daysSinceCached > cacheTtlDays;
        } catch (Exception e) {
            log.warn("Could not parse cache timestamp: {}", cachedAtStr, e);
            return true; // Treat as expired if we can't parse
        }
    }

    /**
     * Invalidate cache for a specific chemical
     */
    public void invalidateCache(String chemicalName) {
        // Note: VectorStore doesn't typically support deletion by query
        // This is a placeholder - implement based on your VectorStore capabilities
        log.info("Cache invalidation requested for: {}", chemicalName);
    }

    /**
     * Get cache statistics (optional utility method)
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("ttl_days", cacheTtlDays);
        stats.put("similarity_threshold", similarityThreshold);
        // Add more stats as needed
        return stats;
    }
}