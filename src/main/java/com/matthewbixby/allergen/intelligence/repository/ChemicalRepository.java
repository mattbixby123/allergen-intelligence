package com.matthewbixby.allergen.intelligence.repository;

import com.matthewbixby.allergen.intelligence.model.ChemicalIdentification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChemicalRepository extends JpaRepository<ChemicalIdentification, Long> {

    // Existing methods
    Optional<ChemicalIdentification> findByCasNumber(String casNumber);
    Optional<ChemicalIdentification> findByCommonNameIgnoreCase(String commonName);
    Optional<ChemicalIdentification> findByScientificNameIgnoreCase(String scientificName);

    // New methods for updated model
    Optional<ChemicalIdentification> findByPubchemCid(Long pubchemCid);
    Optional<ChemicalIdentification> findByInchiKey(String inchiKey);

    List<ChemicalIdentification> findByChemicalFamily(String chemicalFamily);
    List<ChemicalIdentification> findByIsOxidationProduct(boolean isOxidationProduct);

    // Find chemicals with specific oxidation products
    @Query("SELECT c FROM ChemicalIdentification c WHERE :product MEMBER OF c.oxidationProducts")
    List<ChemicalIdentification> findByOxidationProduct(String product);

    // Find by synonym
    @Query("SELECT c FROM ChemicalIdentification c WHERE :synonym MEMBER OF c.synonyms")
    List<ChemicalIdentification> findBySynonym(String synonym);
}