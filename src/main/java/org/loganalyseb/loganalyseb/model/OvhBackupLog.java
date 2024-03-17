package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OvhBackupLog {

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateDebutRclone;


}
