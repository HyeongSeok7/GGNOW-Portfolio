package com.project.web;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            WebApplication.class,
            args
        );
    }

    @Bean
    CommandLineRunner test(
            DataSource dataSource
    ) {
        return args -> {

            System.out.println(
                "DB URL = "
                + dataSource.getConnection()
                            .getMetaData()
                            .getURL()
            );

            System.out.println(
                "DB USER = "
                + dataSource.getConnection()
                            .getMetaData()
                            .getUserName()
            );

            System.out.println(
                "DB CATALOG = "
                + dataSource.getConnection()
                            .getCatalog()
            );
        };
    }
}