package org.loganalyseb.loganalyseb.config;

import org.loganalyseb.loganalyseb.properties.AppProperties;
import org.loganalyseb.loganalyseb.runner.Runner;
import org.loganalyseb.loganalyseb.service.AnalyseService;
import org.loganalyseb.loganalyseb.service.DatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {


    @Bean
    public AnalyseService analyseService(AppProperties appProperties, DatabaseService databaseService) {
        return new AnalyseService(appProperties, databaseService);
    }

    @Bean
    public Runner runner(AnalyseService analyseService) {
        return new Runner(analyseService);
    }

    @Bean
    public DatabaseService databaseService(AppProperties appProperties) {
        return new DatabaseService(appProperties);
    }

}
