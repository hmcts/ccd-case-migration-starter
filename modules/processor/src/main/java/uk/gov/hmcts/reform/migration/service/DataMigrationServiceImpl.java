package uk.gov.hmcts.reform.migration.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.Map;
import java.util.function.Predicate;

@Service
public class DataMigrationServiceImpl<T> implements DataMigrationService<T> {
    @Override
    public Predicate<CaseDetails> accepts() {
        return null;
    }

    @Override
    public T migrate(Map<String, Object> data) {
        return null;
    }
}
