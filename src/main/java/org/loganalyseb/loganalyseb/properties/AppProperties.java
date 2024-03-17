package org.loganalyseb.loganalyseb.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private String repertoire;

    private LocalDate dateAnalyse;

}
