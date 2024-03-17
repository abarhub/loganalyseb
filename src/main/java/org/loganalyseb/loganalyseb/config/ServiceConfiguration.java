package org.loganalyseb.loganalyseb.config;

import org.loganalyseb.loganalyseb.properties.AppProperties;
import org.loganalyseb.loganalyseb.runner.Runner;
import org.loganalyseb.loganalyseb.service.AnalyseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {


    @Bean
    public AnalyseService analyseService(AppProperties appProperties) {
        return new AnalyseService(appProperties);
    }

    @Bean
    public Runner runner(AnalyseService analyseService) {
        return new Runner(analyseService);
    }

}
