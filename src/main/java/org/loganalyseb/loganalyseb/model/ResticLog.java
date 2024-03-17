package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResticLog {

    private String nomFichier;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int nbErreur;
    private LocalDateTime dateDebutFull2Backup;
    private LocalDateTime dateDebutFull2Forget;
    private LocalDateTime dateDebutNasbackupBackup;
    private LocalDateTime dateDebutNasbackupForget;
    private LocalDateTime dateDebutRaspberryBackup;
    private LocalDateTime dateDebutRaspberryForget;
    private LocalDateTime dateDebutRclone;
}
