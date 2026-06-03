package com.devrenno.bookland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BooklandApplication {

    public static void main(String[] args) {
        SpringApplication.run(BooklandApplication.class, args);
    }
}
