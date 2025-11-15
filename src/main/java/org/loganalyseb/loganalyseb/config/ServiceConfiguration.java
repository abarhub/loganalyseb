package org.loganalyseb.loganalyseb.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.loganalyseb.loganalyseb.properties.AppProperties;
import org.loganalyseb.loganalyseb.runner.Runner;
import org.loganalyseb.loganalyseb.service.AnalyseService;
import org.loganalyseb.loganalyseb.service.DatabaseService;
import org.loganalyseb.loganalyseb.service.MetricsPusher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {


    @Bean
    public AnalyseService analyseService(AppProperties appProperties, DatabaseService databaseService,
                                         MetricsPusher metricsPusher) {
        return new AnalyseService(appProperties, databaseService, metricsPusher);
    }

    @Bean
    public Runner runner(AnalyseService analyseService) {
        return new Runner(analyseService);
    }

    @Bean
    public DatabaseService databaseService(AppProperties appProperties) {
        return new DatabaseService(appProperties);
    }

    @Bean
    public MetricsPusher metricsPusher(AppProperties appProperties) {
        return new MetricsPusher(appProperties.getPushGateway(),appProperties.getFiles());
    }

}
