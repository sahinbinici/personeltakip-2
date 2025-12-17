package com.bidb.personetakip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersonetakipApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonetakipApplication.class, args);
    }

}
