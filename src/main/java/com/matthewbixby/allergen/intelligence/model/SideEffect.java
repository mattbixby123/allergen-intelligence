package com.matthewbixby.allergen.intelligence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "side_effects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SideEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemical_id", nullable = false)
    private ChemicalIdentification chemical;

    // Effect classification
    @Column(nullable = false)
    private String effectType;  // "Contact Dermatitis", "Respiratory", etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // Epidemiological data
    private Double prevalenceRate;  // 0.0 to 1.0

    @ElementCollection
    @CollectionTable(name = "affected_body_areas")
    @Column(name = "area")
    private List<String> affectedBodyAreas = new ArrayList<>();

    // Medical context
    private String population;  // Who is affected
    private String exposureRoute;  // How exposure occurred
    private String dosage;  // Concentration/amount

    // Oxidation product tracking
    private Boolean isFromOxidationProduct;
    private String specificChemicalForm;  // Which exact chemical caused this

    // Source attribution - CRITICAL for medical info
    @ElementCollection
    @CollectionTable(name = "side_effect_sources",
            joinColumns = @JoinColumn(name = "side_effect_id"))
    private List<SourceReference> sources = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String studyEvidence;  // Direct quote from research

    // Verification and trust
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private Integer confidenceScore;  // 0-100

    private LocalDate studyDate;  // When study was published

    private LocalDateTime lastVerified;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
