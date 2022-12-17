package uk.gov.hmcts.reform.migration;

import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.idam.client.models.User;

import java.util.ArrayList;
import java.util.List;

public interface MigrationProcessor {
    String EVENT_ID = "migrateCase";
    String EVENT_SUMMARY = "Migrate Case";
    String EVENT_DESCRIPTION = "Migrate Case";

    List<Long> migratedCases = new ArrayList<>();

    List<Long> failedCases = new ArrayList<>();

    default void validateCaseType(String caseType) {
        if (!StringUtils.hasText(caseType)) {
            throw new CaseMigrationException("Provide case type for the migration");
        }

        if (caseType.split(",").length > 1) {
            throw new CaseMigrationException("Only One case type at a time is allowed for the migration");
        }
    }

    void process(User user) throws InterruptedException;
}
