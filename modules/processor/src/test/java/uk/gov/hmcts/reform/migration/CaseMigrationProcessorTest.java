package uk.gov.hmcts.reform.migration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class CaseMigrationProcessorTest {

    private static final String USER_TOKEN = "Bearer eeeejjjttt";
    private static final String USER_ID = "30";
    private static final String CASE_ID = "11111";
    private static final String EVENT_ID = "migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";

    private final CaseDetails caseDetails1 = createCaseDetails(1111L, "case-1");
    private final CaseDetails caseDetails2 = createCaseDetails(1112L, "case-2");
    private final CaseDetails caseDetails3 = createCaseDetails(1113L, "case-3");

    @InjectMocks
    private CaseMigrationProcessor caseMigrationProcessor;
    @Mock
    private CoreCaseDataService coreCaseDataService;
    @Mock
    private DataMigrationService dataMigrationService;

    @Test
    public void shouldNotProcessASingleCaseWithOutRedundantFields() {
        when(coreCaseDataService.fetchOne(USER_TOKEN, CASE_ID)).thenReturn(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> false);
        caseMigrationProcessor.processSingleCase(USER_TOKEN, CASE_ID);
        verify(coreCaseDataService, times(1)).fetchOne(USER_TOKEN, CASE_ID);
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
        assertThat(caseMigrationProcessor.getMigratedCases(), hasSize(0));
    }

    @Test
    public void shouldProcessASingleCaseAndMigrationIsSuccessful() {
        when(coreCaseDataService.fetchOne(USER_TOKEN, CASE_ID)).thenReturn(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        caseMigrationProcessor.processSingleCase(USER_TOKEN, CASE_ID);
        verify(coreCaseDataService, times(1)).fetchOne(USER_TOKEN, CASE_ID);
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
        assertThat(caseMigrationProcessor.getMigratedCases(), contains(1111L));
    }

    @Test
    public void shouldProcessASingleCaseAndMigrationIsFailed() {
        when(coreCaseDataService.fetchOne(USER_TOKEN, CASE_ID)).thenReturn(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(coreCaseDataService.update(USER_TOKEN, caseDetails1.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails1.getData())).thenThrow(new RuntimeException("Internal server error"));
        caseMigrationProcessor.processSingleCase(USER_TOKEN, CASE_ID);
        verify(coreCaseDataService, times(1)).fetchOne(USER_TOKEN, CASE_ID);
        verify(coreCaseDataService, times(1)).update(USER_TOKEN, "1111", EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails1.getData());
        assertThat(caseMigrationProcessor.getFailedCases(), contains(1111L));
        assertThat(caseMigrationProcessor.getMigratedCases(), hasSize(0));
    }

    @Test
    public void shouldProcessAllTheCandidateCases_whenOneCaseFailed() {
        mockDataFetch(caseDetails1, caseDetails2, caseDetails3);
        mockDataUpdate(caseDetails1);
        mockDataUpdate(caseDetails2);
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(coreCaseDataService.update(USER_TOKEN, caseDetails3.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails3.getData())).thenThrow(new RuntimeException("Internal server error"));
        caseMigrationProcessor.processAllCases(USER_TOKEN, USER_ID);
        assertThat(caseMigrationProcessor.getFailedCases(), contains(1113L));
        assertThat(caseMigrationProcessor.getMigratedCases(), contains(1111L, 1112L));
    }

    @Test
    public void shouldProcessAllTheCandidateCases_whenTwoCasesFailed() {
        mockDataFetch(caseDetails1, caseDetails2, caseDetails3);
        mockDataUpdate(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(coreCaseDataService.update(USER_TOKEN, caseDetails2.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails2.getData())).thenThrow(new RuntimeException("Internal server error"));
        when(coreCaseDataService.update(USER_TOKEN, caseDetails3.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails3.getData())).thenThrow(new RuntimeException("Internal server error"));
        caseMigrationProcessor.processAllCases(USER_TOKEN, USER_ID);
        assertThat(caseMigrationProcessor.getFailedCases(), contains(1112L, 1113L));
        assertThat(caseMigrationProcessor.getMigratedCases(), contains(1111L));
    }

    @Test
    public void shouldProcessNoCaseWhenNoCasesAvailable() {
        mockDataFetch();

        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        caseMigrationProcessor.processAllCases(USER_TOKEN, USER_ID);
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
    }

    private void mockDataFetch(CaseDetails... caseDetails) {
        when(coreCaseDataService.fetchAll(USER_TOKEN, USER_ID)).thenReturn(asList(caseDetails));
    }

    private void mockDataUpdate(CaseDetails caseDetails) {
        when(coreCaseDataService.update(USER_TOKEN, caseDetails.getId().toString(),
            EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails.getData()
        )).thenReturn(caseDetails);
    }

    private CaseDetails createCaseDetails(long id, String value) {
        Map<String, Object> data = new HashMap<>();
        data.put("key", value);
        return CaseDetails.builder()
            .id(id)
            .data(data)
            .build();
    }
}
