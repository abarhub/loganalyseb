package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.Duration;
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
    private long dureeTotaleSecondes;
    private long dureeFull2BackupSecondes;
    private long dureeFull2FogetSecondes;
    private long dureeNasbackupBackupSecondes;
    private long dureeNasbackupForgetSecondes;
    private long dureeRaspberryBackupSecondes;
    private long dureeRaspberryForgetSecondes;
    private long dureeRcloneSecondes;

    public void calculDuree() {
        dureeTotaleSecondes = Duration.between(dateDebut, dateFin).getSeconds();
        dureeFull2BackupSecondes = Duration.between(dateDebutFull2Backup, dateDebutFull2Forget).getSeconds();
        dureeFull2FogetSecondes = Duration.between(dateDebutFull2Forget, dateDebutNasbackupBackup).getSeconds();
        dureeNasbackupBackupSecondes = Duration.between(dateDebutNasbackupBackup, dateDebutNasbackupForget).getSeconds();
        dureeNasbackupForgetSecondes = Duration.between(dateDebutNasbackupForget, dateDebutRaspberryBackup).getSeconds();
        dureeRaspberryBackupSecondes = Duration.between(dateDebutRaspberryBackup, dateDebutRaspberryForget).getSeconds();
        dureeRaspberryForgetSecondes = Duration.between(dateDebutRaspberryForget, dateDebutRclone).getSeconds();
        dureeRcloneSecondes = Duration.between(dateDebutRclone, dateFin).getSeconds();
    }
}
