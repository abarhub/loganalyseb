package org.loganalyseb.loganalyseb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class NasbackupLog {

    private String nomFichier;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int nbErreur;

    @JsonProperty("dureeTotaleSecondes")
    public long duree() {
        if (dateDebut != null && dateFin != null) {
            return Duration.between(dateDebut, dateFin).getSeconds();
        } else {
            return 0L;
        }
    }

}
