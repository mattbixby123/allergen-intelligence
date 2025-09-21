package com.matthewbixby.allergen.intelligence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matthewbixby.allergen.intelligence.model.ChemicalIdentification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PubChemService {

    private static final String PUBCHEM_API = "https://pubchem.ncbi.nlm.nih.gov/rest/pug";
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VectorStoreService vectorStoreService;

    public ChemicalIdentification getChemicalData(String ingredientName) {
        // Check cache first
        Optional<String> cached = vectorStoreService.getCachedChemical(ingredientName);
        if (cached.isPresent()) {
            try {
                log.info("Using cached compound data for: {}", ingredientName);
                return parseChemicalResponse(cached.get(), ingredientName);
            } catch (Exception e) {
                log.warn("Failed to parse cached compound data, fetching fresh", e);
                // Continue to fetch fresh data if cache parsing fails
            }
        }

        try {
            String url = PUBCHEM_API + "/compound/name/" + ingredientName + "/JSON";

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                log.warn("No response from PubChem for: {}", ingredientName);
                return null;
            }

            // Cache the compound response for future use
            vectorStoreService.cacheChemicalData(ingredientName, response);

            return parseChemicalResponse(response, ingredientName);

        } catch (Exception e) {
            log.error("Error fetching PubChem data for: {}", ingredientName, e);
            return null;
        }
    }

    private ChemicalIdentification parseChemicalResponse(String response, String ingredientName) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode compound = root.path("PC_Compounds").get(0);

        // Extract CID
        Long cid = compound.path("id").path("id").path("cid").asLong();

        // Build chemical identification
        ChemicalIdentification chemical = ChemicalIdentification.builder()
                .commonName(ingredientName)
                .pubchemCid(cid)
                .synonyms(new ArrayList<>())
                .oxidationProducts(new ArrayList<>())
                .isOxidationProduct(false)
                .build();

        // Extract properties
        JsonNode props = compound.path("props");
        for (JsonNode prop : props) {
            String label = prop.path("urn").path("label").asText();
            JsonNode value = prop.path("value");

            log.debug("PubChem property - Label: '{}', Value: {}", label, value);

            switch (label) {
                case "IUPAC Name":
                    chemical.setIupacName(prop.path("value").path("sval").asText());
                    break;
                case "Molecular Formula":
                    chemical.setMolecularFormula(prop.path("value").path("sval").asText());
                    break;
                case "Molecular Weight":
                    String weightStr = prop.path("value").path("sval").asText();
                    chemical.setMolecularWeight(Double.parseDouble(weightStr));
                    break;
                case "SMILES":
                    chemical.setSmiles(prop.path("value").path("sval").asText());
                    break;
                case "InChI":
                    chemical.setInchi(prop.path("value").path("sval").asText());
                    break;
                case "InChIKey":
                    chemical.setInchiKey(prop.path("value").path("sval").asText());
                    break;
                case "CAS":
                    chemical.setCasNumber(prop.path("value").path("sval").asText());
                    break;
            }
        }

        // Get synonyms (with caching)
        List<String> synonyms = getSynonyms(cid);
        chemical.setSynonyms(synonyms);

        // Extract CAS if not already found
        if (chemical.getCasNumber() == null) {
            extractCasFromSynonyms(chemical, synonyms);
        }

        return chemical;
    }

    private List<String> getSynonyms(Long cid) {
        // Check cache first
        Optional<String> cached = vectorStoreService.getCachedSynonyms(cid);
        if (cached.isPresent()) {
            try {
                log.info("Using cached synonyms for CID: {}", cid);
                return parseSynonymsResponse(cached.get());
            } catch (Exception e) {
                log.warn("Failed to parse cached synonyms, fetching fresh", e);
                // Continue to fetch fresh data
            }
        }

        try {
            String url = PUBCHEM_API + "/compound/cid/" + cid + "/synonyms/JSON";

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                log.warn("No synonyms response from PubChem for CID: {}", cid);
                return new ArrayList<>();
            }

            // Cache the synonyms response
            vectorStoreService.cacheSynonymsData(cid, response);

            return parseSynonymsResponse(response);

        } catch (Exception e) {
            log.error("Error fetching synonyms for CID: {}", cid, e);
            return new ArrayList<>();
        }
    }

    private List<String> parseSynonymsResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode synonymArray = root.path("InformationList")
                .path("Information")
                .get(0)
                .path("Synonym");

        List<String> synonyms = new ArrayList<>();

        // Limit to first 10 synonyms to avoid huge lists
        int maxSynonyms = Math.min(10, synonymArray.size());
        for (int i = 0; i < maxSynonyms; i++) {
            String synonym = synonymArray.get(i).asText();
            synonyms.add(synonym);

            // Check if this synonym is a CAS number (format: XXX-XX-X or XXXXX-XX-X)
            if (synonym.matches("\\d{2,7}-\\d{2}-\\d")) {
                log.debug("Found CAS number in synonyms: {}", synonym);
            }
        }

        log.info("Retrieved {} synonyms", synonyms.size());
        return synonyms;
    }

    private void extractCasFromSynonyms(ChemicalIdentification chemical, List<String> synonyms) {
        // Look for CAS number pattern in synonyms
        for (String synonym : synonyms) {
            if (synonym.matches("\\d{2,7}-\\d{2}-\\d")) {
                chemical.setCasNumber(synonym);
                log.info("Found CAS number {} in synonyms", synonym);
                break;
            }
        }
    }
}