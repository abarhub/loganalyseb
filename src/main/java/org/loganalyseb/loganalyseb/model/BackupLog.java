package org.loganalyseb.loganalyseb.model;

import lombok.Data;

@Data
public class BackupLog {

    private NasbackupLog nasbackupLog;
    private OvhBackupLog ovhBackupLog;
    private GithubLog githubLog;
    private ResticLog resticLog;

}
