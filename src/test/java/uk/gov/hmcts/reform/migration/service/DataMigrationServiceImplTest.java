package uk.gov.hmcts.reform.migration.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class DataMigrationServiceImplTest {

    private DataMigrationServiceImpl service = new DataMigrationServiceImpl();

    @Test
    public void shouldReturnTrueForCaseDetailsPassed() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1234L)
            .build();
        assertTrue(service.accepts().test(caseDetails));
    }

    @Test
    public void shouldReturnFalseForCaseDetailsNull() {
        assertFalse(service.accepts().test(null));
    }

    @Test
    public void shouldReturnPassedDataWhenMigrateCalled() {
        CaseDetails caseDetails = CaseDetails.builder().build();
        CaseDetails result = service.migrate(caseDetails);
        assertNotNull(result);
        assertEquals(caseDetails, result);
    }

    @Test
    public void shouldReturnNullWhenDataIsNotPassed() {
        CaseDetails result = service.migrate(null);
        assertNull(result);
        assertEquals(null, result);
    }
}
