package com.matthewbixby.allergen.intelligence.model;

import jakarta.persistence.Column;
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
    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String url;

    private String studyType;
    private LocalDate publicationDate;

    @Column(columnDefinition = "TEXT")
    private String citation;

    private String doi;
}