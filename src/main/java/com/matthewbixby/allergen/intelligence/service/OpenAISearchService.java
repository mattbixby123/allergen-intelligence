package com.matthewbixby.allergen.intelligence.service;

import com.matthewbixby.allergen.intelligence.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.WebSearchOptions;
import static org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAISearchService {

    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;

//    public OpenAISearchService(VectorStoreService vectorStoreService) {
//        this.vectorStoreService = vectorStoreService;
//
//        String apiKey = System.getenv("OPENAI_API_KEY");
//
//        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
//                .model("gpt-4o-search-preview")
//                .build();
//
//        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
//                .openAiApi(OpenAiApi.builder().apiKey(apiKey).build())
//                .defaultOptions(chatOptions)
//                .build();
//
//        this.chatClient = ChatClient.builder(openAiChatModel).build();
//    }

    /**
     * Search for allergen side effects using OpenAI's web search capabilities
     */
    public List<SideEffect> searchAllergenEffects(ChemicalIdentification chemical) {
        // Check cache first
        Optional<String> cached = vectorStoreService.getCachedAllergenEffects(chemical.getCommonName());
        if (cached.isPresent()) {
            try {
                log.info("Using cached allergen effects for: {}", chemical.getCommonName());
                return parseSideEffectsResponse(cached.get(), chemical);
            } catch (Exception e) {
                log.warn("Failed to parse cached allergen effects, fetching fresh", e);
                // Continue to fetch fresh data if cache parsing fails
            }
        }

        log.info("Searching for allergen effects for: {}", chemical.getCommonName());

        try {
            // Configure web search with medium context for medical research
            WebSearchOptions webSearchOptions = new WebSearchOptions(
                    SearchContextSize.MEDIUM, // Need comprehensive medical sources
                    null
            );

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .webSearchOptions(webSearchOptions)
                    .build();

            String systemPrompt = createSystemPrompt();
            String userPrompt = createUserPrompt(chemical);

            String response;
            try {
                response = CompletableFuture.supplyAsync(() -> {
                    return chatClient.prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .options(options)
                            .call()
                            .content();
                }).get(45, TimeUnit.SECONDS); // ⏱️ Timeout after 45 seconds

                log.info("Raw OpenAI response for {}: {}", chemical.getCommonName(), response);


            } catch (TimeoutException e) {
                log.error("OpenAI request timed out after 45s for allergen effects of {}", chemical.getCommonName());
                return new ArrayList<>(); // EARLY RETURN
            } catch (InterruptedException | ExecutionException e) {
                log.error("OpenAI execution error for allergen effects of {}: {}", chemical.getCommonName(), e.getMessage());
                return new ArrayList<>(); // EARLY RETURN
            }
            // Cache the response for future use
            vectorStoreService.cacheAllergenEffects(chemical.getCommonName(), response);

            return parseSideEffectsResponse(response, chemical);

        } catch (Exception e) {
            log.error("Error searching for allergen effects for {}: {}", chemical.getCommonName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Search specifically for oxidation products and their allergenicity
     */
    public List<String> searchOxidationProducts(ChemicalIdentification chemical) {
        // Check cache first
        Optional<String> cached = vectorStoreService.getCachedOxidationProducts(chemical.getCommonName());
        if (cached.isPresent()) {
            try {
                log.info("Using cached oxidation products for: {}", chemical.getCommonName());
                return parseOxidationProducts(cached.get());
            } catch (Exception e) {
                log.warn("Failed to parse cached oxidation products, fetching fresh", e);
            }
        }

        log.info("Searching for oxidation products of: {}", chemical.getCommonName());

        try {
            WebSearchOptions webSearchOptions = new WebSearchOptions(
                    SearchContextSize.MEDIUM,
                    null
            );

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .webSearchOptions(webSearchOptions)
                    .build();

            String systemPrompt = """
                You are a chemical oxidation expert specializing in allergen formation.
                
                **CRITICAL REQUIREMENTS:**
                1. Search ONLY for peer-reviewed scientific sources
                2. Focus on oxidation products that cause allergic reactions
                3. Include IUPAC names and CAS numbers when available
                4. Verify information from multiple authoritative sources
                5. Exclude speculative or unverified claims
                
                **Response Format:**
                For each oxidation product found:
                PRODUCT: [exact chemical name]
                CAS: [CAS number if available]
                FORMED_BY: [oxidation mechanism]
                ALLERGENICITY: [confirmed/suspected/unknown]
                SOURCE: [research paper or database]
                """;

            String userPrompt = String.format("""
                Search for oxidation products of %s (CAS: %s, SMILES: %s) that are known allergens.
                
                **Search Focus:**
                - Air oxidation products (exposure to oxygen)
                - Light-induced oxidation (UV/visible light)
                - Heat-induced oxidation products
                - Products formed during storage or processing
                
                **Priority Sources:**
                - PubMed/NCBI research papers
                - Chemical safety databases (ECHA, EPA)
                - Peer-reviewed dermatology journals
                - Contact dermatitis research
                
                **Key Terms to Include:**
                - "oxidation products"
                - "allergenic potential"  
                - "contact sensitization"
                - "dermatitis"
                - "%s hydroperoxide"
                - "%s oxide"
                
                Only report oxidation products with documented evidence of allergenic properties.
                """,
                    chemical.getCommonName(),
                    chemical.getCasNumber() != null ? chemical.getCasNumber() : "unknown",
                    chemical.getSmiles() != null ? chemical.getSmiles() : "unknown",
                    chemical.getCommonName().toLowerCase(),
                    chemical.getCommonName().toLowerCase());

            String response;
            try {
                response = CompletableFuture.supplyAsync(() -> {
                    return chatClient.prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .options(options)
                            .call()
                            .content();
                }).get(45, TimeUnit.SECONDS); // ⏱️ Timeout after 45 seconds

            } catch (TimeoutException e) {
                log.error("OpenAI request timed out after 45s for oxidation products of {}", chemical.getCommonName());
                return new ArrayList<>(); // EARLY RETURN
            } catch (InterruptedException | ExecutionException e) {
                log.error("OpenAI execution error for oxidation products of {}: {}", chemical.getCommonName(), e.getMessage());
                return new ArrayList<>(); // EARLY RETURN
            }
            // Cache the response
            vectorStoreService.cacheOxidationProducts(chemical.getCommonName(), response);

            return parseOxidationProducts(response);

        } catch (Exception e) {
            log.error("Error searching oxidation products for {}: {}", chemical.getCommonName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * PUBLIC METHOD: Parse side effects from cached response
     * Used by AllergenSearchController when vector cache hit occurs
     */
    public List<SideEffect> parseSideEffectsFromCache(String cachedResponse, ChemicalIdentification chemical) {
        return parseSideEffectsResponse(cachedResponse, chemical);
    }

    /**
     * PUBLIC METHOD: Parse oxidation products from cached response
     * Used by AllergenSearchController when vector cache hit occurs
     */
    public List<String> parseOxidationProductsFromCache(String cachedResponse) {
        return parseOxidationProducts(cachedResponse);
    }

    private String createSystemPrompt() {
        return """
            You are a medical researcher specializing in allergen identification and side effect documentation.
            
            **CRITICAL MEDICAL DISCLAIMER REQUIREMENTS:**
            - This information is for research purposes only
            - All findings must cite authoritative medical sources
            - Never provide medical advice or diagnoses
            - Always recommend consulting healthcare professionals
            
            **RESEARCH STANDARDS:**
            1. Prioritize peer-reviewed medical literature
            2. Search PubMed, medical journals, and clinical databases
            3. Focus on evidence-based findings with source attribution
            4. Include prevalence rates and severity classifications when available
            5. Distinguish between immediate and delayed reactions
            6. Note population-specific variations (age, gender, genetics)
            
            **Response Format for Each Side Effect:**
            EFFECT: [specific reaction name]
            SEVERITY: [MILD/MODERATE/SEVERE/LIFE_THREATENING]  
            PREVALENCE: [percentage or "rare/common/very common"]
            POPULATION: [affected groups - general/sensitive individuals/specific demographics]
            MECHANISM: [how the reaction occurs - IgE-mediated/contact sensitivity/irritant/etc]
            ONSET: [immediate/hours/days after exposure]
            AREAS: [body areas affected]
            EVIDENCE: [study details, sample size, methodology]
            SOURCE: [exact citation with DOI if available]
            
            **Source Quality Priorities:**
            1. Peer-reviewed medical journals
            2. Clinical trial data
            3. Government health agencies (FDA, EMA, etc)
            4. Professional medical associations
            5. Established medical databases
            
            Exclude anecdotal reports, social media, and non-medical sources.
            """;
    }

    private String createUserPrompt(ChemicalIdentification chemical) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("""
            Research documented side effects and allergic reactions for %s:
            
            **Chemical Identifiers:**
            - Common Name: %s
            - IUPAC Name: %s
            - CAS Number: %s
            - Molecular Formula: %s
            - PubChem CID: %s
            
            **Search Requirements:**
            1. Focus on allergic reactions and sensitization
            2. Include both immediate and delayed hypersensitivity
            3. Search for oxidation product allergies (e.g., "%s hydroperoxide", "%s oxide")
            4. Look for contact dermatitis and respiratory reactions
            5. Include cross-reactivity with similar compounds
            
            **Priority Search Terms:**
            - "allergic contact dermatitis"
            - "sensitization"  
            - "hypersensitivity"
            - "occupational allergy"
            - "fragrance allergy" (if applicable)
            - "cosmetic allergy" (if applicable)
            
            **Key Research Areas:**
            - Dermatology and contact dermatitis literature
            - Occupational health studies
            - Cosmetic ingredient safety data
            - Food allergy research (if applicable)
            - Environmental health studies
            
            Report only scientifically documented effects with proper source attribution.
            """,
                chemical.getCommonName(),
                chemical.getCommonName(),
                chemical.getIupacName() != null ? chemical.getIupacName() : "unknown",
                chemical.getCasNumber() != null ? chemical.getCasNumber() : "unknown",
                chemical.getMolecularFormula() != null ? chemical.getMolecularFormula() : "unknown",
                chemical.getPubchemCid() != null ? chemical.getPubchemCid().toString() : "unknown",
                chemical.getCommonName().toLowerCase(),
                chemical.getCommonName().toLowerCase()));

        // Add synonyms to improve search coverage
        if (chemical.getSynonyms() != null && !chemical.getSynonyms().isEmpty()) {
            prompt.append("\n**Alternative Names to Search:**\n");
            for (String synonym : chemical.getSynonyms().subList(0, Math.min(5, chemical.getSynonyms().size()))) {
                prompt.append("- ").append(synonym).append("\n");
            }
        }

        return prompt.toString();
    }

    private List<SideEffect> parseSideEffectsResponse(String response, ChemicalIdentification chemical) {
        List<SideEffect> sideEffects = new ArrayList<>();

        try {
            // Parse structured response format
            String[] effects = response.split("EFFECT:");

            log.debug("Parsing response with {} EFFECT blocks", effects.length);

            for (int i = 1; i < effects.length; i++) { // Skip first empty split
                String effectBlock = effects[i].trim();
                SideEffect sideEffect = parseIndividualEffect(effectBlock, chemical);

                if (sideEffect != null) {
                    sideEffects.add(sideEffect);
                }
            }

            log.info("Parsed {} side effects for {}", sideEffects.size(), chemical.getCommonName());

        } catch (Exception e) {
            log.error("Error parsing side effects response: {}", e.getMessage());
        }

        return sideEffects;
    }

    private SideEffect parseIndividualEffect(String effectBlock, ChemicalIdentification chemical) {
        try {
            log.debug("Processing effect block: {}", effectBlock.substring(0, Math.min(300, effectBlock.length())));

            SideEffect.SideEffectBuilder builder = SideEffect.builder()
                    .chemical(chemical)
                    .sources(new ArrayList<>())
                    .affectedBodyAreas(new ArrayList<>())
                    .verificationStatus(VerificationStatus.UNVERIFIED)
                    .confidenceScore(70); // Default moderate confidence

            // Extract effect name
            String effectName = extractField(effectBlock, "EFFECT", "SEVERITY");
            log.debug("Extracted effect name: '{}'", effectName);

            if (effectName == null) return null;

            builder.effectType(effectName.trim());
            builder.description("Documented allergic reaction: " + effectName.trim());

            // Extract severity
            String severityStr = extractField(effectBlock, "SEVERITY", "PREVALENCE");
            if (severityStr != null) {
                builder.severity(parseSeverity(severityStr.trim()));
            } else {
                builder.severity(Severity.MODERATE); // Default
            }

            // Extract prevalence
            String prevalenceStr = extractField(effectBlock, "PREVALENCE", "POPULATION");
            if (prevalenceStr != null) {
                builder.prevalenceRate(parsePrevalence(prevalenceStr.trim()));
            }

            // Extract affected population
            String populationStr = extractField(effectBlock, "POPULATION", "MECHANISM");
            if (populationStr != null) {
                builder.population(populationStr.trim());
            }

            // Extract exposure route/mechanism
            String mechanismStr = extractField(effectBlock, "MECHANISM", "ONSET");
            if (mechanismStr != null) {
                builder.exposureRoute(mechanismStr.trim());
            }

            // Extract body areas
            String areasStr = extractField(effectBlock, "AREAS", "EVIDENCE");
            if (areasStr != null) {
                List<String> areasList = new ArrayList<>();
                String[] areas = areasStr.split("[,;]");
                for (String area : areas) {
                    areasList.add(area.trim());
                }
                builder.affectedBodyAreas(areasList);
            }

            // Extract evidence and source
            String evidenceStr = extractField(effectBlock, "EVIDENCE", "SOURCE");
            if (evidenceStr != null) {
                builder.studyEvidence(evidenceStr.trim());
            }

            String sourceStr = extractField(effectBlock, "SOURCE", null);
            if (sourceStr != null) {
                SourceReference source = parseSourceReference(sourceStr.trim());
                List<SourceReference> sourcesList = new ArrayList<>();
                sourcesList.add(source);
                builder.sources(sourcesList);
                builder.verificationStatus(VerificationStatus.VERIFIED);
                builder.confidenceScore(85);
            }

            return builder.build();

        } catch (Exception e) {
            log.warn("Error parsing individual effect: {}", e.getMessage());
            return null;
        }
    }

    private List<String> parseOxidationProducts(String response) {
        List<String> products = new ArrayList<>();

        try {
            String[] productBlocks = response.split("PRODUCT:");

            for (int i = 1; i < productBlocks.length; i++) {
                String block = productBlocks[i].trim();
                String productName = extractField(block, "PRODUCT", "CAS");

                if (productName != null && !productName.trim().isEmpty()) {
                    products.add(productName.trim());
                }
            }

        } catch (Exception e) {
            log.error("Error parsing oxidation products: {}", e.getMessage());
        }

        return products;
    }

    // Helper methods
    private String extractField(String text, String startField, String endField) {
        try {
            Pattern pattern;

            // Handle both "EFFECT:" and "** EffectName" formats
            if (startField.equals("EFFECT")) {
                // Look for markdown format: ** EffectName
                pattern = Pattern.compile("\\*\\*\\s*([^*]+?)\\s*(?:\\n|$)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }

            // Handle other fields with markdown: **FIELD:** Value
            if (endField != null) {
                pattern = Pattern.compile("\\*\\*" + startField + ":\\*\\*\\s*(.*?)(?=\\*\\*|$)",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            } else {
                pattern = Pattern.compile("\\*\\*" + startField + ":\\*\\*\\s*(.*)",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            }

            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("Could not extract field {}: {}", startField, e.getMessage());
        }
        return null;
    }

    private Severity parseSeverity(String severityStr) {
        String severity = severityStr.toUpperCase();
        if (severity.contains("LIFE_THREATENING") || severity.contains("SEVERE")) {
            return Severity.SEVERE;
        } else if (severity.contains("MODERATE")) {
            return Severity.MODERATE;
        } else if (severity.contains("MILD")) {
            return Severity.MILD;
        }
        return Severity.MODERATE; // Default
    }

    private Double parsePrevalence(String prevalenceStr) {
        try {
            // Extract percentage if present
            Pattern percentPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
            Matcher matcher = percentPattern.matcher(prevalenceStr);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1)) / 100.0;
            }

            // Handle descriptive prevalence
            String prev = prevalenceStr.toLowerCase();
            if (prev.contains("rare")) return 0.01; // 1%
            if (prev.contains("uncommon")) return 0.05; // 5%
            if (prev.contains("common")) return 0.20; // 20%
            if (prev.contains("very common")) return 0.50; // 50%

        } catch (Exception e) {
            log.debug("Could not parse prevalence: {}", prevalenceStr);
        }
        return null;
    }

    private SourceReference parseSourceReference(String sourceStr) {
        SourceReference source = new SourceReference();

        // Try to extract DOI
        Pattern doiPattern = Pattern.compile("(10\\.\\d{4,}/[^\\s]+)");
        Matcher doiMatcher = doiPattern.matcher(sourceStr);
        if (doiMatcher.find()) {
            source.setDoi(doiMatcher.group(1));
        }

        // Try to extract year
        Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");
        Matcher yearMatcher = yearPattern.matcher(sourceStr);
        if (yearMatcher.find()) {
            try {
                int year = Integer.parseInt(yearMatcher.group(0));
                source.setPublicationDate(LocalDate.of(year, 1, 1));
            } catch (Exception ignored) {}
        }

        // Use full source as citation
        source.setCitation(sourceStr.substring(0, Math.min(500, sourceStr.length())));
        source.setStudyType("Literature Review");

        return source;
    }
}