package com.matthewbixby.allergen.intelligence.controller;

import com.matthewbixby.allergen.intelligence.model.ChemicalIdentification;
import com.matthewbixby.allergen.intelligence.model.SideEffect;
import com.matthewbixby.allergen.intelligence.repository.ChemicalRepository;
import com.matthewbixby.allergen.intelligence.repository.SideEffectRepository;
import com.matthewbixby.allergen.intelligence.service.OpenAISearchService;
import com.matthewbixby.allergen.intelligence.service.PubChemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/allergen")
@RequiredArgsConstructor
@Slf4j
public class AllergenSearchController {

    private final PubChemService pubChemService;
    private final OpenAISearchService openAISearchService;
    private final ChemicalRepository chemicalRepository;
    private final SideEffectRepository sideEffectRepository;

    /**
     * Complete allergen analysis pipeline with database persistence
     */
    @GetMapping("/analyze/{ingredientName}")
    public ResponseEntity<Map<String, Object>> analyzeAllergen(@PathVariable String ingredientName) {
        try {
            log.info("Starting allergen analysis for: {}", ingredientName);

            // Step 1: Get or create chemical identification
            ChemicalIdentification chemical = getOrCreateChemical(ingredientName);
            if (chemical == null) {
                return ResponseEntity.notFound().build();
            }

            // Step 2: Get side effects (database first, then OpenAI if needed)
            List<SideEffect> sideEffects = getOrCreateSideEffects(chemical);

            // Step 3: Get oxidation products (database first, then OpenAI if needed)
            List<String> oxidationProducts = getOrCreateOxidationProducts(chemical);

            // Build comprehensive response
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
    public ResponseEntity<Map<String, Object>> analyzeBatch(@RequestBody List<String> ingredients) {
        Map<String, Object> batchResults = new HashMap<>();

        try {
            for (String ingredient : ingredients) {
                log.info("Analyzing ingredient in batch: {}", ingredient);

                // Use the same database-first approach for batch processing
                ChemicalIdentification chemical = getOrCreateChemical(ingredient);
                if (chemical != null) {
                    List<SideEffect> sideEffects = getOrCreateSideEffects(chemical);
                    List<String> oxidationProducts = getOrCreateOxidationProducts(chemical);

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
     * Health check for search functionality
     */
    @GetMapping("/search/health")
    public ResponseEntity<Map<String, Object>> searchHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "operational");
        health.put("searchCapabilities", List.of("allergen_effects", "oxidation_products", "clinical_data"));
        health.put("disclaimer", getMedicalDisclaimer());
        return ResponseEntity.ok(health);
    }

    // Database-first helper methods
    private ChemicalIdentification getOrCreateChemical(String ingredientName) {
        // First check if we already have this chemical in the database
        Optional<ChemicalIdentification> existing = chemicalRepository.findByCommonNameIgnoreCase(ingredientName);
        if (existing.isPresent()) {
            log.info("Using existing chemical from database: {}", ingredientName);
            return existing.get();
        }

        // Not in database, get from PubChem
        ChemicalIdentification chemical = pubChemService.getChemicalData(ingredientName);
        if (chemical == null) {
            return null;
        }

        // Check if we have this PubChem CID already (different name, same compound)
        if (chemical.getPubchemCid() != null) {
            existing = chemicalRepository.findByPubchemCid(chemical.getPubchemCid());
            if (existing.isPresent()) {
                log.info("Found existing chemical by CID: {} for name: {}",
                        chemical.getPubchemCid(), ingredientName);
                return existing.get();
            }
        }

        // Save new chemical to database
        chemical = chemicalRepository.save(chemical);
        log.info("Saved new chemical to database: {} (CID: {})",
                chemical.getCommonName(), chemical.getPubchemCid());

        return chemical;
    }

    private List<SideEffect> getOrCreateSideEffects(ChemicalIdentification chemical) {
        // First check if we already have persisted side effects for this chemical
        List<SideEffect> existingSideEffects = sideEffectRepository.findByChemical_Id(chemical.getId());

        if (!existingSideEffects.isEmpty()) {
            log.info("Using existing {} side effects from database for: {}",
                    existingSideEffects.size(), chemical.getCommonName());
            return existingSideEffects;
        }

        // No persisted data, get from OpenAI (will use vector cache if available)
        log.info("No existing side effects found, searching via OpenAI for: {}", chemical.getCommonName());
        List<SideEffect> sideEffects = openAISearchService.searchAllergenEffects(chemical);

        // Persist the parsed results to database
        if (!sideEffects.isEmpty()) {
            // Ensure all side effects have the correct chemical reference
            sideEffects.forEach(effect -> effect.setChemical(chemical));
            sideEffects = sideEffectRepository.saveAll(sideEffects);
            log.info("Persisted {} side effects to database for: {}",
                    sideEffects.size(), chemical.getCommonName());
        } else {
            log.warn("No side effects found for: {}", chemical.getCommonName());
        }

        return sideEffects;
    }

    private List<String> getOrCreateOxidationProducts(ChemicalIdentification chemical) {
        // Check if we already have oxidation products for this chemical
        if (chemical.getOxidationProducts() != null && !chemical.getOxidationProducts().isEmpty()) {
            log.info("Using existing {} oxidation products from database for: {}",
                    chemical.getOxidationProducts().size(), chemical.getCommonName());
            return chemical.getOxidationProducts();
        }

        // No persisted oxidation products, get from OpenAI (will use vector cache if available)
        log.info("No existing oxidation products found, searching via OpenAI for: {}", chemical.getCommonName());
        List<String> oxidationProducts = openAISearchService.searchOxidationProducts(chemical);

        // Persist oxidation products to the chemical entity
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

    // Risk assessment and warning helper methods
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
                .anyMatch(effect -> effect.getSeverity().ordinal() >= 3); // SEVERE or LIFE_THREATENING

        if (hasSevere) return "HIGH";

        boolean hasModerate = sideEffects.stream()
                .anyMatch(effect -> effect.getSeverity().ordinal() >= 2); // MODERATE

        return hasModerate ? "MODERATE" : "LOW";
    }

    private List<String> generateWarnings(ChemicalIdentification chemical, List<SideEffect> sideEffects) {
        List<String> warnings = new ArrayList<>();

        // Check for oxidation products
        if (chemical.getOxidationProducts() != null && !chemical.getOxidationProducts().isEmpty()) {
            warnings.add("OXIDATION ALERT: This chemical forms allergenic oxidation products when exposed to air or light. " +
                    "The actual allergen may be different from the listed ingredient.");
        }

        // Check for severe reactions
        boolean hasSevere = sideEffects.stream()
                .anyMatch(effect -> effect.getSeverity().ordinal() >= 3);
        if (hasSevere) {
            warnings.add("SEVERE REACTION RISK: This chemical has been associated with severe allergic reactions. " +
                    "Seek immediate medical attention if symptoms occur.");
        }

        // Check for respiratory effects
        boolean hasRespiratory = sideEffects.stream()
                .anyMatch(effect -> effect.getAffectedBodyAreas().stream()
                        .anyMatch(area -> area.toLowerCase().contains("respiratory") ||
                                area.toLowerCase().contains("lung")));
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

    private String getMedicalDisclaimer() {
        return """
                MEDICAL DISCLAIMER: This information is for educational and research purposes only. 
                It does not constitute medical advice, diagnosis, or treatment recommendations. 
                Always consult qualified healthcare professionals for medical decisions. 
                Individual reactions may vary. In case of allergic reactions, seek immediate medical attention.
                """;
    }
}