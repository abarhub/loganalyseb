package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BackupLog {

    private LocalDate timestamp;
    private NasbackupLog nasbackupLog;
    private OvhBackupLog ovhBackupLog;
    private GithubLog githubLog;
    private ResticLog resticLog;

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
