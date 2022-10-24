package uk.gov.hmcts.reform.migration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import uk.gov.hmcts.reform.idam.client.models.User;
import uk.gov.hmcts.reform.migration.repository.IdamRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
@PropertySource("classpath:application.yml")
public class CaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private CaseMigrationProcessor caseMigrationProcessor;
    @Autowired
    private MigrationProperties migrationProperties;
    @Autowired
    private IdamRepository idamRepository;

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            User user = idamRepository.authenticateUser();
            log.info("User authentication successful.");
            if (migrationProperties.getCaseIds() != null && !migrationProperties.getCaseIds().isBlank()) {
                log.info("Data migration of cases started: " + migrationProperties.getCaseIds());
                List<String> caseIdsList = Stream.of(migrationProperties.getCaseIds().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
                caseIdsList
                    .stream()
                    .map(String::trim)
                    .forEach(caseId -> {
                        caseMigrationProcessor.migrateSingleCase(user, caseId);
                    });
            } else {
                log.info("Data migration of cases started");
                caseMigrationProcessor.process(user);
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
