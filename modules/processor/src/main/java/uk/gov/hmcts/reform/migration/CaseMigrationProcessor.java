package uk.gov.hmcts.reform.migration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseMigrationProcessor {

    private static final String EVENT_ID = "migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";

    private final CoreCaseDataService coreCaseDataService;
    private final DataMigrationService<?> dataMigrationService;

    @Getter
    private final List<Long> migratedCases = new ArrayList<>();

    @Getter
    private final List<Long> failedCases = new ArrayList<>();

    @Getter
    private Long totalCases = 0L;

    @Value("${migration.parallel:false}")
    private boolean parallel;

    public void processSingleCase(String userToken, String caseId, boolean dryrun) {
        CaseDetails caseDetails;
        try {
            caseDetails = coreCaseDataService.fetchOne(userToken, caseId);
        } catch (Exception ex) {
            log.error("Case {} not found due to: {}", caseId, ex.getMessage());
            return;
        }
        if (dataMigrationService.accepts().test(caseDetails)) {
            updateCase(userToken, caseDetails.getId(), caseDetails.getData(), dryrun);
        } else {
            log.info("Case {} already migrated", caseDetails.getId());
        }
    }

    public void processAllCases(String userToken, String firstDate, String lastDate, boolean dryrun) {
        CaseDetails oldestCaseDetails = coreCaseDataService.fetchOldestCase(userToken);
        if (oldestCaseDetails != null) {
            log.info("The data of the oldest case is " + oldestCaseDetails.getCreatedDate());
        }

        if (firstDate != null && lastDate != null) {
            List<LocalDate> listOfDates = getListOfDates(LocalDate.parse(firstDate), LocalDate.parse(lastDate));

            Optional<Stream<CaseDetails>> caseDetailsStreamOptional =
                coreCaseDataService.fetchAllBetweenDates(userToken, listOfDates, parallel);

            Stream<CaseDetails> caseDetailsStream;

            if (caseDetailsStreamOptional.isEmpty()) {
                return;
            }

            caseDetailsStream = caseDetailsStreamOptional.get();

            if (parallel) {
                log.info("Executing in parallel.. please wait.");
                caseDetailsStream = caseDetailsStream.parallel();
            }

            caseDetailsStream
                .forEach(caseDetail -> updateCase(
                    userToken,
                    caseDetail.getId(),
                    caseDetail.getData(),
                    dryrun)
                );
        }
    }

    protected List<LocalDate> getListOfDates(LocalDate startDate, LocalDate endDate) {
        return startDate
            .datesUntil(endDate)
            .collect(Collectors.toList());
    }

    private void updateCase(String authorisation, Long id, Map<String, Object> data, boolean dryrun) {

        totalCases++;

        if (dryrun) {
            return;
        }

        try {
            var migratedData = dataMigrationService.migrate(data);
            coreCaseDataService.update(
                authorisation,
                id.toString(),
                EVENT_ID,
                EVENT_SUMMARY,
                EVENT_DESCRIPTION,
                migratedData);

            log.info("Case {} successfully updated", id);
            migratedCases.add(id);

        } catch (Exception e) {
            log.error("Case {} update failed due to: {}", id, e.getMessage());
            failedCases.add(id);
        }
    }
}
