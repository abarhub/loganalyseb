package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NasbackupLog {

    private String nomFichier;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;


}
