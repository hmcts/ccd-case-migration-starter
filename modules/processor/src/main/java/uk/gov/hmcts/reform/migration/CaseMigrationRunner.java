package uk.gov.hmcts.reform.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.User;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
@PropertySource("classpath:application.properties")
public class CaseMigrationRunner implements CommandLineRunner {

    @Value("${migration.idam.username}")
    private String idamUsername;
    @Value("${migration.idam.password}")
    private String idamPassword;
    @Value("${migration.caseId:}")
    private String ccdCaseId;

    @Value("${migration.caseIds:}")
    private String ccdCaseIds;

    @Value("${migration.pageByPage:true}")
    private Boolean pageByPage;
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
            UserDetails userDetails = idamClient.getUserDetails(userToken);
            String userId = userDetails.getId();
            log.debug("User ID: {}", userId);
            User user = new User(userToken, userDetails);

            if (ccdCaseId != null && !ccdCaseId.isBlank()) {
                log.info("Data migration of single case started");
                caseMigrationProcessor.processSingleCase(user, ccdCaseId);
            } else if (ccdCaseIds != null && !ccdCaseIds.isBlank()) {
                log.info("Data migration of list of cases started");
                List<String> caseIdsList = Stream.of(ccdCaseIds.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
                log.info("Total case ids provided: " + caseIdsList.size());
                caseIdsList.stream().forEach(caseId -> {
                    caseMigrationProcessor.processSingleCase(user, caseId);
                });

            } else {
                if (pageByPage) {
                    log.info("Data migration of all cases page by page started");
                    caseMigrationProcessor.processAllCasesPageByPage(user);
                } else {
                    log.info("Data migration of all cases started");
                    caseMigrationProcessor.processAllCases(user);
                }
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
