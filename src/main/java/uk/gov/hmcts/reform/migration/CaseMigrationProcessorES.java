package uk.gov.hmcts.reform.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.models.User;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.repository.IdamRepository;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseMigrationProcessorES implements MigrationProcessor {
    public static final String LOG_STRING = "-----------------------------------------";

    private final IdamRepository idamRepository;
    private final ElasticSearchRepository elasticSearchRepository;
    private final DataMigrationService<CaseDetails> dataMigrationService;
    private final CoreCaseDataService coreCaseDataService;

    @Value("${case-migration.processing.limit}")
    private int caseProcessLimit;

    @Override
    public void process(User user) {

    }

    public void migrateCases(String caseType) {
        validateCaseType(caseType);
        log.info("Data migration of cases started for case type: {}", caseType);
        String userToken = idamRepository.generateUserToken();
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(userToken, caseType);
        listOfCaseDetails.stream()
            .limit(caseProcessLimit)
            .forEach(caseDetails -> updateCase(userToken, caseType, caseDetails));
        log.info(
            """
                {}
                Data migration completed
                {}
                Total number of processed cases:
                {}
                Total number of migrations performed:
                {}
                {}
                """,
            LOG_STRING,
            LOG_STRING,
            migratedCases.size() + failedCases.size(),
            migratedCases.size(),
            LOG_STRING
        );

        if (migratedCases.isEmpty()) {
            log.info("Migrated cases: NONE ");
        } else {
            log.info("Migrated cases: {} ", migratedCases);
        }

        if (failedCases.isEmpty()) {
            log.info("Failed cases: NONE ");
        } else {
            log.info("Failed cases: {} ", failedCases);
        }
        log.info("Data migration of cases completed");
    }

    private void updateCase(String authorisation, String caseType, CaseDetails caseDetails) {
        if (dataMigrationService.accepts().test(caseDetails)) {
            Long id = caseDetails.getId();
            log.info("Updating case {}", id);
            try {
                log.debug("Case data: {}", caseDetails.getData());
                coreCaseDataService.update(
                    authorisation,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    caseType,
                    dataMigrationService.migrate(caseDetails)
                );
                log.info("Case {} successfully updated", id);
                migratedCases.add(id);
            } catch (Exception e) {
                log.error("Case {} update failed due to: {}", id, e.getMessage());
                failedCases.add(id);
            }
        } else {
            log.info("Case {} does not meet criteria for migration", caseDetails.getId());
        }
    }

}
