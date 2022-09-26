package uk.gov.hmcts.reform.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@SpringBootApplication
@PropertySource("classpath:application.properties")
public class CaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private CaseMigrationProcessor caseMigrationProcessor;

    @Value("${migration.caseType}")
    private String caseType;

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            caseMigrationProcessor.migrateCases(caseType);
        } catch (Exception e) {
            log.error("Migration failed with the following reason: {}", e.getMessage(), e);
        }
    }
}
