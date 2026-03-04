package com.mancalagame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MancalaGameSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(MancalaGameSpringbootApplication.class, args);
    }

}
