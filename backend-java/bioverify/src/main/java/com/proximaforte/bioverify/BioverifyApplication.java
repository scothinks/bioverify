package com.proximaforte.bioverify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; 

@SpringBootApplication
@EnableAsync 
public class BioverifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BioverifyApplication.class, args);
    }

}   