package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BackupLog {

    private LocalDate timestamp;
    private NasbackupLog nasbackupLog;
    private OvhBackupLog ovhBackupLog;
    private GithubLog githubLog;
    private ResticLog resticLog;
    private LocalDateTime dateAnalyse = LocalDateTime.now();

    public void calculDuree() {
        if (nasbackupLog != null) {
            nasbackupLog.calculDuree();
        }
        if (ovhBackupLog != null) {
            ovhBackupLog.calculDuree();
        }
        if (githubLog != null) {
            githubLog.calculDuree();
        }
        if (resticLog != null) {
            resticLog.calculDuree();
        }
    }
}
