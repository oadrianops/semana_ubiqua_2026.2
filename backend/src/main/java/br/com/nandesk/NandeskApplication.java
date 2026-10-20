package br.com.nandesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NandeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(NandeskApplication.class, args);
    }
}
