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
import org.loganalyseb.loganalyseb.vo.GoBackupVo;
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
import java.util.*;

import static java.util.Map.entry;

@Slf4j
public class AnalyseService {

    private static final Logger ANALYTICS = LoggerFactory.getLogger("analytics");

    private final DateTimeFormatter formatters3 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String BOM_UTF_16LE = "\uFEFF";

    private ObjectMapper objectMapper;

    private final AppProperties appProperties;

    private final DatabaseService databaseService;

    private final MetricsPusher metricsPusher;

    public AnalyseService(AppProperties appProperties, DatabaseService databaseService, MetricsPusher metricsPusher) {
        this.databaseService = databaseService;
        this.appProperties = Objects.requireNonNull(appProperties, "la propriété app est nulle");
        this.metricsPusher = metricsPusher;
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
                analyseDate(repertoireLog, appProperties.getDateAnalyse(), null);// TODO: a corriger
            } else {
                analyseRepertoire(repertoireLog);
            }


        } else {
            throw new PlateformeException("Le répertoire " + repertoireLog + " n'existe pas");
        }

        analyseFichiers();
    }

    private void analyseFichiers() {
        metricsPusher.pushFilesMetrics();
    }

    private void analyseRepertoire(Path repertoireLog) throws PlateformeException {
        var glob = "glob:program_output.20*.log";
        var listeFichiers = findAllFiles(repertoireLog, glob);
        var liste = listeFichiers.stream().map(x -> x.getFileName().toString()).sorted().toList();
        LocalDate dateDerniereAnalyse = databaseService.getDaterDernier();
        for (var file : liste) {
            log.info("Détermination de la date du fichier {}", file);

            var s = StringUtils.substring(file, 15, 25);
            if (s != null && s.length() == 10) {

                try {
                    LocalDate date = LocalDate.parse(s, formatters3);

                    log.info("fichier: {}, date: {}", file, date);

                    if (dateDerniereAnalyse != null && (date.isBefore(dateDerniereAnalyse) || date.equals(dateDerniereAnalyse))) {
                        log.warn("fichier trop ancien ou déjà traité: {}", file);
                    } else {
                        analyseDate(repertoireLog, date, file);
                        databaseService.setDerniereDate(date);
                    }

                } catch (DateTimeParseException e) {
                    log.error("Erreur pour parser la date {}", s, e);
                }
            } else {
                log.error("Erreur pour analyser la date du fichier: {}" + file);
            }
        }
    }

    private void analyseDate(Path repertoireLog, LocalDate dateAnalyse, String file2) throws PlateformeException {
        try {
            MDC.put("date", DateTimeFormatter.ISO_DATE.format(dateAnalyse));

            BackupLog backupLog = new BackupLog();
            backupLog.setTimestamp(dateAnalyse);

            Map<String, FailableConsumer<List<Ligne>, Exception>> map = Map.ofEntries(entry("Backup rsync", (lignes) -> {
                parseLogNasbackup(repertoireLog.resolve(file2), backupLog, lignes);
            }), entry("fileutils", (lignes) -> {
                parseLogOvh(repertoireLog.resolve(file2), backupLog, lignes);
                //}),entry("rclone", (lignes) -> {
                //parseLogGithub(repertoireLog.resolve(file2), backupLog, lignes);
                //}),entry("robocopy", (lignes) -> {
                //parseLogGithub(repertoireLog.resolve(file2), backupLog, lignes);
            }), entry("Backup jbackup", (lignes) -> {
                parseLogGithub(repertoireLog.resolve(file2), backupLog, lignes);
            }), entry("Resticprofile full2 backup", (lignes) -> {
                parseLogRestic(repertoireLog.resolve(file2), backupLog, lignes, "full2 backup");
            }), entry("Resticprofile full2 forget", (lignes) -> {
                parseLogRestic(repertoireLog.resolve(file2), backupLog, lignes, "full2 forget");
            }), entry("Resticprofile nasbackup backup", (lignes) -> {
                parseLogRestic(repertoireLog.resolve(file2), backupLog, lignes, "nasbackup backup");
            }), entry("Resticprofile nasbackup forget", (lignes) -> {
                parseLogRestic(repertoireLog.resolve(file2), backupLog, lignes, "nasbackup forget");
            }), entry("Resticprofile raspberrypi2 backup", (lignes) -> {
                parseLogRestic(repertoireLog.resolve(file2), backupLog, lignes, "raspberrypi2 backup");
            }), entry("Resticprofile raspberrypi2 forget", (lignes) -> {
                parseLogRestic(repertoireLog.resolve(file2), backupLog, lignes, "raspberrypi2 forget");
            }), entry("Resticprofile rclone", (lignes) -> {
                parseLogRestic(repertoireLog.resolve(file2), backupLog, lignes, "rclone");
            }), entry("Backup gobackup", (lignes) -> {
                parseLogGoBackup(repertoireLog.resolve(file2), backupLog, lignes);
            }));

            parse2(repertoireLog.resolve(file2), map);

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
            metricsPusher.pushMetrics(backupLog);
        } finally {
            MDC.remove("date");
        }
    }

    private void parse2(Path fichier, Map<String, FailableConsumer<List<Ligne>, Exception>> map) throws PlateformeException {

        try (var lignes = Files.lines(fichier, StandardCharsets.ISO_8859_1)) {

            Iterable<String> iterableStream = lignes::iterator;
            Map<String, List<Ligne>> map2 = new HashMap<>();
            List<Ligne> lignes2 = null;
            String cle = null;
            LocalDateTime derniereDate = null;

            for (var ligne : iterableStream) {
                if (ligne != null && ligne.length() > 20) {
                    var s = ligne;
                    s = removeUTF16_LEBOM(s);
                    var s2 = StringUtils.substring(s, 22);
                    s = StringUtils.substring(s, 0, 21);
                    LocalDateTime dateTime = derniereDate;
                    if (s.startsWith("[") && s.endsWith("]")) {
                        s = s.substring(1, s.length() - 1);
                        if (debutDate(s)) {
                            try {
                                var date = LocalDateTime.parse(s, formatter4);
                                dateTime = date;
                                derniereDate = date;
                            } catch (DateTimeParseException e) {
                                log.error("Erreur pour parser la date {}", ligne, e);
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(s2)) {
                        s2 = s2.trim();
                        if (s2.startsWith(new String("Début de la tache ".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))) {
                            var s3 = s2.substring(19);
                            if (map.containsKey(s3)) {
                                lignes2 = new ArrayList<>();
                                map2.put(s3, lignes2);
                                cle = s3;
                                lignes2.add(new Ligne(dateTime, ligne));
                            } else {
                                log.error("Erreur pour trouver la tache {}", s3);
                            }
                        } else if (s2.startsWith("Fin de la tache ")) {
                            var s3 = s2.substring(16);
                            if (lignes2 != null) {
                                lignes2.add(new Ligne(dateTime, ligne));
                            }
                            if (!Objects.equals(s3, cle)) {
                                log.error("Erreur la tache de fin ne correspond pas : {} != {}", s3, cle);
                            } else {
                                if (map.containsKey(cle)) {
                                    map.get(cle).accept(lignes2);
                                }
                            }
                            cle = null;
                            lignes2 = null;
                        } else if (lignes2 != null) {
                            lignes2.add(new Ligne(dateTime, ligne));
                        }
                    }
                    log.debug("ligne: {}", ligne);
                }
            }
        } catch (IOException e) {
            log.error("Erreur pour lire le fichier {}", fichier, e);
        } catch (Exception e) {
            log.error("Erreur pour lire le fichier {}", fichier, e);
        }


    }

    private void parseLogNasbackup(Path file, BackupLog backupLog, List<Ligne> lignes2) {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        boolean debut = false;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = lignes2.stream()) {
            Iterable<Ligne> iterableStream = lignes::iterator;
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne2 : iterableStream) {
                    var ligne = ligne2.ligne();
                    var date = ligne2.date();
                    MDC.put("noLigne", "" + noLigne);
                    LocalDateTime dateTime = null;
                    if (date != null) {
                        dateTime = date;
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
        }

        log.info("date debut={}, date fin={}", dateDebut, dateFin);

        NasbackupLog nasbackupLog = new NasbackupLog();
        nasbackupLog.setNomFichier(file.getFileName().toString());
        dateDebut.ifPresent(nasbackupLog::setDateDebut);
        dateFin.ifPresent(nasbackupLog::setDateFin);
        backupLog.setNasbackupLog(nasbackupLog);
    }


    private void parseLogOvh(Path file, BackupLog backupLog, List<Ligne> lignes2) {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        Optional<LocalDateTime> dateDebutRClone = Optional.empty();
        boolean debut = false;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = lignes2.stream()) {
            Iterable<Ligne> iterableStream = lignes::iterator;
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne2 : iterableStream) {
                    var ligne = ligne2.ligne();
                    var date = ligne2.date();
                    LocalDateTime dateTime = null;
                    MDC.put("noLigne", "" + noLigne);
                    if (date != null) {
                        dateTime = date;
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
        }

        log.info("date debut={}, date fin={}", dateDebut, dateFin);

        var ovhBackupLog = new OvhBackupLog();
        ovhBackupLog.setNomFichier(file.getFileName().toString());
        dateDebut.ifPresent(ovhBackupLog::setDateDebut);
        dateFin.ifPresent(ovhBackupLog::setDateFin);
        dateDebutRClone.ifPresent(ovhBackupLog::setDateDebutRclone);
        backupLog.setOvhBackupLog(ovhBackupLog);
    }

    private void parseLogGithub(Path file, BackupLog backupLog, List<Ligne> lignes2) throws PlateformeException {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        Optional<LocalDateTime> dateDebutRClone = Optional.empty();
        boolean debut = false;
        int nbErreur = 0;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = lignes2.stream()) {
            Iterable<Ligne> iterableStream = lignes::iterator;
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne2 : iterableStream) {
                    var ligne = ligne2.ligne();
                    var date = ligne2.date();
                    LocalDateTime dateTime = null;
                    MDC.put("noLigne", "" + noLigne);
                    if (date != null) {
                        dateTime = date;
                    } else {
                        log.warn("Le format de la date n'est pas bon à la ligne {} (s={})", noLigne, ligne);
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
        return s != null && !s.isEmpty() && (s.startsWith("0") || s.startsWith("1") || s.startsWith("2") || s.startsWith("3"));
    }

    private void parseLogRestic(Path file, BackupLog backupLog, List<Ligne> lignes2, String nom) {
        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        Optional<LocalDateTime> dateDebutFull2Backup = Optional.empty(), dateDebutFull2Forget = Optional.empty();
        Optional<LocalDateTime> dateDebutNasbackupBackup = Optional.empty(), dateDebutNasbackupForget = Optional.empty();
        Optional<LocalDateTime> dateDebutRaspberryBackup = Optional.empty(), dateDebutRaspberryForget = Optional.empty();
        Optional<LocalDateTime> dateDebutRclone = Optional.empty();
        boolean debut = false;
        int nbErreur = 0;
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = lignes2.stream()) {
            Iterable<Ligne> iterableStream = lignes::iterator;
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne2 : iterableStream) {
                    var ligne = ligne2.ligne();
                    var date = ligne2.date();
                    LocalDateTime dateTime = null;
                    MDC.put("noLigne", "" + noLigne);
                    if (date != null) {
                        dateTime = date;
                    } else {
                        log.warn("Le format de la date n'est pas bon à la ligne {} (s={})", noLigne, ligne);
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
                    noLigne++;
                }
            } finally {
                MDC.remove("file");
                MDC.remove("noLigne");
            }
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
        resticLog.setNom(nom);
        backupLog.addResticLog(resticLog);
    }


    private void parseLogGoBackup(Path file, BackupLog backupLog, List<Ligne> lignes2) throws PlateformeException {

        Optional<LocalDateTime> dateDebut = Optional.empty(), dateFin = Optional.empty();
        boolean debut = false;
        int nbErreur = 0;
        Map<String, GoBackupVo> map = new LinkedHashMap<>();
        GoBackupVo goBackupVo = new GoBackupVo();
        final String MESDOC = "mesdoc", WEB = "web", DOCUMENT = "document", PROJETS = "projets";
        log.info("analyse du fichier: {}", file.getFileName());
        try (var lignes = lignes2.stream()) {
            Iterable<Ligne> iterableStream = lignes::iterator;
            try {
                MDC.put("file", file.getFileName().toString());
                int noLigne = 1;
                for (var ligne2 : iterableStream) {
                    var ligne = ligne2.ligne();
                    var date = ligne2.date();
                    LocalDateTime dateTime = null;
                    MDC.put("noLigne", "" + noLigne);
                    if (date != null) {
                        dateTime = date;
                    } else {
                        log.warn("Le format de la date n'est pas bon à la ligne {} (s={})", noLigne, ligne);
                    }
                    if (!debut && dateTime != null) {
                        dateDebut = Optional.of(dateTime);
                        debut = true;
                    }

                    if (dateTime != null) {
                        dateFin = Optional.of(dateTime);

                        if (!map.containsKey(MESDOC) && ligne != null &&
                                ligne.contains("traitement de mesdoc")) {
                            goBackupVo = new GoBackupVo();
                            goBackupVo.setDateDebutMesDoc(Optional.of(dateTime));
                            map.put(MESDOC, goBackupVo);
                        } else if (!map.containsKey(WEB) && ligne != null &&
                                ligne.contains("traitement de web")) {
                            goBackupVo = new GoBackupVo();
                            goBackupVo.setDateDebutMesDoc(Optional.of(dateTime));
                            map.put(WEB, goBackupVo);
                        } else if (!map.containsKey(DOCUMENT) && ligne != null &&
                                ligne.contains("traitement de document")) {
                            goBackupVo = new GoBackupVo();
                            goBackupVo.setDateDebutMesDoc(Optional.of(dateTime));
                            map.put(DOCUMENT, goBackupVo);
                        } else if (!map.containsKey(PROJETS) && ligne != null &&
                                ligne.contains("traitement de projets")) {
                            goBackupVo = new GoBackupVo();
                            goBackupVo.setDateDebutMesDoc(Optional.of(dateTime));
                            map.put(PROJETS, goBackupVo);
                        }
                        if (goBackupVo.getDateDebutCompressionMesdoc().isEmpty() && ligne != null &&
                                ligne.contains("compression ...")) {
                            goBackupVo.setDateDebutCompressionMesdoc(Optional.of(dateTime));
                        }
                        if (goBackupVo.getDateDebutCompressionMesdoc().isEmpty() && ligne != null &&
                                ligne.contains("cryptage de ")) {
                            goBackupVo.setDateDebutCryptageMesdoc(Optional.of(dateTime));
                        }
                        if (goBackupVo.getDateDebutEnregistrementHash().isEmpty() && ligne != null &&
                                ligne.contains("cryptage termin")) {
                            goBackupVo.setDateDebutEnregistrementHash(Optional.of(dateTime));
                        }
                        if (ligne != null &&
                                ligne.contains("* fin du backup gobackup : ")) {
                            break;
                        }
                    }
                }
            } finally {
                MDC.remove("file");
                MDC.remove("noLigne");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur pour traiter le fichier " + file, e);
        }

        var goBackupLog = new GoBackupLog();
        goBackupLog.setNomFichier(file.getFileName().toString());
        dateDebut.ifPresent(goBackupLog::setDateDebut);
        dateFin.ifPresent(goBackupLog::setDateFin);
        goBackupLog.setNbErreur(nbErreur);
        if (map.containsKey(MESDOC)) {
            map.get(MESDOC).getDateDebutMesDoc().ifPresent(goBackupLog::setDateDebutMesdoc);
            map.get(MESDOC).getDateDebutCompressionMesdoc().ifPresent(goBackupLog::setDateDebutCompressionMesdoc);
            map.get(MESDOC).getDateDebutCryptageMesdoc().ifPresent(goBackupLog::setDateDebutCryptageMesdoc);
            map.get(MESDOC).getDateDebutEnregistrementHash().ifPresent(goBackupLog::setDateDebutEnregistrementHashMesdoc);
        }
        if (map.containsKey(WEB)) {
            map.get(WEB).getDateDebutMesDoc().ifPresent(goBackupLog::setDateDebutWeb);
            map.get(WEB).getDateDebutCompressionMesdoc().ifPresent(goBackupLog::setDateDebutCompressionWeb);
            map.get(WEB).getDateDebutCryptageMesdoc().ifPresent(goBackupLog::setDateDebutCryptageWeb);
            map.get(WEB).getDateDebutEnregistrementHash().ifPresent(goBackupLog::setDateDebutEnregistrementHashWeb);
            map.get(WEB).getDateDebutMesDoc().ifPresent(goBackupLog::setDateFinMesdoc);
        }
        if (map.containsKey(DOCUMENT)) {
            map.get(DOCUMENT).getDateDebutMesDoc().ifPresent(goBackupLog::setDateDebutDocument);
            map.get(DOCUMENT).getDateDebutCompressionMesdoc().ifPresent(goBackupLog::setDateDebutCompressionDocument);
            map.get(DOCUMENT).getDateDebutCryptageMesdoc().ifPresent(goBackupLog::setDateDebutCryptageDocument);
            map.get(DOCUMENT).getDateDebutEnregistrementHash().ifPresent(goBackupLog::setDateDebutEnregistrementHashDocument);
            map.get(DOCUMENT).getDateDebutMesDoc().ifPresent(goBackupLog::setDateFinWeb);
        }
        if (map.containsKey(PROJETS)) {
            map.get(PROJETS).getDateDebutMesDoc().ifPresent(goBackupLog::setDateDebutProjets);
            map.get(PROJETS).getDateDebutCompressionMesdoc().ifPresent(goBackupLog::setDateDebutCompressionProjets);
            map.get(PROJETS).getDateDebutCryptageMesdoc().ifPresent(goBackupLog::setDateDebutCryptageProjets);
            map.get(PROJETS).getDateDebutEnregistrementHash().ifPresent(goBackupLog::setDateDebutEnregistrementHashProjets);
            map.get(PROJETS).getDateDebutMesDoc().ifPresent(goBackupLog::setDateFinDocument);
            dateFin.ifPresent(goBackupLog::setDateFinProjets);
        }
        backupLog.setGoBackupLog(goBackupLog);
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


    private String removeUTF16_LEBOM(String s) {
        if (s.startsWith(BOM_UTF_16LE)) {
            s = s.substring(1);
        }
        return s;
    }
}
