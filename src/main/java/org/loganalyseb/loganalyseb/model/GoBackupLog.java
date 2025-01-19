package org.loganalyseb.loganalyseb.model;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class GoBackupLog {

    private String nomFichier;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int nbErreur;
    private LocalDateTime dateDebutMesdoc;
    private LocalDateTime dateDebutCompressionMesdoc;
    private LocalDateTime dateDebutCryptageMesdoc;
    private LocalDateTime dateDebutEnregistrementHashMesdoc;
    private LocalDateTime dateFinMesdoc;
    private LocalDateTime dateDebutWeb;
    private LocalDateTime dateDebutCompressionWeb;
    private LocalDateTime dateDebutCryptageWeb;
    private LocalDateTime dateDebutEnregistrementHashWeb;
    private LocalDateTime dateFinWeb;
    private LocalDateTime dateDebutDocument;
    private LocalDateTime dateDebutCompressionDocument;
    private LocalDateTime dateDebutCryptageDocument;
    private LocalDateTime dateDebutEnregistrementHashDocument;
    private LocalDateTime dateFinDocument;
    private LocalDateTime dateDebutProjets;
    private LocalDateTime dateDebutCompressionProjets;
    private LocalDateTime dateDebutCryptageProjets;
    private LocalDateTime dateDebutEnregistrementHashProjets;
    private LocalDateTime dateFinProjets;
    private long dureeTotaleSecondes;
    private long dureeMesdocSecondes;
    private long dureeCompressionMesdocSecondes;
    private long dureeCryptageMesdocSecondes;
    private long dureeEnregistrementHashMesdocSecondes;
    private long dureeWebSecondes;
    private long dureeCompressionWebSecondes;
    private long dureeCryptageWebSecondes;
    private long dureeEnregistrementHashWebSecondes;
    private long dureeDocumentSecondes;
    private long dureeCompressionDocumentSecondes;
    private long dureeCryptageDocumentSecondes;
    private long dureeEnregistrementHashDocumentSecondes;
    private long dureeProjetsSecondes;
    private long dureeCompressionProjetsSecondes;
    private long dureeCryptageProjetsSecondes;
    private long dureeEnregistrementHashProjetsSecondes;


    public void calculDuree() {
        if (dateDebut != null && dateFin != null) {
            dureeTotaleSecondes = Duration.between(dateDebut, dateFin).getSeconds();
        }
        // mesdoc
        if (dateDebutMesdoc != null && dateFinMesdoc != null) {
            dureeMesdocSecondes = Duration.between(dateDebutMesdoc, dateFinMesdoc).getSeconds();
        }
        if (dateDebutCompressionMesdoc != null && dateDebutCryptageMesdoc != null) {
            dureeCompressionMesdocSecondes = Duration.between(dateDebutCompressionMesdoc, dateDebutCryptageMesdoc).getSeconds();
        }
        if (dateDebutCryptageMesdoc != null && dateDebutEnregistrementHashMesdoc != null) {
            dureeCryptageMesdocSecondes = Duration.between(dateDebutCryptageMesdoc, dateDebutEnregistrementHashMesdoc).getSeconds();
        }
        if (dateDebutEnregistrementHashMesdoc != null && dateFinMesdoc != null) {
            dureeEnregistrementHashMesdocSecondes = Duration.between(dateDebutEnregistrementHashMesdoc, dateFinMesdoc).getSeconds();
        }
        // web
        if (dateDebutWeb != null && dateFinWeb != null) {
            dureeWebSecondes = Duration.between(dateDebutWeb, dateFinWeb).getSeconds();
        }
        if (dateDebutCompressionWeb != null && dateDebutCryptageWeb != null) {
            dureeCompressionWebSecondes = Duration.between(dateDebutCompressionWeb, dateDebutCryptageWeb).getSeconds();
        }
        if (dateDebutCryptageWeb != null && dateDebutEnregistrementHashWeb != null) {
            dureeCryptageWebSecondes = Duration.between(dateDebutCryptageWeb, dateDebutEnregistrementHashWeb).getSeconds();
        }
        if (dateDebutEnregistrementHashWeb != null && dateFinWeb != null) {
            dureeEnregistrementHashWebSecondes = Duration.between(dateDebutEnregistrementHashWeb, dateFinWeb).getSeconds();
        }
        // document
        if (dateDebutDocument != null && dateFinDocument != null) {
            dureeDocumentSecondes = Duration.between(dateDebutDocument, dateFinDocument).getSeconds();
        }
        if (dateDebutCompressionDocument != null && dateDebutCryptageDocument != null) {
            dureeCompressionDocumentSecondes = Duration.between(dateDebutCompressionDocument, dateDebutCryptageDocument).getSeconds();
        }
        if (dateDebutCryptageDocument != null && dateDebutEnregistrementHashDocument != null) {
            dureeCryptageDocumentSecondes = Duration.between(dateDebutCryptageDocument, dateDebutEnregistrementHashDocument).getSeconds();
        }
        if (dateDebutEnregistrementHashDocument != null && dateFinDocument != null) {
            dureeEnregistrementHashDocumentSecondes = Duration.between(dateDebutEnregistrementHashDocument, dateFinDocument).getSeconds();
        }
        // projets
        if (dateDebutProjets != null && dateFinProjets != null) {
            dureeProjetsSecondes = Duration.between(dateDebutProjets, dateFinProjets).getSeconds();
        }
        if (dateDebutCompressionProjets != null && dateDebutCryptageProjets != null) {
            dureeCompressionProjetsSecondes = Duration.between(dateDebutCompressionProjets, dateDebutCryptageProjets).getSeconds();
        }
        if (dateDebutCryptageProjets != null && dateDebutEnregistrementHashProjets != null) {
            dureeCryptageProjetsSecondes = Duration.between(dateDebutCryptageProjets, dateDebutEnregistrementHashProjets).getSeconds();
        }
        if (dateDebutEnregistrementHashProjets != null && dateFinProjets != null) {
            dureeEnregistrementHashProjetsSecondes = Duration.between(dateDebutEnregistrementHashProjets, dateFinProjets).getSeconds();
        }
    }
}
