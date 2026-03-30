package com.unicornt.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Punto de entrada de la aplicación Spring Boot.
 * Extiende SpringBootServletInitializer para permitir el despliegue como WAR
 * en un Tomcat externo (10.1+).
 */
@SpringBootApplication
public class StoreApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(StoreApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }
}
