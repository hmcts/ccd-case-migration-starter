package uk.gov.hmcts.reform.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.repository.IdamRepository;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CaseMigrationProcessor {
    private static final String EVENT_ID = "migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";

    @Autowired
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private DataMigrationService dataMigrationService;

    @Autowired
    private ElasticSearchRepository elasticSearchRepository;

    @Autowired
    private IdamRepository idamRepository;

    @Getter
    private List<Long> migratedCases = new ArrayList<>();

    @Getter
    private List<Long> failedCases = new ArrayList<>();

    @Value("${migration.caseType}")
    private String caseType;

    public void processSingleCase(String userToken, String caseId) {
        CaseDetails caseDetails;
        try {
            caseDetails = coreCaseDataService.fetchOne(userToken, caseId);
        } catch (Exception ex) {
            log.error("Case {} not found due to: {}", caseId, ex.getMessage());
            return;
        }
        updateCase(userToken, caseDetails);
    }

    public void processAllCases(String userToken, String userId) {
        coreCaseDataService.fetchAll(userToken, userId).stream()
            .forEach(caseDetails -> updateCase(userToken, caseDetails));
    }

    public void migrateCases() {
        log.info("Data migration of all cases started for case type: {}", caseType);
        String userToken =  idamRepository.generateUserToken();
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCasesWithOutHmctsSServiceId(userToken, caseType);
        listOfCaseDetails.stream()
            .forEach(caseDetails -> updateCase(userToken, caseDetails));
        log.info("-----------------------------------------");
        log.info("Data migration completed");
        log.info("-----------------------------------------");
        log.info("Total number of processed cases: {}", getMigratedCases().size() + getFailedCases().size());
        log.info("Total number of migrations performed: {}", getMigratedCases().size());
        log.info("-----------------------------------------");
        log.info("Migrated cases: {} ", !getMigratedCases().isEmpty() ? getMigratedCases() : "NONE");
        log.info("Failed cases: {}", !getFailedCases().isEmpty() ? getFailedCases() : "NONE");
        log.info("Data migration of all cases completed");
    }

    private void updateCase(String authorisation, CaseDetails caseDetails) {
        if (dataMigrationService.accepts().test(caseDetails)) {
            Long id = caseDetails.getId();
            log.info("Updating case {}", id);
            try {
                log.debug("Case data: {}", caseDetails.getData());
                coreCaseDataService.update(
                    authorisation,
                    id.toString(),
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    caseDetails.getData()
                );
                log.info("Case {} successfully updated", id);
                migratedCases.add(id);
            } catch (Exception e) {
                log.error("Case {} update failed due to: {}", id, e.getMessage());
                failedCases.add(id);
            }
        } else {
            log.info("Case {} already migrated", caseDetails.getId());
        }
    }
}
