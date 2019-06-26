package uk.gov.hmcts.reform.migration.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.function.Predicate;

public interface DataMigrationService {
    Predicate<CaseDetails> accepts();

    void migrate(CaseDetails caseDetails);
}
