package com.bvmt.decision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de la plateforme d'aide à la décision BVMT.
 *
 * Active :
 *  - @EnableScheduling : pour les jobs ETL d'import des cours
 *  - @EnableAsync      : pour les calculs d'indicateurs en parallèle
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class DecisionPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DecisionPlatformApplication.class, args);
    }
}
