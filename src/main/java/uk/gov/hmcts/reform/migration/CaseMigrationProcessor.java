package uk.gov.hmcts.reform.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.idam.client.models.User;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "migration", name = "elasticsearch", havingValue = "false")
public class CaseMigrationProcessor implements MigrationProcessor {
    private static final int DEFAULT_MAX_CASES_TO_PROCESS = 100000;
    private static final int DEFAULT_THREAD_LIMIT = 10;
    private final CoreCaseDataService coreCaseDataService;
    private final DataMigrationService<CaseDetails> dataMigrationService;
    private final MigrationProperties migrationProperties;

    @Override
    public void process(User user) throws InterruptedException {
        String authToken = user.getAuthToken();
        String userId = user.getUserDetails().getId();
        int numberOfPages = coreCaseDataService.getNumberOfPages(authToken, userId, new HashMap<>());
        log.info("Total no of pages: {}", numberOfPages);
        int numberOfThreads = ofNullable(migrationProperties.getNumThreads()).orElse(DEFAULT_THREAD_LIMIT);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        IntStream.rangeClosed(1, numberOfPages).boxed()
            .peek(pageNo ->
                      log.info("Fetching cases for the page no {} of total {}", pageNo, numberOfPages)
            )
            .flatMap(pageNumber -> coreCaseDataService.fetchPage(authToken, userId, pageNumber).stream())
            .filter(dataMigrationService.accepts())
            .limit(ofNullable(migrationProperties.getMaxCasesToProcess()).orElse(DEFAULT_MAX_CASES_TO_PROCESS))
            .forEach(submitMigration(authToken, executorService));

        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    private Consumer<CaseDetails> submitMigration(String authToken, ExecutorService executorService) {
        return caseDetails ->
            executorService.submit(() -> updateCase(authToken, caseDetails));
    }

    public void migrateSingleCase(User user, String caseId) {
        try {
            validateCaseType(migrationProperties.getCaseType());
            String userToken = user.getAuthToken();
            CaseDetails caseDetails = coreCaseDataService.fetchOne(
                userToken,
                caseId
            ).orElseThrow(CaseNotFoundException::new);
            if (dataMigrationService.accepts().test(caseDetails)) {
                updateCase(userToken, caseDetails);
            } else {
                log.info("Case {} already migrated", caseDetails.getId());
            }
        } catch (CaseNotFoundException ex) {
            log.error("Case {} not found due to: {}", caseId, ex.getMessage());
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }

    private void updateCase(String userToken, CaseDetails caseDetails) {
        Long id = caseDetails.getId();
        log.info("Updating case {}", id);
        try {
            log.debug("Case data: {}", caseDetails.getData());
            if (!migrationProperties.isDryRun()) {
                coreCaseDataService.update(
                    userToken,
                    id.toString(),
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    dataMigrationService.migrate(caseDetails)
                );
                log.info("Case {} successfully updated", id);
            } else {
                log.info("Case {} dry run migration", id);
            }
            migratedCases.add(id);
        } catch (Exception e) {
            log.error("Case {} update failed due to: {}", id, e.getMessage());
            failedCases.add(id);
        }
    }
}
