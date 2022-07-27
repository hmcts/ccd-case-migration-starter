package uk.gov.hmcts.reform.migration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import static uk.gov.hmcts.reform.migration.queries.CcdElasticSearchQueries.fetchAllUnsetCaseAccessManagementFieldsCasesQuery;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseMigrationProcessor {

    private static final String EVENT_ID = "migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";

    private final StopWatch totalTimer = new StopWatch();

    private final CoreCaseDataService coreCaseDataService;
    private final DataMigrationService<?> dataMigrationService;

    @Getter
    private final List<Long> migratedCases = new ArrayList<>();

    @Getter
    private final List<Long> failedCases = new ArrayList<>();

    @Getter
    private Long totalCases = 0L;

    public void processSingleCase(String userToken, String caseId, boolean dryRun) {
        CaseDetails caseDetails;
        try {
            caseDetails = coreCaseDataService.fetchOne(userToken, caseId);
        } catch (Exception ex) {
            log.error("Case {} not found due to: {}", caseId, ex.getMessage());
            return;
        }
        if (dataMigrationService.accepts().test(caseDetails)) {
            updateCase(userToken, caseDetails.getId(), caseDetails.getData(), dryRun);
        } else {
            log.info("Case {} already migrated", caseDetails.getId());
        }
    }

    public void fetchAndProcessCases(String userToken, boolean dryRun, int numThreads, MigrationPageParams pageParams)
        throws InterruptedException {

        SearchResult initialSearch = coreCaseDataService.searchCases(userToken,
            fetchAllUnsetCaseAccessManagementFieldsCasesQuery());

        if (initialSearch.getTotal() <= 0) {
            return;
        }

        totalTimer.start();

        int totalCasesToProcess = resolveTotalCasesToProcess(initialSearch, pageParams.getMaxCasesToProcess());

        Long searchFrom = handleFirstCase(userToken, dryRun, initialSearch);

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        fetchAndSubmitTasks(userToken, dryRun, totalCasesToProcess, pageParams.getPageSize(), searchFrom,
            executorService);

        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    private int resolveTotalCasesToProcess(SearchResult initialSearch, int maxCasesToProcess) {
        int totalCasesToProcess = 0;

        if (maxCasesToProcess > 0) {
            log.info("Manual case override in use, limiting to {} cases", maxCasesToProcess);
            totalCasesToProcess = maxCasesToProcess;
        } else {
            log.info("No manual case override in use, fetching all: {} cases", initialSearch.getTotal());
            totalCasesToProcess = initialSearch.getTotal();
        }

        return totalCasesToProcess;
    }

    private void fetchAndSubmitTasks(String userToken, boolean dryRun, int totalCasesToProcess, int pageSize,
                                     Long searchFrom, ExecutorService executorService) {
        int casesFetched = 1;
        int numCasesToFetch = pageSize;

        while (casesFetched < totalCasesToProcess) {
            numCasesToFetch = resolvePageSize(totalCasesToProcess, casesFetched, numCasesToFetch, pageSize);

            List<CaseDetails> caseDetails =
                coreCaseDataService.fetchNCases(userToken, numCasesToFetch, searchFrom);

            if (caseDetails.isEmpty()) {
                break;
            }

            searchFrom = caseDetails.get(caseDetails.size() - 1).getId();

            executorService.execute(() -> caseDetails
                .forEach(caseDetail ->
                    updateCase(userToken, caseDetail.getId(), caseDetail.getData(), dryRun)));

            log.info("New task submitted");

            casesFetched += caseDetails.size();

            log.info("{} cases fetched out of {}", casesFetched, totalCasesToProcess);
        }
    }

    private int resolvePageSize(int totalCasesToProcess, int casesFetched, int numCasesToFetch, int pageSize) {
        int remainingCases = totalCasesToProcess - casesFetched;
        if (remainingCases < pageSize) {
            numCasesToFetch = remainingCases;
        }
        return numCasesToFetch;
    }

    private Long handleFirstCase(String userToken, boolean dryRun, SearchResult initialSearch) {
        log.info("Processing first case...");
        CaseDetails firstCase = initialSearch.getCases().get(0);
        updateCase(userToken, firstCase.getId(), firstCase.getData(), dryRun);
        return firstCase.getId();
    }

    public void processAllCases(String userToken, String userId, boolean dryRun) {
        coreCaseDataService.fetchAll(userToken, userId).stream()
            .filter(dataMigrationService.accepts())
            .forEach(caseDetails -> updateCase(userToken, caseDetails.getId(), caseDetails.getData(), dryRun));
    }

    protected List<LocalDate> getListOfDates(LocalDate startDate, LocalDate endDate) {
        return startDate
            .datesUntil(endDate)
            .collect(Collectors.toList());
    }

    private void updateCase(String authorisation, Long id, Map<String, Object> data, boolean dryRun) {

        totalCases++;

        try {
            var migratedData = dataMigrationService.migrate(data);
            if (!dryRun) {
                coreCaseDataService.update(
                    authorisation,
                    id.toString(),
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    migratedData);
                log.info("Case {} successfully updated", id);
            }
            migratedCases.add(id);

        } catch (Exception e) {
            log.error("Case {} update failed due to: {}", id, e.getMessage());
            failedCases.add(id);
        }

        if (totalCases % 1000 == 0) {
            log.info("----------{} cases migrated in {} minutes ({} seconds)----------", totalCases,
                totalTimer.getTime(TimeUnit.MINUTES), totalTimer.getTime(TimeUnit.SECONDS));
        }
    }
}
