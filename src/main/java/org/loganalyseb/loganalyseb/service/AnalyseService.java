package org.loganalyseb.loganalyseb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.loganalyseb.loganalyseb.exception.PlateformeException;
import org.loganalyseb.loganalyseb.model.*;
import org.loganalyseb.loganalyseb.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
import java.util.List;
import java.util.Optional;

@Slf4j
public class AnalyseService {

    private static final Logger ANALYTICS = LoggerFactory.getLogger("analytics");

    private final DateTimeFormatter formatters = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss: ");
    private final String BOM_UTF_16LE = "\uFEFF";

    private ObjectMapper objectMapper;

    private final AppProperties appProperties;

    private final DatabaseService databaseService;

    public AnalyseService(AppProperties appProperties, DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.appProperties = appProperties;
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.findAndRegisterModules();
    }

    public void analyse() throws PlateformeException {
        log.info("Début d'analyse");

        log.atInfo().log("Répertoire {}", appProperties.getRepertoire());
        log.atInfo().log("Date analyse {}", appProperties.getDateAnalyse());

        final Path repertoireLog = Path.of(appProperties.getRepertoire());
        if (Files.exists(repertoireLog) && Files.isDirectory(repertoireLog)) {

            if (appProperties.getDateAnalyse() != null) {
                analyseDate(repertoireLog, appProperties.getDateAnalyse());
            } else {
                analyseRepertoire(repertoireLog);
            }


        } else {
            throw new PlateformeException("Le répertoire " + repertoireLog + " n'existe pas");
        }
    }

    private void analyseRepertoire(Path repertoireLog) throws PlateformeException {
        var glob = "glob:log_backup_20*.log";
        var listeFichiers = findAllFiles(repertoireLog, glob);
        var liste = listeFichiers.stream().map(x -> x.getFileName().toString()).sorted().toList();
        LocalDate dateDerniereAnalyse = databaseService.getDaterDernier();
        for (var file : liste) {
            log.info("Détermination de la date du fichier {}", file);

            var s = StringUtils.substring(file, 11, 19);
            if (s != null && s.length() == 8) {

                try {
                    LocalDate date = LocalDate.parse(s, formatters);

                    log.info("fichier: {}, date: {}", file, date);

                    if (dateDerniereAnalyse != null && (date.isBefore(dateDerniereAnalyse)||date.equals(dateDerniereAnalyse))) {
                        log.warn("fichier trop ancien ou déjà traité: {}", file);
                    } else if (date.isAfter(LocalDate.of(2024, 3, 3))) {
                        analyseDate(repertoireLog, date);
                        databaseService.setDerniereDate(date);
                    } else {
                        log.warn("fichier trop ancien: {}", file);
                    }

                } catch (DateTimeParseException e) {
                    log.error("Erreur pour parser la date {}", s, e);
                }
            } else {
                log.error("Erreur pour analyser la date du fichier: {}" + file);
            }
        }
    }

    private void analyseDate(Path repertoireLog, LocalDate dateAnalyse) throws PlateformeException {
        try {
            MDC.put("date", DateTimeFormatter.ISO_DATE.format(dateAnalyse));

            BackupLog backupLog = new BackupLog();
            backupLog.setTimestamp(dateAnalyse);

            parse(repertoireLog, "log_backup", dateAnalyse, (file) -> {
                parseLogNasbackup(file, backupLog);
            });

            parse(repertoireLog, "log_backup_ovh", dateAnalyse, (file) -> {
                parseLogOvh(file, backupLog);
            });

            parse(repertoireLog, "log_backup_github", dateAnalyse, (file) -> {
                parseLogGithub(file, backupLog);
            });


            parse(repertoireLog, "log_backup_restic", dateAnalyse, (file) -> {
                parseLogRestic(file, backupLog);
            });

            backupLog.calculDuree();
            log.info("resultat: {}", backupLog);

            if (ANALYTICS.isInfoEnabled()) {
                try {
                    var s = objectMapper.writeValueAsString(backupLog);
                    ANALYTICS.info("{}", s);
                } catch (JsonProcessingException e) {
                    throw new PlateformeException("Erreur pour créer le json résultat", e);
                }
            }
        } finally {
            MDC.remove("date");
        }
    }

    private void parse(Path repertoireLog, String debutNom, LocalDate date, FailableConsumer<Path, PlateformeException> consumer) throws PlateformeException {
        var glob = "glob:" + debutNom + "_" + formatters.format(date) + "_*.log";
        var resultat = findFile(repertoireLog, glob);
        if (resultat.isPresent()) {
            var file = resultat.get();
            log.info("trouve: {}", file);

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
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne : iterableStream) {
                    MDC.put("noLigne", "" + noLigne);
                    LocalDateTime dateTime = null;
                    if (ligne != null && ligne.length() > 20) {
                        var s = ligne;
                        if (!debut) {
                            s = removeUTF16_LEBOM(s);
                        }
                        if (debutDate(s)) {
                            s = s.substring(0, 21);
                            try {
                                dateTime = LocalDateTime.parse(s, formatter2);
                            } catch (DateTimeParseException e) {
                                log.warn("Le format de la date n'est pas bon à la ligne {}", noLigne, e);
                            }
                        } else {
                            log.warn("Le format de la date n'est pas bon à la ligne {} (s={})", noLigne, s);
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
            } finally {
                MDC.remove("file");
                MDC.remove("noLigne");
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
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne : iterableStream) {
                    LocalDateTime dateTime = null;
                    MDC.put("noLigne", "" + noLigne);
                    if (ligne != null && ligne.length() > 20) {
                        var s = ligne;
                        if (!debut) {
                            s = removeUTF16_LEBOM(s);
                        }
                        s = s.substring(0, 21);
                        if (debutDate(s)) {
                            try {
                                dateTime = LocalDateTime.parse(s, formatter2);
                            } catch (DateTimeParseException e) {
                                log.warn("Le format de la date n'est pas bon à la ligne {}", noLigne, e);
                            }
                        } else {
                            log.warn("Le format de la date n'est pas bon à la ligne {} (s={})", noLigne, s);
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
            } finally {
                MDC.remove("file");
                MDC.remove("noLigne");
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
        int nbErreur = 0;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = Files.lines(file, StandardCharsets.UTF_16LE)) {
            Iterable<String> iterableStream = lignes::iterator;
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne : iterableStream) {
                    LocalDateTime dateTime = null;
                    MDC.put("noLigne", "" + noLigne);
                    if (ligne != null && ligne.length() > 20) {
                        var s = ligne;
                        if (!debut) {
                            s = removeUTF16_LEBOM(s);
                        }
                        s = s.substring(0, 21);
                        if (debutDate(s)) {
                            try {
                                dateTime = LocalDateTime.parse(s, formatter2);
                            } catch (DateTimeParseException e) {
                                log.warn("Le format de la date n'est pas bon à la ligne {}", noLigne, e);
                            }
                        } else {
                            log.warn("Le format de la date n'est pas bon à la ligne {} (s={})", noLigne, s);
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
                    if (ligne != null) {
                        if (ligne.contains("stderr") || ligne.contains("ERROR")) {
                            nbErreur++;
                        }
                    }
                    noLigne++;
                }
            } finally {
                MDC.remove("file");
                MDC.remove("noLigne");
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

    private boolean debutDate(String s) {
        return s != null && s.length() > 0 && (s.startsWith("0") || s.startsWith("1") || s.startsWith("2") || s.startsWith("3"));
    }

    private void parseLogRestic(Path file, BackupLog backupLog) throws PlateformeException {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        Optional<LocalDateTime> dateDebutFull2Backup = Optional.empty(), dateDebutFull2Forget = Optional.empty();
        Optional<LocalDateTime> dateDebutNasbackupBackup = Optional.empty(), dateDebutNasbackupForget = Optional.empty();
        Optional<LocalDateTime> dateDebutRaspberryBackup = Optional.empty(), dateDebutRaspberryForget = Optional.empty();
        Optional<LocalDateTime> dateDebutRclone = Optional.empty();
        boolean debut = false;
        int nbErreur = 0;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = Files.lines(file, StandardCharsets.UTF_16LE)) {
            Iterable<String> iterableStream = lignes::iterator;
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne : iterableStream) {
                    LocalDateTime dateTime = null;
                    MDC.put("noLigne", "" + noLigne);
                    if (ligne != null && ligne.length() > 20) {
                        var s = ligne;
                        if (!debut) {
                            s = removeUTF16_LEBOM(s);
                        }
                        s = s.substring(0, 21);
                        if (debutDate(s)) {
                            try {
                                dateTime = LocalDateTime.parse(s, formatter2);
                            } catch (DateTimeParseException e) {
                                log.warn("Le format de la date n'est pas bon à la ligne {}", noLigne, e);
                            }
                        } else {
                            log.warn("Le format de la date n'est pas bon à la ligne {} (s={})", noLigne, s);
                        }
                    }
                    if (!debut && dateTime != null) {
                        dateDebut = Optional.of(dateTime);
                        debut = true;
                    }
                    if (dateTime != null) {
                        dateFin = Optional.of(dateTime);

                        if (dateDebutFull2Backup.isEmpty() && ligne != null &&
                                ligne.contains("resticprofile.exe --name full2 backup")) {
                            dateDebutFull2Backup = Optional.of(dateTime);
                        }
                        if (dateDebutFull2Forget.isEmpty() && ligne != null &&
                                ligne.contains("resticprofile.exe --name full2 forget")) {
                            dateDebutFull2Forget = Optional.of(dateTime);
                        }
                        if (dateDebutNasbackupBackup.isEmpty() && ligne != null &&
                                ligne.contains("resticprofile.exe --name nasbackup backup")) {
                            dateDebutNasbackupBackup = Optional.of(dateTime);
                        }
                        if (dateDebutNasbackupForget.isEmpty() && ligne != null &&
                                ligne.contains("resticprofile.exe --name nasbackup forget")) {
                            dateDebutNasbackupForget = Optional.of(dateTime);
                        }
                        if (dateDebutRaspberryBackup.isEmpty() && ligne != null &&
                                ligne.contains("resticprofile.exe --name raspberrypi2 backup")) {
                            dateDebutRaspberryBackup = Optional.of(dateTime);
                        }
                        if (dateDebutRaspberryForget.isEmpty() && ligne != null &&
                                ligne.contains("resticprofile.exe --name raspberrypi2 forget")) {
                            dateDebutRaspberryForget = Optional.of(dateTime);
                        }
                        if (dateDebutRclone.isEmpty() && ligne != null &&
                                ligne.contains("rclone --progress --checksum")) {
                            dateDebutRclone = Optional.of(dateTime);
                        }

                    }
//                if(ligne!=null){
//                    if(ligne.contains("stderr")||ligne.contains("ERROR")){
//                        nbErreur++;
//                    }
//                }
                    noLigne++;
                }
            } finally {
                MDC.remove("file");
                MDC.remove("noLigne");
            }


        } catch (IOException e) {
            throw new PlateformeException("Erreur pour lire le fichier " + file, e);
        }

        log.info("date debut={}, date fin={}", dateDebut, dateFin);

        var resticLog = new ResticLog();
        resticLog.setNomFichier(file.getFileName().toString());
        dateDebut.ifPresent(resticLog::setDateDebut);
        dateFin.ifPresent(resticLog::setDateFin);
        dateDebutFull2Backup.ifPresent(resticLog::setDateDebutFull2Backup);
        dateDebutFull2Forget.ifPresent(resticLog::setDateDebutFull2Forget);
        dateDebutNasbackupBackup.ifPresent(resticLog::setDateDebutNasbackupBackup);
        dateDebutNasbackupForget.ifPresent(resticLog::setDateDebutNasbackupForget);
        dateDebutRaspberryBackup.ifPresent(resticLog::setDateDebutRaspberryBackup);
        dateDebutRaspberryForget.ifPresent(resticLog::setDateDebutRaspberryForget);
        dateDebutRclone.ifPresent(resticLog::setDateDebutRclone);
        resticLog.setNbErreur(nbErreur);
        backupLog.setResticLog(resticLog);
    }

    private List<Path> findAllFiles(Path repertoire, String glob) throws PlateformeException {
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
            return stream.toList();
        } catch (IOException e) {
            throw new PlateformeException("Erreur pour trouver le fichier correspondant au critere '" + glob + "'", e);
        }
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
