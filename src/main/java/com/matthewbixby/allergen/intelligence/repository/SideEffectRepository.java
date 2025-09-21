package com.matthewbixby.allergen.intelligence.repository;

import com.matthewbixby.allergen.intelligence.model.SideEffect;
import com.matthewbixby.allergen.intelligence.model.Severity;
import com.matthewbixby.allergen.intelligence.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SideEffectRepository extends JpaRepository<SideEffect, Long> {

    // Existing methods
    List<SideEffect> findByChemical_Id(Long chemicalId);
    List<SideEffect> findByEffectType(String effectType);

    // New methods for enhanced model
    List<SideEffect> findBySeverity(Severity severity);
    List<SideEffect> findByVerificationStatus(VerificationStatus status);

    // Find high-severity effects
    @Query("SELECT s FROM SideEffect s WHERE s.severity IN ('SEVERE', 'LIFE_THREATENING')")
    List<SideEffect> findHighSeverityEffects();

    // Find effects from oxidation products
    List<SideEffect> findByIsFromOxidationProduct(Boolean isFromOxidationProduct);

    // Find by specific chemical form
    List<SideEffect> findBySpecificChemicalForm(String chemicalForm);

    // Find effects needing verification (older than certain date)
    List<SideEffect> findByLastVerifiedBefore(LocalDateTime date);

    // Find verified effects with high confidence
    @Query("SELECT s FROM SideEffect s WHERE s.verificationStatus = 'VERIFIED' AND s.confidenceScore >= :minScore")
    List<SideEffect> findVerifiedWithMinConfidence(int minScore);

    // Find effects by chemical and severity
    List<SideEffect> findByChemical_IdAndSeverity(Long chemicalId, Severity severity);

    // Find effects with recent studies
    List<SideEffect> findByStudyDateAfter(LocalDate date);

    // Get all effects for a chemical including oxidation products
    @Query("SELECT s FROM SideEffect s WHERE s.chemical.id = :chemicalId OR s.chemical.commonName IN " +
            "(SELECT op FROM ChemicalIdentification c JOIN c.oxidationProducts op WHERE c.id = :chemicalId)")
    List<SideEffect> findAllEffectsIncludingOxidationProducts(Long chemicalId);
}