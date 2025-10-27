package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class BackupLog {

    private LocalDate timestamp;
    private NasbackupLog nasbackupLog;
    private OvhBackupLog ovhBackupLog;
    private GithubLog githubLog;
    private ResticLog resticLog;
    private List<ResticLog> resticLogList;
    private GoBackupLog goBackupLog;
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
        if (resticLogList != null) {
            resticLogList.forEach(ResticLog::calculDuree);
        }
        if (goBackupLog != null) {
            goBackupLog.calculDuree();
        }
    }

    public void addResticLog(ResticLog resticLog) {
        if (resticLogList == null) {
            resticLogList = new ArrayList<>();
        }
        resticLogList.add(resticLog);
    }
}
