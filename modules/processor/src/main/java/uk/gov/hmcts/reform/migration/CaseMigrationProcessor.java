package uk.gov.hmcts.reform.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void processAllCases(String userToken, String userId, String firstDate,
                                String lastDate, boolean dryrun) {
        CaseDetails oldestCaseDetails = coreCaseDataService.fetchOldestCase(userToken, userId);
        if (oldestCaseDetails != null) {
            log.info("The data of the oldest case is " + oldestCaseDetails.getCreatedDate());
        }
        if (firstDate != null && lastDate != null) {
            List<LocalDate> dates = getListOfDates(LocalDate.parse(firstDate), LocalDate.parse(lastDate));
            for (LocalDate localDate : dates) {
                coreCaseDataService.fetchAllForDay(userToken, userId, localDate.toString()).stream()
                    .filter(dataMigrationService.accepts())
                    .forEach(caseDetails -> updateCase(userToken, caseDetails.getId(), caseDetails.getData(), dryrun));
            }
        }
    }

    protected List<LocalDate> getListOfDates(LocalDate startDate, LocalDate endDate) {
            return startDate.datesUntil(endDate)
                .collect(Collectors.toList());
    }

    private void updateCase(String authorisation, Long id, Map<String, Object> data, boolean dryrun) {
        log.info("Updating case {}", id);
        try {
            if (dryrun) {
                log.info("Case {} not being updated, dryrun", id);
            } else {
                log.debug("Case data: {}", data);
                coreCaseDataService.update(authorisation, id.toString(),
                    EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, dataMigrationService.migrate(data));
                log.info("Case {} successfully updated", id);
                migratedCases.add(id);
            }
        } catch (Exception e) {
            log.error("Case {} update failed due to: {}", id, e.getMessage());
            failedCases.add(id);
        }
    }
}
