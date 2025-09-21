package com.matthewbixby.allergen.intelligence.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceReference {
    private String title;
    private String url;
    private String studyType;
    private LocalDate publicationDate;
    private String citation;
    private String doi;
}