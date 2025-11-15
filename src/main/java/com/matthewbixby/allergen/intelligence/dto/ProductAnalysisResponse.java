package com.matthewbixby.allergen.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductAnalysisResponse {
    private String productName;
    private Integer totalIngredients;
    private Integer highRiskIngredients;
    private String overallRiskLevel;  // HIGH, MODERATE, LOW
    private List<String> ingredients;
    private Map<String, IngredientAnalysis> detailedAnalysis;
    private List<String> recommendations;
    private String disclaimer;
    private String error;  // Optional - only if product not found
}