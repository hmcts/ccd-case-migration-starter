package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DataMigrationServiceImplTest {

    private DataMigrationServiceImpl service = new DataMigrationServiceImpl();

    // Unit tests for 'accepts()'

    @Test
    public void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(service.accepts().test(null));
    }

    @Test
    public void shouldReturnFalseForCaseDetailsWithoutData() {
        CaseDetails caseDetails = CaseDetails.builder().build();
        assertFalse(service.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnFalseForCaseDetailsWithoutGeneralEmailElement() {
        CaseDetails caseDetails = CaseDetails.builder().build();
        caseDetails.setData(new HashMap<>());
        assertFalse(service.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnTrueForCaseDetailsWithGeneralEmailRecipient() {
        CaseDetails caseDetails = CaseDetails.builder().build();
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailRecipient", null);
        caseDetails.setData(data);
        assertTrue(service.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnTrueForCaseDetailsWithGeneralEmailCreatedBy() {
        CaseDetails caseDetails = CaseDetails.builder().build();
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailCreatedBy", null);
        caseDetails.setData(data);
        assertTrue(service.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnTrueForCaseDetailsWithGeneralEmailUploadedDocument() {
        CaseDetails caseDetails = CaseDetails.builder().build();
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailUploadedDocument", null);
        caseDetails.setData(data);
        assertTrue(service.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnTrueForCaseDetailsWithGeneralEmailBody() {
        CaseDetails caseDetails = CaseDetails.builder().build();
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailBody", null);
        caseDetails.setData(data);
        assertTrue(service.accepts().test(caseDetails));
    }


    // Unit tests for 'migrate()'

    @Test
    public void shouldReturnNullWhenDataIsNotPassed() {
        Map<String, Object> result = service.migrate(null);
        assertNull(result);
        assertEquals(null, result);
    }

    @Test
    public void shouldReturnPassedDataWhenMigrateCalled() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> result = service.migrate(data);
        assertNotNull(result);
        assertEquals(data, result);
    }

    @Test
    public void shouldRemoveGeneralEmailRecipient() {
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailRecipient", null);
        data.put("otherElement", null);
        Map<String, Object> result = service.migrate(data);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void shouldRemoveGeneralEmailCreatedBy() {
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailCreatedBy", null);
        data.put("otherElement", null);
        Map<String, Object> result = service.migrate(data);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void shouldRemoveGeneralEmailUploadedDocument() {
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailUploadedDocument", null);
        data.put("otherElement", null);
        Map<String, Object> result = service.migrate(data);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void shouldRemoveGeneralEmailBody() {
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailBody", null);
        data.put("otherElement", null);
        Map<String, Object> result = service.migrate(data);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void shouldRemoveAllGeneralEmailElements() {
        Map<String, Object> data = new HashMap<>();
        data.put("generalEmailRecipient", null);
        data.put("generalEmailCreatedBy", null);
        data.put("generalEmailUploadedDocument", null);
        data.put("generalEmailBody", null);
        data.put("otherElement", null);
        Map<String, Object> result = service.migrate(data);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

}
