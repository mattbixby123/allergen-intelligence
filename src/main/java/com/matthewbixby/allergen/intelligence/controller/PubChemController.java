package com.matthewbixby.allergen.intelligence.controller;

import com.matthewbixby.allergen.intelligence.model.ChemicalIdentification;
import com.matthewbixby.allergen.intelligence.service.PubChemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pubchem")
@RequiredArgsConstructor
public class PubChemController {

    private final PubChemService pubChemService;

    @GetMapping("/chemical/{chemical}")
    public ChemicalIdentification testChemical(@PathVariable String chemical) {
        return pubChemService.getChemicalData(chemical);
    }

    @GetMapping("/health")
    public String health() {
        return "Allergen Intelligence Platform is running!";
    }
}