package com.matthewbixby.allergen.intelligence.controller;

import com.matthewbixby.allergen.intelligence.dto.IngredientAnalysis;
import com.matthewbixby.allergen.intelligence.dto.ProductAnalysisRequest;
import com.matthewbixby.allergen.intelligence.dto.ProductAnalysisResponse;
import com.matthewbixby.allergen.intelligence.model.ChemicalIdentification;
import com.matthewbixby.allergen.intelligence.model.SideEffect;
import com.matthewbixby.allergen.intelligence.repository.ChemicalRepository;
import com.matthewbixby.allergen.intelligence.repository.SideEffectRepository;
import com.matthewbixby.allergen.intelligence.service.JwtService;
import com.matthewbixby.allergen.intelligence.service.OpenAISearchService;
import com.matthewbixby.allergen.intelligence.service.PubChemService;
import com.matthewbixby.allergen.intelligence.service.UsageTrackingService;
import com.matthewbixby.allergen.intelligence.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.WebSearchOptions;
import static org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize;

@RestController
@RequestMapping("/api/allergen")
@RequiredArgsConstructor
@Slf4j
public class AllergenSearchController {

    private final PubChemService pubChemService;
    private final OpenAISearchService openAISearchService;
    private final ChemicalRepository chemicalRepository;
    private final SideEffectRepository sideEffectRepository;
    private final JwtService jwtService;
    private final UsageTrackingService usageTrackingService;
    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;

    /**
     * Complete allergen analysis pipeline with database persistence and usage tracking
     */
    @GetMapping("/analyze/{ingredientName}")
    public ResponseEntity<Map<String, Object>> analyzeAllergen(
            @PathVariable String ingredientName,
            @RequestHeader("Authorization") String authHeader) {
        try {
            log.info("Starting allergen analysis for: {}", ingredientName);

            String token = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(token);

            ChemicalIdentification chemical = getOrCreateChemical(ingredientName);
            if (chemical == null) {
                return ResponseEntity.notFound().build();
            }

            List<SideEffect> sideEffects = getOrCreateSideEffects(chemical, userEmail);
            List<String> oxidationProducts = getOrCreateOxidationProducts(chemical, userEmail);

            Map<String, Object> analysis = new HashMap<>();
            analysis.put("chemical", chemical);
            analysis.put("sideEffects", sideEffects);
            analysis.put("oxidationProducts", oxidationProducts);
            analysis.put("riskAssessment", generateRiskAssessment(sideEffects));
            analysis.put("warnings", generateWarnings(chemical, sideEffects));
            analysis.put("disclaimer", getMedicalDisclaimer());

            log.info("Completed allergen analysis for: {} - found {} side effects, {} oxidation products",
                    ingredientName, sideEffects.size(), oxidationProducts.size());

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("Error analyzing allergen {}: {}", ingredientName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Search for side effects only (without full chemical lookup)
     */
    @PostMapping("/side-effects")
    public ResponseEntity<List<SideEffect>> searchSideEffects(@RequestBody ChemicalIdentification chemical) {
        try {
            List<SideEffect> sideEffects = openAISearchService.searchAllergenEffects(chemical);
            return ResponseEntity.ok(sideEffects);
        } catch (Exception e) {
            log.error("Error searching side effects for {}: {}", chemical.getCommonName(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search for oxidation products only
     */
    @PostMapping("/oxidation-products")
    public ResponseEntity<List<String>> searchOxidationProducts(@RequestBody ChemicalIdentification chemical) {
        try {
            List<String> products = openAISearchService.searchOxidationProducts(chemical);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error searching oxidation products for {}: {}", chemical.getCommonName(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Batch analysis for multiple ingredients
     */
    @PostMapping("/analyze-batch")
    public ResponseEntity<Map<String, Object>> analyzeBatch(
            @RequestBody List<String> ingredients,
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> batchResults = new HashMap<>();
        String userEmail = jwtService.extractUsername(authHeader.substring(7));

        try {
            for (String ingredient : ingredients) {
                log.info("Analyzing ingredient in batch: {}", ingredient);

                ChemicalIdentification chemical = getOrCreateChemical(ingredient);
                if (chemical != null) {
                    List<SideEffect> sideEffects = getOrCreateSideEffects(chemical, userEmail);
                    List<String> oxidationProducts = getOrCreateOxidationProducts(chemical, userEmail);

                    Map<String, Object> result = new HashMap<>();
                    result.put("chemical", chemical);
                    result.put("sideEffects", sideEffects);
                    result.put("oxidationProducts", oxidationProducts);
                    result.put("riskLevel", calculateRiskLevel(sideEffects));

                    batchResults.put(ingredient, result);
                } else {
                    batchResults.put(ingredient, Map.of("error", "Chemical not found"));
                }
            }

            batchResults.put("summary", generateBatchSummary(batchResults));
            batchResults.put("disclaimer", getMedicalDisclaimer());

            return ResponseEntity.ok(batchResults);

        } catch (Exception e) {
            log.error("Error in batch analysis: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Batch analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Analyze a complete product by name - searches for ingredient list and analyzes each
     */
    @PostMapping("/analyze-product")
    public ResponseEntity<ProductAnalysisResponse> analyzeProduct(
            @RequestBody ProductAnalysisRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String productName = request.getProductName();
        if (productName == null || productName.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ProductAnalysisResponse.builder()
                            .error("Product name is required")
                            .build());
        }

        try {
            log.info("Starting product analysis for: {}", productName);
            String userEmail = jwtService.extractUsername(authHeader.substring(7));

            // Step 1: Use OpenAI to find the ingredient list (ALWAYS uses tokens)
            List<String> ingredients = searchProductIngredients(productName, userEmail);

            if (ingredients.isEmpty()) {
                return ResponseEntity.ok(ProductAnalysisResponse.builder()
                        .productName(productName)
                        .error("Could not find ingredient list for this product. Try using the full product name with brand (e.g., 'CeraVe Moisturizing Cream')")
                        .totalIngredients(0)
                        .highRiskIngredients(0)
                        .overallRiskLevel("UNKNOWN")
                        .disclaimer(getMedicalDisclaimer())
                        .build());
            }

            // Step 2: Analyze each ingredient (uses cache if available!)
            Map<String, IngredientAnalysis> detailedAnalysis = new HashMap<>();
            int highRiskCount = 0;
            int totalAnalyzed = 0;

            for (String ingredient : ingredients) {
                log.info("Analyzing ingredient in product: {}", ingredient);

                ChemicalIdentification chemical = getOrCreateChemical(ingredient);
                if (chemical != null) {
                    // These will use THREE-TIER CACHE: Database → Vector → OpenAI
                    List<SideEffect> sideEffects = getOrCreateSideEffects(chemical, userEmail);
                    List<String> oxidationProducts = getOrCreateOxidationProducts(chemical, userEmail);

                    String riskLevel = calculateRiskLevel(sideEffects);
                    if ("HIGH".equals(riskLevel)) {
                        highRiskCount++;
                    }

                    IngredientAnalysis analysis = IngredientAnalysis.builder()
                            .chemical(chemical)
                            .sideEffects(sideEffects)
                            .oxidationProducts(oxidationProducts)
                            .riskLevel(riskLevel)
                            .build();

                    detailedAnalysis.put(ingredient, analysis);
                    totalAnalyzed++;
                } else {
                    detailedAnalysis.put(ingredient, IngredientAnalysis.builder()
                            .error("Chemical data not found")
                            .riskLevel("UNKNOWN")
                            .build());
                }
            }

            // Step 3: Build response
            ProductAnalysisResponse response = ProductAnalysisResponse.builder()
                    .productName(productName)
                    .totalIngredients(totalAnalyzed)
                    .highRiskIngredients(highRiskCount)
                    .overallRiskLevel(determineOverallRisk(highRiskCount, totalAnalyzed))
                    .ingredients(ingredients)
                    .detailedAnalysis(detailedAnalysis)
                    .recommendations(generateProductRecommendations(highRiskCount, totalAnalyzed))
                    .disclaimer(getMedicalDisclaimer())
                    .build();

            log.info("Completed product analysis for: {} - {} ingredients, {} high risk",
                    productName, totalAnalyzed, highRiskCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing product {}: {}", productName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ProductAnalysisResponse.builder()
                            .productName(productName)
                            .error("Product analysis failed: " + e.getMessage())
                            .disclaimer(getMedicalDisclaimer())
                            .build());
        }
    }

    /**
     * Health check for search functionality
     */
    @GetMapping("/search/health")
    public ResponseEntity<Map<String, Object>> searchHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "operational");
        health.put("searchCapabilities", List.of("allergen_effects", "oxidation_products", "clinical_data", "product_analysis"));
        health.put("disclaimer", getMedicalDisclaimer());
        return ResponseEntity.ok(health);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private ChemicalIdentification getOrCreateChemical(String ingredientName) {
        Optional<ChemicalIdentification> existing = chemicalRepository.findByCommonNameIgnoreCase(ingredientName);
        if (existing.isPresent()) {
            log.info("Using existing chemical from database: {}", ingredientName);
            return existing.get();
        }

        ChemicalIdentification chemical = pubChemService.getChemicalData(ingredientName);
        if (chemical == null) {
            return null;
        }

        if (chemical.getPubchemCid() != null) {
            existing = chemicalRepository.findByPubchemCid(chemical.getPubchemCid());
            if (existing.isPresent()) {
                log.info("Found existing chemical by CID: {} for name: {}",
                        chemical.getPubchemCid(), ingredientName);
                return existing.get();
            }
        }

        chemical = chemicalRepository.save(chemical);
        log.info("Saved new chemical to database: {} (CID: {})",
                chemical.getCommonName(), chemical.getPubchemCid());

        return chemical;
    }

    /**
     * THREE-TIER CACHE STRATEGY for Side Effects:
     * TIER 1: Database (fastest - milliseconds)
     * TIER 2: Vector Cache (fast - ~100ms, no tokens)
     * TIER 3: OpenAI API (slow - seconds, uses tokens)
     */
    private List<SideEffect> getOrCreateSideEffects(ChemicalIdentification chemical, String userEmail) {
        // TIER 1: Check database cache (fastest)
        List<SideEffect> existingSideEffects = sideEffectRepository.findByChemical_Id(chemical.getId());

        if (!existingSideEffects.isEmpty()) {
            log.info("✅ DATABASE CACHE HIT: Using existing {} side effects for: {} (NO TOKENS USED)",
                    existingSideEffects.size(), chemical.getCommonName());
            return existingSideEffects;
        }

        // TIER 2: Check vector cache (fast, no tokens)
        Optional<String> cachedResponse = vectorStoreService.getCachedAllergenEffects(chemical.getCommonName());
        if (cachedResponse.isPresent()) {
            try {
                log.info("✅ VECTOR CACHE HIT: Found cached allergen effects for: {} (NO TOKENS USED)",
                        chemical.getCommonName());

                // Parse the cached response using OpenAISearchService's parser
                List<SideEffect> sideEffects = openAISearchService.parseSideEffectsFromCache(
                        cachedResponse.get(), chemical);

                if (!sideEffects.isEmpty()) {
                    // Persist to database for even faster future lookups
                    sideEffects.forEach(effect -> effect.setChemical(chemical));
                    sideEffects = sideEffectRepository.saveAll(sideEffects);
                    log.info("Persisted {} side effects from vector cache to database for: {}",
                            sideEffects.size(), chemical.getCommonName());
                    return sideEffects;
                }
            } catch (Exception e) {
                log.warn("Failed to parse cached side effects for {}, will fetch fresh: {}",
                        chemical.getCommonName(), e.getMessage());
            }
        }

        // TIER 3: Cache miss - call OpenAI (slow, uses tokens)
        log.info("🔍 CACHE MISS: No cached data found, searching via OpenAI for: {} (TOKENS WILL BE USED)",
                chemical.getCommonName());

        String estimatedPrompt = String.format("Search allergen effects for %s", chemical.getCommonName());
        int estimatedPromptTokens = usageTrackingService.estimateTokens(estimatedPrompt);

        // OpenAISearchService will handle its own vector caching internally
        List<SideEffect> sideEffects = openAISearchService.searchAllergenEffects(chemical);

        String estimatedResponse = sideEffects.toString();
        int estimatedResponseTokens = usageTrackingService.estimateTokens(estimatedResponse);
        int totalTokens = estimatedPromptTokens + estimatedResponseTokens;

        usageTrackingService.trackAnalysis(userEmail, totalTokens);

        if (!sideEffects.isEmpty()) {
            sideEffects.forEach(effect -> effect.setChemical(chemical));
            sideEffects = sideEffectRepository.saveAll(sideEffects);
            log.info("Persisted {} side effects to database for: {}",
                    sideEffects.size(), chemical.getCommonName());
        } else {
            log.warn("No side effects found for: {}", chemical.getCommonName());
        }

        return sideEffects;
    }

    /**
     * THREE-TIER CACHE STRATEGY for Oxidation Products:
     * TIER 1: Database (fastest - milliseconds)
     * TIER 2: Vector Cache (fast - ~100ms, no tokens)
     * TIER 3: OpenAI API (slow - seconds, uses tokens)
     */
    private List<String> getOrCreateOxidationProducts(ChemicalIdentification chemical, String userEmail) {
        // TIER 1: Check database cache (fastest)
        if (chemical.getOxidationProducts() != null && !chemical.getOxidationProducts().isEmpty()) {
            log.info("✅ DATABASE CACHE HIT: Using existing {} oxidation products for: {} (NO TOKENS USED)",
                    chemical.getOxidationProducts().size(), chemical.getCommonName());
            return chemical.getOxidationProducts();
        }

        // TIER 2: Check vector cache (fast, no tokens)
        Optional<String> cachedResponse = vectorStoreService.getCachedOxidationProducts(chemical.getCommonName());
        if (cachedResponse.isPresent()) {
            try {
                log.info("✅ VECTOR CACHE HIT: Found cached oxidation products for: {} (NO TOKENS USED)",
                        chemical.getCommonName());

                // Parse the cached response using OpenAISearchService's parser
                List<String> oxidationProducts = openAISearchService.parseOxidationProductsFromCache(
                        cachedResponse.get());

                if (!oxidationProducts.isEmpty()) {
                    // Persist to database for even faster future lookups
                    chemical.setOxidationProducts(oxidationProducts);
                    chemical.setLastUpdated(LocalDateTime.now());
                    chemicalRepository.save(chemical);
                    log.info("Persisted {} oxidation products from vector cache to database for: {}",
                            oxidationProducts.size(), chemical.getCommonName());
                    return oxidationProducts;
                }
            } catch (Exception e) {
                log.warn("Failed to parse cached oxidation products for {}, will fetch fresh: {}",
                        chemical.getCommonName(), e.getMessage());
            }
        }

        // TIER 3: Cache miss - call OpenAI (slow, uses tokens)
        log.info("🔍 CACHE MISS: No cached data found, searching via OpenAI for: {} (TOKENS WILL BE USED)",
                chemical.getCommonName());

        String estimatedPrompt = String.format("Search oxidation products for %s", chemical.getCommonName());
        int estimatedPromptTokens = usageTrackingService.estimateTokens(estimatedPrompt);

        // OpenAISearchService will handle its own vector caching internally
        List<String> oxidationProducts = openAISearchService.searchOxidationProducts(chemical);

        String estimatedResponse = String.join(", ", oxidationProducts);
        int estimatedResponseTokens = usageTrackingService.estimateTokens(estimatedResponse);
        int totalTokens = estimatedPromptTokens + estimatedResponseTokens;

        usageTrackingService.trackAnalysis(userEmail, totalTokens);

        if (!oxidationProducts.isEmpty()) {
            chemical.setOxidationProducts(oxidationProducts);
            chemical.setLastUpdated(LocalDateTime.now());
            chemicalRepository.save(chemical);
            log.info("Persisted {} oxidation products to database for: {}",
                    oxidationProducts.size(), chemical.getCommonName());
        } else {
            log.warn("No oxidation products found for: {}", chemical.getCommonName());
        }

        return oxidationProducts;
    }

    private List<String> searchProductIngredients(String productName, String userEmail) {
        log.info("🔍 Searching for ingredient list of: {} (TOKENS WILL BE USED)", productName);

        try {
            WebSearchOptions webSearchOptions = new WebSearchOptions(
                    SearchContextSize.MEDIUM,
                    null
            );

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .webSearchOptions(webSearchOptions)
                    .build();

            String systemPrompt = """
                You are a product ingredient researcher. Find the COMPLETE ingredient list 
                for consumer products from official sources.
                
                Response Format:
                INGREDIENT: [exact chemical name]
                INGREDIENT: [exact chemical name]
                """;

            String userPrompt = String.format("""
                Find the complete ingredient list for: "%s"
                Use INCI names for cosmetics. List each ingredient starting with "INGREDIENT:"
                """, productName);

            String fullPrompt = systemPrompt + "\n\n" + userPrompt;
            int promptTokens = usageTrackingService.estimateTokens(fullPrompt);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(options)
                    .call()
                    .content();

            int responseTokens = usageTrackingService.estimateTokens(response);
            usageTrackingService.trackAnalysis(userEmail, promptTokens + responseTokens);

            return parseIngredientList(response);

        } catch (Exception e) {
            log.error("Error searching product ingredients for {}: {}", productName, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> parseIngredientList(String response) {
        List<String> ingredients = new ArrayList<>();

        try {
            String[] lines = response.split("\n");

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("INGREDIENT:")) {
                    String ingredient = line.substring("INGREDIENT:".length()).trim();
                    if (!ingredient.isEmpty()) {
                        ingredients.add(ingredient);
                    }
                } else if (line.matches("^\\d+\\.\\s+.*")) {
                    String ingredient = line.replaceFirst("^\\d+\\.\\s+", "").trim();
                    if (!ingredient.isEmpty()) {
                        ingredients.add(ingredient);
                    }
                }
            }

            log.info("Parsed {} ingredients from response", ingredients.size());

        } catch (Exception e) {
            log.error("Error parsing ingredient list: {}", e.getMessage());
        }

        return ingredients;
    }

    private Map<String, Object> generateRiskAssessment(List<SideEffect> sideEffects) {
        Map<String, Object> assessment = new HashMap<>();

        long severeReactions = sideEffects.stream()
                .mapToLong(effect -> effect.getSeverity().ordinal())
                .max()
                .orElse(0);

        double avgPrevalence = sideEffects.stream()
                .filter(effect -> effect.getPrevalenceRate() != null)
                .mapToDouble(SideEffect::getPrevalenceRate)
                .average()
                .orElse(0.0);

        assessment.put("maxSeverityLevel", severeReactions);
        assessment.put("averagePrevalence", avgPrevalence);
        assessment.put("totalReactionsFound", sideEffects.size());
        assessment.put("riskLevel", calculateRiskLevel(sideEffects));

        return assessment;
    }

    private String calculateRiskLevel(List<SideEffect> sideEffects) {
        if (sideEffects.isEmpty()) return "UNKNOWN";

        boolean hasSevere = sideEffects.stream()
                .anyMatch(effect -> effect.getSeverity().ordinal() >= 3);

        if (hasSevere) return "HIGH";

        boolean hasModerate = sideEffects.stream()
                .anyMatch(effect -> effect.getSeverity().ordinal() >= 2);

        return hasModerate ? "MODERATE" : "LOW";
    }

    private String determineOverallRisk(int highRiskCount, int totalAnalyzed) {
        if (highRiskCount > 0) {
            return "HIGH";
        } else if (totalAnalyzed > 5) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }

    private List<String> generateWarnings(ChemicalIdentification chemical, List<SideEffect> sideEffects) {
        List<String> warnings = new ArrayList<>();

        if (chemical.getOxidationProducts() != null && !chemical.getOxidationProducts().isEmpty()) {
            warnings.add("OXIDATION ALERT: This chemical forms allergenic oxidation products when exposed to air or light.");
        }

        boolean hasSevere = sideEffects.stream()
                .anyMatch(effect -> effect.getSeverity().ordinal() >= 3);
        if (hasSevere) {
            warnings.add("SEVERE REACTION RISK: This chemical has been associated with severe allergic reactions.");
        }

        boolean hasRespiratory = sideEffects.stream()
                .anyMatch(effect -> effect.getAffectedBodyAreas().stream()
                        .anyMatch(area -> area.toLowerCase().contains("respiratory")));
        if (hasRespiratory) {
            warnings.add("RESPIRATORY ALERT: May cause breathing difficulties or respiratory sensitization.");
        }

        return warnings;
    }

    private Map<String, Object> generateBatchSummary(Map<String, Object> results) {
        Map<String, Object> summary = new HashMap<>();

        int totalAnalyzed = (int) results.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("summary") && !entry.getKey().equals("disclaimer"))
                .count();

        long highRiskCount = results.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("summary") && !entry.getKey().equals("disclaimer"))
                .filter(entry -> {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) entry.getValue();
                        return "HIGH".equals(data.get("riskLevel"));
                    }
                    return false;
                })
                .count();

        summary.put("totalIngredients", totalAnalyzed);
        summary.put("highRiskIngredients", highRiskCount);
        summary.put("overallRiskLevel", highRiskCount > 0 ? "HIGH" : "LOW");

        return summary;
    }

    private List<String> generateProductRecommendations(int highRiskCount, int totalAnalyzed) {
        List<String> recommendations = new ArrayList<>();

        if (highRiskCount == 0) {
            recommendations.add("✓ No high-risk allergens detected in this formulation");
            recommendations.add("Perform patch test before first use if you have sensitive skin");
        } else {
            recommendations.add("⚠ This product contains " + highRiskCount + " high-risk allergen(s)");
            recommendations.add("Consult with a dermatologist before use if you have known allergies");
            recommendations.add("Perform a patch test on inner arm for 48 hours before facial application");
        }

        if (totalAnalyzed > 15) {
            recommendations.add("Complex formulation with many ingredients - monitor for reactions");
        }

        recommendations.add("Always check individual ingredient sensitivities before use");

        return recommendations;
    }

    private String getMedicalDisclaimer() {
        return """
                MEDICAL DISCLAIMER: This information is for educational and research purposes only. 
                It does not constitute medical advice, diagnosis, or treatment recommendations. 
                Always consult qualified healthcare professionals for medical decisions.
                """;
    }
}