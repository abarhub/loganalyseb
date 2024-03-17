package org.loganalyseb.loganalyseb.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.FailableConsumer;
import org.loganalyseb.loganalyseb.exception.PlateformeException;
import org.loganalyseb.loganalyseb.model.BackupLog;
import org.loganalyseb.loganalyseb.model.GithubLog;
import org.loganalyseb.loganalyseb.model.NasbackupLog;
import org.loganalyseb.loganalyseb.model.OvhBackupLog;
import org.loganalyseb.loganalyseb.properties.AppProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class AnalyseService {

    private final DateTimeFormatter formatters = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss: ");
    private final String BOM_UTF_16LE = "\uFEFF";

    private final AppProperties appProperties;

    public AnalyseService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void analyse() throws PlateformeException {
        log.info("Début d'analyse");

        log.atInfo().log("Répertoire {}", appProperties.getRepertoire());
        log.atInfo().log("Date analyse {}", appProperties.getDateAnalyse());

        final Path repertoireLog = Path.of(appProperties.getRepertoire());// log_backup
        if (Files.exists(repertoireLog) && Files.isDirectory(repertoireLog)) {

            if (appProperties.getDateAnalyse() != null) {

                BackupLog backupLog = new BackupLog();


                parse(repertoireLog,"log_backup",appProperties.getDateAnalyse(),(file)->{
                    parseLogNasbackup(file, backupLog);
                });

                parse(repertoireLog,"log_backup_ovh",appProperties.getDateAnalyse(),(file)->{
                    parseLogOvh(file, backupLog);
                });

                parse(repertoireLog,"log_backup_github",appProperties.getDateAnalyse(),(file)->{
                    parseLogGithub(file, backupLog);
                });

                log.info("resultat: {}", backupLog);

            }

        } else {
            throw new PlateformeException("Le répertoire " + repertoireLog + " n'existe pas");
        }
    }

    private void parse(Path repertoireLog, String debutNom, LocalDate date, FailableConsumer<Path,PlateformeException> consumer) throws PlateformeException {
        var glob = "glob:"+debutNom+"_" + formatters.format(date) + "_*.log";
        var resultat = findFile(repertoireLog, glob);
        if (resultat.isPresent()) {
            var file = resultat.get();
            log.info("trouve: {}", file);

//            parseLogGithub(file, backupLog);
            consumer.accept(file);

        } else {
            log.warn("Impossible de trouver le fichier {}", glob);
        }
    }

    private void parseLogNasbackup(Path file, BackupLog backupLog) throws PlateformeException {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        boolean debut = false;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = Files.lines(file, StandardCharsets.UTF_16LE)) {
            Iterable<String> iterableStream = lignes::iterator;
            int noLigne = 1;
            for (var ligne : iterableStream) {
                LocalDateTime dateTime = null;
                if (ligne != null && ligne.length() > 20) {
                    var s = ligne;
                    if (!debut) {
                        s = removeUTF16_LEBOM(s);
                    }
                    s = s.substring(0, 21);
                    try {
                        dateTime = LocalDateTime.parse(s, formatter2);
                    } catch (DateTimeParseException e) {
                        log.warn("Le format de la date n'est pas bon à la ligne {}", noLigne, e);
                    }
                }
                if (!debut && dateTime != null) {
                    dateDebut = Optional.of(dateTime);
                    debut = true;
                }
                if (dateTime != null) {
                    dateFin = Optional.of(dateTime);
                }
                noLigne++;
            }


        } catch (IOException e) {
            throw new PlateformeException("Erreur pour lire le fichier " + file, e);
        }

        log.info("date debut={}, date fin={}", dateDebut, dateFin);

        NasbackupLog nasbackupLog = new NasbackupLog();
        nasbackupLog.setNomFichier(file.getFileName().toString());
        dateDebut.ifPresent(nasbackupLog::setDateDebut);
        dateFin.ifPresent(nasbackupLog::setDateFin);
        backupLog.setNasbackupLog(nasbackupLog);
    }


    private void parseLogOvh(Path file, BackupLog backupLog) throws PlateformeException {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        Optional<LocalDateTime> dateDebutRClone = Optional.empty();
        boolean debut = false;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = Files.lines(file, StandardCharsets.UTF_16LE)) {
            Iterable<String> iterableStream = lignes::iterator;
            int noLigne = 1;
            for (var ligne : iterableStream) {
                LocalDateTime dateTime = null;
                if (ligne != null && ligne.length() > 20) {
                    var s = ligne;
                    if (!debut) {
                        s = removeUTF16_LEBOM(s);
                    }
                    s = s.substring(0, 21);
                    try {
                        dateTime = LocalDateTime.parse(s, formatter2);
                    } catch (DateTimeParseException e) {
                        log.warn("Le format de la date n'est pas bon à la ligne {}", noLigne, e);
                    }
                }
                if (!debut && dateTime != null) {
                    dateDebut = Optional.of(dateTime);
                    debut = true;
                }
                if (dateTime != null) {
                    dateFin = Optional.of(dateTime);

                    if (dateDebutRClone.isEmpty() && ligne != null &&
                            ligne.contains("transfert rclone ovh")) {
                        dateDebutRClone = Optional.of(dateTime);
                    }

                }
                noLigne++;
            }


        } catch (IOException e) {
            throw new PlateformeException("Erreur pour lire le fichier " + file, e);
        }

        log.info("date debut={}, date fin={}", dateDebut, dateFin);

        var ovhBackupLog = new OvhBackupLog();
        ovhBackupLog.setNomFichier(file.getFileName().toString());
        dateDebut.ifPresent(ovhBackupLog::setDateDebut);
        dateFin.ifPresent(ovhBackupLog::setDateFin);
        dateDebutRClone.ifPresent(ovhBackupLog::setDateDebutRclone);
        backupLog.setOvhBackupLog(ovhBackupLog);
    }

    private void parseLogGithub(Path file, BackupLog backupLog) throws PlateformeException {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        Optional<LocalDateTime> dateDebutRClone = Optional.empty();
        boolean debut = false;
        int nbErreur=0;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = Files.lines(file, StandardCharsets.UTF_16LE)) {
            Iterable<String> iterableStream = lignes::iterator;
            int noLigne = 1;
            for (var ligne : iterableStream) {
                LocalDateTime dateTime = null;
                if (ligne != null && ligne.length() > 20) {
                    var s = ligne;
                    if (!debut) {
                        s = removeUTF16_LEBOM(s);
                    }
                    s = s.substring(0, 21);
                    try {
                        dateTime = LocalDateTime.parse(s, formatter2);
                    } catch (DateTimeParseException e) {
                        log.warn("Le format de la date n'est pas bon à la ligne {}", noLigne, e);
                    }
                }
                if (!debut && dateTime != null) {
                    dateDebut = Optional.of(dateTime);
                    debut = true;
                }
                if (dateTime != null) {
                    dateFin = Optional.of(dateTime);

                    if (dateDebutRClone.isEmpty() && ligne != null &&
                            ligne.contains("transfert rclone ovh")) {
                        dateDebutRClone = Optional.of(dateTime);
                    }

                }
                if(ligne!=null){
                    if(ligne.contains("stderr")||ligne.contains("ERROR")){
                        nbErreur++;
                    }
                }
                noLigne++;
            }


        } catch (IOException e) {
            throw new PlateformeException("Erreur pour lire le fichier " + file, e);
        }

        log.info("date debut={}, date fin={}", dateDebut, dateFin);

        var githubLog = new GithubLog();
        githubLog.setNomFichier(file.getFileName().toString());
        dateDebut.ifPresent(githubLog::setDateDebut);
        dateFin.ifPresent(githubLog::setDateFin);
        githubLog.setNbErreur(nbErreur);
        backupLog.setGithubLog(githubLog);
    }


    private Optional<Path> findFile(Path repertoire, String glob) throws PlateformeException {
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(glob);
        try (var stream = Files.find(repertoire, 1, (path, attributes) -> {
            if (Files.isDirectory(path)) {
                return false;
            } else if (pathMatcher.matches(path.getFileName())) {
                return true;
            } else {
                return false;
            }
        })) {
            return stream.findAny();
        } catch (IOException e) {
            throw new PlateformeException("Erreur pour trouver le fichier correspondant au critere '" + glob + "'", e);
        }
    }

    private String removeUTF16_LEBOM(String s) {
        if (s.startsWith(BOM_UTF_16LE)) {
            s = s.substring(1);
        }
        return s;
    }
}
