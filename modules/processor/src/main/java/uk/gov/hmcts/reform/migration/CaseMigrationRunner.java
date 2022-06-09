package uk.gov.hmcts.reform.migration;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class CaseMigrationRunner implements CommandLineRunner {

    @Value("${migration.idam.username}")
    private String idamUsername;
    @Value("${migration.idam.password}")
    private String idamPassword;
    @Value("${migration.caseId}")
    private String ccdCaseId;
    @Value("${migration.startDate}")
    private String startDate;
    @Value("${migration.endDate}")
    private String endDate;
    @Value("${migration.dryrun}")
    private boolean dryrun;

    private final IdamClient idamClient;
    private final CaseMigrationProcessor caseMigrationProcessor;

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            String userToken = idamClient.authenticateUser(idamUsername, idamPassword);

            StopWatch stopWatch = StopWatch.createStarted();

            if (ccdCaseId != null && !ccdCaseId.isBlank()) {
                log.info("Data migration of single case started");
                caseMigrationProcessor.processSingleCase(userToken, ccdCaseId, dryrun);
            } else {
                log.info("Data migration of cases between {} and {} started", startDate, endDate);
                caseMigrationProcessor.processAllCases(userToken, startDate, endDate, dryrun);
            }

            stopWatch.stop();

            log.info("-----------------------------------------");
            log.info("Total number of cases: {}", caseMigrationProcessor.getTotalCases());
            log.info("Total number of processed cases: {}", caseMigrationProcessor.getMigratedCases().size() + caseMigrationProcessor.getFailedCases().size());
            log.info("Total number of migrations performed: {}", caseMigrationProcessor.getMigratedCases().size());
            log.info("-----------------------------------------");
            log.info("Migrated cases: {} ", !caseMigrationProcessor.getMigratedCases().isEmpty() ? caseMigrationProcessor.getMigratedCases() : "NONE");
            log.info("Failed cases: {}", !caseMigrationProcessor.getFailedCases().isEmpty() ? caseMigrationProcessor.getFailedCases() : "NONE");
            log.info("-----------------------------------------");
            log.info("Data migration completed in: {} minutes.", stopWatch.getTime(TimeUnit.MINUTES));
            log.info("-----------------------------------------");
        } catch (Throwable e) {
            log.error("Migration failed with the following reason: {}", e.getMessage(), e);
        }
    }
}
