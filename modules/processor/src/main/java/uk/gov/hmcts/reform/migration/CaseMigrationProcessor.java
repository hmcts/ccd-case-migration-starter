package uk.gov.hmcts.reform.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.models.User;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
public class CaseMigrationProcessor {
    private static final String EVENT_ID = "migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";

    @Autowired
    private CoreCaseDataService coreCaseDataService;
    @Autowired
    private DataMigrationService<?> dataMigrationService;
    @Getter
    private List<Long> migratedCases = new ArrayList<>();
    @Getter
    private List<Long> failedCases = new ArrayList<>();

    public void processSingleCase(User user, String caseId) {
        CaseDetails caseDetails;
        try {
            caseDetails = coreCaseDataService.fetchOne(user.getAuthToken(), caseId);
        } catch (Exception ex) {
            log.error("Case {} not found due to: {}", caseId, ex.getMessage());
            return;
        }
        if (dataMigrationService.accepts().test(caseDetails)) {
            updateCase(user, caseDetails);
        } else {
            log.info("Case {} already migrated", caseDetails.getId());
        }
    }

    public void processAllCases(User user) {
        coreCaseDataService.fetchAll(user.getAuthToken(), user.getUserDetails().getId()).stream()
            .filter(dataMigrationService.accepts())
            .forEach(caseDetails -> updateCase(user, caseDetails));
    }

    public void processAllCasesPageByPage(User user) {
        String authToken = user.getAuthToken();
        String userId = user.getUserDetails().getId();
        int numberOfPages = coreCaseDataService.getNumberOfPages(authToken, userId, new HashMap<>());
        log.info("Total no of pages: {}", numberOfPages);

        IntStream.rangeClosed(1, numberOfPages).boxed()
            .peek(pageNo ->
                log.info("Fetching cases for the page no {} of total {}", pageNo, numberOfPages)
            )
            .flatMap(pageNumber -> coreCaseDataService.fetchPage(authToken, userId, pageNumber).stream())
            .filter(dataMigrationService.accepts())
            .forEach(caseDetails -> updateCase(user, caseDetails));
    }

    private void updateCase(User user, CaseDetails caseDetails) {
        Long id = caseDetails.getId();
        log.info("Updating case {}", id);
        try {
            log.debug("Case data: {}", caseDetails.getData());
            coreCaseDataService.update(user.getAuthToken(), id.toString(),
                EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, dataMigrationService.migrate(user, caseDetails));
            log.info("Case {} successfully updated", id);
            migratedCases.add(id);
        } catch (Exception e) {
            log.error("Case {} update failed due to: {}", id, e.getMessage());
            failedCases.add(id);
        }
    }
}
