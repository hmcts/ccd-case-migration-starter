package uk.gov.hmcts.reform.migration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
@PropertySource("classpath:application.properties")
public class CaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private CaseMigrationProcessor caseMigrationProcessor;

    @Value("${migration.caseType}")
    private String caseType;

    @Value("${migration.caseId}")
    private String ccdCaseId;

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            if (ccdCaseId != null && !ccdCaseId.isBlank()) {
                log.info("Data migration of single case started");
                caseMigrationProcessor.migrateSingleCase(caseType, ccdCaseId);
            } else {
                log.info("Data migration of cases started");
                caseMigrationProcessor.migrateCases(caseType);
            }

        } catch (Exception e) {
            log.error("Migration failed with the following reason: {}", e.getMessage(), e);
        } finally {
            stopWatch.stop();
            log.info("-----------------------------------------");
            log.info("Data migration completed in: {} minutes ({} seconds).",
                     stopWatch.getTime(TimeUnit.MINUTES), stopWatch.getTime(TimeUnit.SECONDS)
            );
            log.info("-----------------------------------------");
        }
    }
}
