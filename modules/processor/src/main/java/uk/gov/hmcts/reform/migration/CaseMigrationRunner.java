package uk.gov.hmcts.reform.migration;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@SpringBootApplication
@RequiredArgsConstructor
public class CaseMigrationRunner implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger("ccd-migration-info");
    @Value("${migration.idam.username}")
    private String idamUsername;
    @Value("${migration.idam.password}")
    private String idamPassword;
    @Value("${migration.caseId}")
    private String ccdCaseId;
    @Value("${migration.dryrun}")
    private boolean dryrun;
    @Value("${migration.pageSize}")
    private int pageSize;
    @Value("${migration.maxCasesToProcess}")
    private int maxCasesToProcess;
    @Value("${migration.numThreads}")
    private int numThreads;
    private final IdamClient idamClient;
    private final CaseMigrationProcessor caseMigrationProcessor;

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            String userToken = idamClient.authenticateUser(idamUsername, idamPassword);

            MigrationPageParams pageParams = new MigrationPageParams(pageSize, maxCasesToProcess);

            StopWatch stopWatch = StopWatch.createStarted();

            if (ccdCaseId != null && !ccdCaseId.isBlank()) {
                log.info("Data migration of single case started");
                caseMigrationProcessor.processSingleCase(userToken, ccdCaseId, dryrun);
            } else {
                log.info("Data migration of cases started");
                caseMigrationProcessor.fetchAndProcessCases(userToken, dryrun, numThreads, pageParams);
            }

            stopWatch.stop();

            log.info("-----------------------------------------");
            log.info("Total number of cases: {}", caseMigrationProcessor.getTotalCases());
            log.info("Total number of processed cases: {}", caseMigrationProcessor.getMigratedCases().size()
                + caseMigrationProcessor.getFailedCases().size());
            log.info("Total number of migrations performed: {}", caseMigrationProcessor.getMigratedCases().size());
            log.info("-----------------------------------------");
            log.info("Number of migrated cases: {}", caseMigrationProcessor.getMigratedCases().size());
            log.info("Migrated cases: {} ", !caseMigrationProcessor.getMigratedCases().isEmpty()
                ? caseMigrationProcessor.getMigratedCases()
                : "NONE");
            log.info("Number of failed cases: {}", caseMigrationProcessor.getFailedCases().size());
            log.info("Failed cases: {} ", !caseMigrationProcessor.getFailedCases().isEmpty()
                ? caseMigrationProcessor.getFailedCases()
                : "NONE");
            log.info("-----------------------------------------");
            log.info("Data migration completed in: {} minutes ({} seconds).",
                stopWatch.getTime(TimeUnit.MINUTES), stopWatch.getTime(TimeUnit.SECONDS));
            log.info("-----------------------------------------");
        } catch (Throwable e) {
            log.error("Migration failed with the following reason: {}", e.getMessage(), e);
        }
    }
}
