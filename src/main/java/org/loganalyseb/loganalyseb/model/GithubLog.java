package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class GithubLog {

    private String nomFichier;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int nbErreur;
    private long dureeTotaleSecondes;

    public void calculDuree() {
        if (dateDebut != null && dateFin != null) {
            dureeTotaleSecondes = Duration.between(dateDebut, dateFin).getSeconds();
        }
    }

}
