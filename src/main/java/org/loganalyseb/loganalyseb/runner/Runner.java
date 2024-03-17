package org.loganalyseb.loganalyseb.runner;

import lombok.extern.slf4j.Slf4j;
import org.loganalyseb.loganalyseb.service.AnalyseService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@Slf4j
public class Runner implements ApplicationRunner {

    private final AnalyseService analyseService;

    public Runner(AnalyseService analyseService) {
        this.analyseService = analyseService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.atInfo().log("Démarrage ...");
        analyseService.analyse();
        log.atInfo().log("Fin");
    }


}
