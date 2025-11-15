package com.matthewbixby.allergen.intelligence.dto;

import com.matthewbixby.allergen.intelligence.model.ChemicalIdentification;
import com.matthewbixby.allergen.intelligence.model.SideEffect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IngredientAnalysis {
    private ChemicalIdentification chemical;
    private List<SideEffect> sideEffects;
    private List<String> oxidationProducts;
    private String riskLevel;  // HIGH, MODERATE, LOW, UNKNOWN
    private String error;  // Optional - only if ingredient analysis failed
}