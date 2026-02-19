package uk.gov.hmcts.reform.migration.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.Map;
import java.util.function.Predicate;

@Service
public class DataMigrationServiceImpl implements DataMigrationService<Map<String, Object>> {

    @Override
    public Predicate<CaseDetails> accepts() {
        return caseDetails -> {
            if (caseDetails == null) {
                return false;
            }
            final Map<String, Object> data = caseDetails.getData();
            if (data == null) {
                return false;
            }
            return data.containsKey("generalEmailRecipient")
                || data.containsKey("generalEmailCreatedBy")
                || data.containsKey("generalEmailUploadedDocument")
                || data.containsKey("generalEmailBody");
        };
    }

    @Override
    public Map<String, Object> migrate(Map<String, Object> data) {
        if (data != null) {
            data.remove("generalEmailRecipient");
            data.remove("generalEmailCreatedBy");
            data.remove("generalEmailUploadedDocument");
            data.remove("generalEmailBody");
        }
        return data;
    }
}
