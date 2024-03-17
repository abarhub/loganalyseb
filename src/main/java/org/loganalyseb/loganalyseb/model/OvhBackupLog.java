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
        dureeTotaleSecondes = Duration.between(dateDebut, dateFin).getSeconds();
        dureeFileutilsSecondes = Duration.between(dateDebut, dateDebutRclone).getSeconds();
        dureeRcloneSecondes = Duration.between(dateDebutRclone, dateFin).getSeconds();
    }


}
