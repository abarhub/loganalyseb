package org.loganalyseb.loganalyseb.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private String repertoire;

    private LocalDate dateAnalyse;

    private String fichierDonnees;

    private PushGatewayProperties pushGateway;

    private List<FileProperties> files;

}
