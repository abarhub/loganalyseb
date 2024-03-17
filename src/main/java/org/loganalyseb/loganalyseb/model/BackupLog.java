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

}
