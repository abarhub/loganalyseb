package org.loganalyseb.loganalyseb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.loganalyseb.loganalyseb.db.Donnees;
import org.loganalyseb.loganalyseb.exception.PlateformeException;
import org.loganalyseb.loganalyseb.properties.AppProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Slf4j
public class DatabaseService {

    private final AppProperties appProperties;

    private ObjectMapper objectMapper;

    private Donnees donnees;

    public DatabaseService(AppProperties appProperties) {
        this.appProperties = appProperties;
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.findAndRegisterModules();
    }

    @PostConstruct
    public void chargeDatabase() throws IOException {
        if (StringUtils.isNotBlank(appProperties.getFichierDonnees())) {
            Path p = Path.of(appProperties.getFichierDonnees());
            if (Files.exists(p)) {
                try (var reader = Files.newBufferedReader(p)) {
                    donnees = objectMapper.readerFor(Donnees.class).readValue(reader);
                }
            } else {
                log.warn("Le fichier {} n'existe pas", p);
                donnees=new Donnees();
            }
        } else {
            log.warn("La propriété file n'est pas configurée");
            donnees=new Donnees();
        }
    }

    public LocalDate getDaterDernier() {
        return donnees.getDerniereDate();
    }

    public void setDerniereDate(LocalDate date) throws PlateformeException {
        donnees.setDerniereDate(date);
        Path p = Path.of(appProperties.getFichierDonnees());
        try (var writer = Files.newBufferedWriter(p)) {
            objectMapper.writerFor(Donnees.class).writeValue(writer, donnees);
        } catch (IOException e) {
            throw new PlateformeException("Erreur pour enregistrer le fichier " + p, e);
        }
    }

}
