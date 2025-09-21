package com.matthewbixby.allergen.intelligence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chemical_identifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChemicalIdentification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Direct from PubChem
    @Column(unique = true)
    private Long pubchemCid;  // ADD THIS - PubChem Compound ID

    @Column(nullable = false)
    private String commonName;  // User input or preferred name

    private String iupacName;  // From PubChem

    private String casNumber;  // From PubChem (may be null)

    private String molecularFormula;  // ADD THIS - From PubChem

    private Double molecularWeight;  // ADD THIS - From PubChem

    @Column(columnDefinition = "TEXT")
    private String smiles;  // From PubChem

    private String inchi;  // ADD THIS - From PubChem
    private String inchiKey;  // ADD THIS - From PubChem

    @ElementCollection
    @CollectionTable(name = "chemical_synonyms")
    @Column(name = "synonym")
    private List<String> synonyms = new ArrayList<>();  // From PubChem

    // Derived/Calculated by you
    private String chemicalFamily;  // You determine this

    private boolean isOxidationProduct;  // You determine this

    @ElementCollection
    @CollectionTable(name = "oxidation_products")
    @Column(name = "product")
    private List<String> oxidationProducts = new ArrayList<>();  // You research this

    // Not from PubChem - added separately
    private String scientificName;  // May differ from IUPAC name

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastUpdated;
}