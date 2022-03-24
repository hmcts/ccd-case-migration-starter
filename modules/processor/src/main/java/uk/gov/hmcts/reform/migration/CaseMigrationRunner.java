package uk.gov.hmcts.reform.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@Slf4j
@SpringBootApplication
@PropertySource("classpath:application.properties")
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
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private CaseMigrationProcessor caseMigrationProcessor;

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            String userToken = idamClient.authenticateUser(idamUsername, idamPassword);
            log.debug("User token: {}", userToken);
            String userId = idamClient.getUserDetails(userToken).getId();
            log.debug("User ID: {}", userId);

            if (ccdCaseId != null && !ccdCaseId.isBlank()) {
                log.info("Data migration of single case started");
                caseMigrationProcessor.processSingleCase(userToken, ccdCaseId, dryrun);
            } else {
                log.info("Data migration of cases between {} and {} started", startDate, endDate);
                caseMigrationProcessor.processAllCases(userToken, userId, startDate, endDate, dryrun);
            }

            log.info("-----------------------------------------");
            log.info("Data migration completed");
            log.info("-----------------------------------------");
            log.info("Total number of processed cases: {}", caseMigrationProcessor.getMigratedCases().size() + caseMigrationProcessor.getFailedCases().size());
            log.info("Total number of migrations performed: {}", caseMigrationProcessor.getMigratedCases().size());
            log.info("-----------------------------------------");
            log.info("Migrated cases: {} ", !caseMigrationProcessor.getMigratedCases().isEmpty() ? caseMigrationProcessor.getMigratedCases() : "NONE");
            log.info("Failed cases: {}", !caseMigrationProcessor.getFailedCases().isEmpty() ? caseMigrationProcessor.getFailedCases() : "NONE");
        } catch (Throwable e) {
            log.error("Migration failed with the following reason: {}", e.getMessage(), e);
        }
    }
}
