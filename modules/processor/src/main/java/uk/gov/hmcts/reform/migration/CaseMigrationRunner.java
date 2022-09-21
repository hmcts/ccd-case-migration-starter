package uk.gov.hmcts.reform.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.repository.IdamRepository;

import java.util.List;

@Slf4j
@SpringBootApplication
@PropertySource("classpath:application.properties")
public class CaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private ElasticSearchRepository elasticSearchRepository;

    @Autowired
    private CaseMigrationProcessor caseMigrationProcessor;

    @Autowired
    private IdamRepository idamRepository;

    @Value("${migration.caseType}")
    private String caseType;

    public static void main(String[] args) {
        SpringApplication.run(CaseMigrationRunner.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Data migration of all cases started");
            String userToken =  idamRepository.generateUserToken();
            List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCasesWithOutHmctsSServiceId(userToken, caseType);
            listOfCaseDetails.stream()
                    .forEach(caseDetails -> caseMigrationProcessor.processCaseDetails(userToken, caseDetails));
            log.info("Data migration of all cases completed");
        } catch (Throwable e) {
            log.error("Migration failed with the following reason: {}", e.getMessage(), e);
        }
    }
}
