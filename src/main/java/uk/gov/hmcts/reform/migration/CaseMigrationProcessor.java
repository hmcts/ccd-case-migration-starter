package uk.gov.hmcts.reform.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.repository.IdamRepository;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CaseMigrationProcessor {
    private static final String EVENT_ID = "migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";
    public static final String LOG_STRING = "-----------------------------------------";

    @Autowired
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private DataMigrationService<Map<String, Object>> dataMigrationService;

    @Autowired
    private ElasticSearchRepository elasticSearchRepository;

    @Autowired
    private IdamRepository idamRepository;

    @Getter
    private List<Long> migratedCases = new ArrayList<>();

    @Getter
    private List<Long> failedCases = new ArrayList<>();

    @Value("${case-migration.processing.limit}")
    private int caseProcessLimit;

    public void migrateCases(String caseType) {
        validateCaseType(caseType);
        log.info("Data migration of cases started for case type: {}", caseType);
        String userToken =  idamRepository.generateUserToken();
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
            getMigratedCases().size() + getFailedCases().size(),
            getMigratedCases().size(),
            LOG_STRING
        );

        if (getMigratedCases().isEmpty()) {
            log.info("Migrated cases: NONE ");
        } else {
            log.info("Migrated cases: {} ", getMigratedCases());
        }

        if (getFailedCases().isEmpty()) {
            log.info("Failed cases: NONE ");
        } else {
            log.info("Failed cases: {} ", getFailedCases());
        }
        log.info("Data migration of cases completed");
    }

    private void validateCaseType(String caseType) {
        if (!StringUtils.hasText(caseType)) {
            throw new CaseMigrationException("Provide case type for the migration");
        }

        if (caseType.split(",").length > 1) {
            throw new CaseMigrationException("Only One case type at a time is allowed for the migration");
        }
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
                    caseDetails
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
