package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class OvhBackupLog {

    private String nomFichier;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateDebutRclone;
    private int nbErreur;
    private long dureeTotaleSecondes;
    private long dureeFileutilsSecondes;
    private long dureeRcloneSecondes;

    public void calculDuree() {
        if (dateDebut != null && dateFin != null) {
            dureeTotaleSecondes = Duration.between(dateDebut, dateFin).getSeconds();
        }
        if (dateDebut != null && dateDebutRclone != null) {
            dureeFileutilsSecondes = Duration.between(dateDebut, dateDebutRclone).getSeconds();
        }
        if (dateDebutRclone != null && dateFin != null) {
            dureeRcloneSecondes = Duration.between(dateDebutRclone, dateFin).getSeconds();
        }
    }


}
