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
    private String nom;
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
        if (dateDebut != null && dateFin != null) {
            dureeTotaleSecondes = Duration.between(dateDebut, dateFin).getSeconds();
        }
        if (dateDebutFull2Backup != null && dateDebutFull2Forget != null) {
            dureeFull2BackupSecondes = Duration.between(dateDebutFull2Backup, dateDebutFull2Forget).getSeconds();
        }
        if (dateDebutFull2Forget != null && dateDebutNasbackupBackup != null) {
            dureeFull2FogetSecondes = Duration.between(dateDebutFull2Forget, dateDebutNasbackupBackup).getSeconds();
        }
        if (dateDebutNasbackupBackup != null && dateDebutNasbackupForget != null) {
            dureeNasbackupBackupSecondes = Duration.between(dateDebutNasbackupBackup, dateDebutNasbackupForget).getSeconds();
        }
        if (dateDebutNasbackupForget != null && dateDebutRaspberryBackup != null) {
            dureeNasbackupForgetSecondes = Duration.between(dateDebutNasbackupForget, dateDebutRaspberryBackup).getSeconds();
        }
        if (dateDebutRaspberryBackup != null && dateDebutRaspberryForget != null) {
            dureeRaspberryBackupSecondes = Duration.between(dateDebutRaspberryBackup, dateDebutRaspberryForget).getSeconds();
        }
        if (dateDebutRaspberryForget != null && dateDebutRclone != null) {
            dureeRaspberryForgetSecondes = Duration.between(dateDebutRaspberryForget, dateDebutRclone).getSeconds();
        }
        if (dateDebutRclone != null && dateFin != null) {
            dureeRcloneSecondes = Duration.between(dateDebutRclone, dateFin).getSeconds();
        }
    }
}
