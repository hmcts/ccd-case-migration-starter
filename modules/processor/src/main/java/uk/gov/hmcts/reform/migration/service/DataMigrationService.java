package uk.gov.hmcts.reform.migration.service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.models.User;

import java.util.Map;
import java.util.function.Predicate;

public interface DataMigrationService<T> {
    Predicate<CaseDetails> accepts();

    T migrate(User user, CaseDetails caseDetails);
}
