package com.bidb.personetakip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class PersonetakipApplication {

    public static void main(String[] args) {
        // Set default timezone to Turkey
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
        SpringApplication.run(PersonetakipApplication.class, args);
    }

}
