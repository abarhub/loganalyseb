package org.loganalyseb.loganalyseb.service;

import com.google.common.base.CharMatcher;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;
import org.loganalyseb.loganalyseb.model.BackupLog;
import org.loganalyseb.loganalyseb.properties.PushGatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MetricsPusher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsPusher.class);

    private final PrometheusMeterRegistry prometheusRegistry;
    private final PushGateway pushGateway;
    private final String jobName;
    private final boolean active;
    private final PushGatewayProperties pushGatewayProperties;

    private static Gauge dataProcessedInBytes /*= Gauge.build()
            .name("duree_backup_nasbackup_total_secondes")
            .help("duree de backup de nasbackup total en secondes")
            .unit("SECONDS")
            .register()*/;

    public MetricsPusher(PrometheusMeterRegistry prometheusRegistry, PushGatewayProperties pushGatewayProperties) {
        this.prometheusRegistry = prometheusRegistry;
        this.pushGateway = new PushGateway(pushGatewayProperties.getUrl());
        this.jobName = pushGatewayProperties.getJobName();
        this.active = pushGatewayProperties.isActif();
        this.pushGatewayProperties = pushGatewayProperties;
    }

    public void pushMetrics(BackupLog backupLog) {

        if (active) {
            try {

                Map<String, String> groupingKey = new HashMap<>();
                groupingKey.put("instance", "loganalyseb");
                groupingKey.put("date", backupLog.getTimestamp().toString());

                CollectorRegistry registry = new CollectorRegistry();

                addTotalNasbackup(backupLog, registry);
                addTotalOvhbackup(backupLog, registry);
                addTotalGithub(backupLog, registry);
                addTotalRestic(backupLog, registry);
                addTotalGoBackup(backupLog, registry);

                var jobName = this.jobName;
                pushGateway.pushAdd(registry, jobName, groupingKey);

                LOGGER.info("Métriques envoyées au Pushgateway");
            } catch (IOException e) {
                LOGGER.error("Erreur pour envoyer les metrics à Pushgateway", e);
            }
        } else {
            LOGGER.info("les metrics ne sont pas activés");
        }
    }

    private void addTotalNasbackup(BackupLog backupLog, CollectorRegistry registry) {
        if (backupLog.getNasbackupLog() != null) {
            var duree = backupLog.getNasbackupLog().getDureeTotaleSecondes();
            var dataProcessedInBytes = Gauge.build()
                    .name("duree_backup_nasbackup_total_secondes")
                    .help("duree de backup de nasbackup total en secondes")
                    .unit("SECONDS")
                    .register(registry);

            dataProcessedInBytes.set(duree);
        }
    }

    private void addTotalOvhbackup(BackupLog backupLog, CollectorRegistry registry) {
        if (backupLog.getOvhBackupLog() != null) {
            var duree = backupLog.getOvhBackupLog().getDureeTotaleSecondes();
            var dataProcessedInBytes = Gauge.build()
                    .name("duree_backup_ovhbackup_total_secondes")
                    .help("duree de backup de ovhbackup total en secondes")
                    .unit("SECONDS")
                    .register(registry);

            dataProcessedInBytes.set(duree);
            duree = backupLog.getOvhBackupLog().getDureeFileutilsSecondes();
            dataProcessedInBytes = Gauge.build()
                    .name("duree_backup_fileutils_total_secondes")
                    .help("duree de backup de fileutils total en secondes")
                    .unit("SECONDS")
                    .register(registry);

            dataProcessedInBytes.set(duree);
            duree = backupLog.getOvhBackupLog().getDureeRcloneSecondes();
            dataProcessedInBytes = Gauge.build()
                    .name("duree_backup_rclone_total_secondes")
                    .help("duree de backup de rclone total en secondes")
                    .unit("SECONDS")
                    .register(registry);

            dataProcessedInBytes.set(duree);
        }
    }


    private void addTotalGithub(BackupLog backupLog, CollectorRegistry registry) {
        if (backupLog.getGithubLog() != null) {
            var duree = backupLog.getGithubLog().getDureeTotaleSecondes();
            var dataProcessedInBytes = Gauge.build()
                    .name("duree_backup_github_total_secondes")
                    .help("duree de backup de github total en secondes")
                    .unit("SECONDS")
                    .register(registry);

            dataProcessedInBytes.set(duree);

            var nbErreur = backupLog.getGithubLog().getNbErreur();
            var nbErreurCounter = Counter.build()
                    .name("backup_nb_erreur_github")
                    .help("backup nb erreur github")
                    .register(registry);
            nbErreurCounter.inc(nbErreur);
        }
    }


    private void addTotalRestic(BackupLog backupLog, CollectorRegistry registry) {
        if (backupLog.getResticLogList() != null) {

            for (var resticLog : backupLog.getResticLogList()) {

                String nom = resticLog.getNom();
                String nom2 = remplacerCaracteresSpeciaux(nom);

                var duree = resticLog.getDureeTotaleSecondes();
                var dataProcessedInBytes = Gauge.build()
                        .name("duree_backup_restic_" + nom2 + "_total_secondes")
                        .help("duree de backup de restic " + nom + " total en secondes")
                        .unit("SECONDS")
                        .register(registry);

                dataProcessedInBytes.set(duree);
            }


        }
    }

    private String remplacerCaracteresSpeciaux(String input) {
        // 1. Définir ce qui est acceptable (lettre OU chiffre)
        CharMatcher lettresEtChiffres = CharMatcher.javaLetterOrDigit();

        // 2. Nier le matcher pour cibler ce qui n'est PAS une lettre ou un chiffre
        CharMatcher caracteresARemplacer = lettresEtChiffres.negate();

        // 3. Effectuer le remplacement
        return caracteresARemplacer.replaceFrom(input, '_');
    }

    private void addTotalGoBackup(BackupLog backupLog, CollectorRegistry registry) {
        if (backupLog.getGoBackupLog() != null) {
            var duree = backupLog.getGoBackupLog().getDureeTotaleSecondes();
            var dataProcessedInBytes = Gauge.build()
                    .name("duree_backup_gobackup_total_secondes")
                    .help("duree de backup de gobackup total en secondes")
                    .unit("SECONDS")
                    .register(registry);

            dataProcessedInBytes.set(duree);
        }
    }
}
