package uk.gov.hmcts.reform.migration.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.function.Predicate;

@Service
public class DataMigrationServiceImpl implements DataMigrationService<CaseDetails> {

    @Override
    public Predicate<CaseDetails> accepts() {
        return caseDetails -> caseDetails == null ? false : true;
    }

    @Override
    public CaseDetails migrate(CaseDetails caseDetails) {
        return caseDetails;
    }
}
