package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OvhBackupLog {

    private String nomFichier;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateDebutRclone;
    private int nbErreur;


}
