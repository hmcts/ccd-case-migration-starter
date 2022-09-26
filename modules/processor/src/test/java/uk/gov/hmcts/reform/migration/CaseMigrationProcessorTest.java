package uk.gov.hmcts.reform.migration;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

@RunWith(MockitoJUnitRunner.class)
public class CaseMigrationProcessorTest {

    private static final String USER_ID = "30";
    private static final String USER_TOKEN = "Bearer eeeejjjttt";
    private static final String CASE_ID = "11111";
    private static final String EVENT_ID = "migrateWorkAllocationR3";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";

    private final CaseDetails caseDetails1 = createCaseDetails(1111L, "case-1");
    private final CaseDetails caseDetails2 = createCaseDetails(1112L, "case-2");
    private final CaseDetails caseDetails3 = createCaseDetails(1113L, "case-3");

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private DataMigrationService dataMigrationService;

    @InjectMocks
    private CaseMigrationProcessor caseMigrationProcessor;

    @Test
    public void shouldNotProcessASingleCaseWithoutRedundantFields() {
        when(coreCaseDataService.fetchOne(USER_TOKEN, CASE_ID)).thenReturn(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> false);
        caseMigrationProcessor.processSingleCase(USER_TOKEN, CASE_ID, false);
        verify(coreCaseDataService, times(1)).fetchOne(USER_TOKEN, CASE_ID);
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
        assertThat(caseMigrationProcessor.getMigratedCases(), hasSize(0));
    }

    @Test
    public void shouldProcessASingleCaseAndMigrationIsSuccessful() {
        when(coreCaseDataService.fetchOne(USER_TOKEN, CASE_ID)).thenReturn(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        caseMigrationProcessor.processSingleCase(USER_TOKEN, CASE_ID, false);
        verify(coreCaseDataService, times(1)).fetchOne(USER_TOKEN, CASE_ID);
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
        assertThat(caseMigrationProcessor.getMigratedCases(), contains(1111L));
    }

    @Test
    public void shouldProcessASingleCaseAndMigrationIsFailed() {
        when(coreCaseDataService.fetchOne(USER_TOKEN, CASE_ID)).thenReturn(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(dataMigrationService.migrate(caseDetails1.getData(), caseDetails1.getId())).thenReturn(caseDetails1.getData());
        when(coreCaseDataService.update(USER_TOKEN, caseDetails1.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails1.getData())).thenThrow(new RuntimeException("Internal server error"));
        caseMigrationProcessor.processSingleCase(USER_TOKEN, CASE_ID, false);
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
        when(dataMigrationService.migrate(caseDetails1.getData(), caseDetails1.getId())).thenReturn(caseDetails1.getData());
        when(dataMigrationService.migrate(caseDetails2.getData(), caseDetails2.getId())).thenReturn(caseDetails2.getData());
        when(dataMigrationService.migrate(caseDetails3.getData(), caseDetails3.getId())).thenReturn(caseDetails3.getData());
        when(coreCaseDataService.update(USER_TOKEN, caseDetails3.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails3.getData())).thenThrow(new RuntimeException("Internal server error"));
        caseMigrationProcessor.processAllCases(USER_TOKEN, USER_ID, false);
        assertThat(caseMigrationProcessor.getFailedCases(), contains(1113L));
        assertThat(caseMigrationProcessor.getMigratedCases(), contains(1111L, 1112L));
    }

    @Test
    public void shouldProcessAllTheCandidateCases_whenTwoCasesFailed() {
        mockDataFetch(caseDetails1, caseDetails2, caseDetails3);
        mockDataUpdate(caseDetails1);
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(dataMigrationService.migrate(caseDetails1.getData(), caseDetails1.getId())).thenReturn(caseDetails1.getData());
        when(dataMigrationService.migrate(caseDetails2.getData(), caseDetails2.getId())).thenReturn(caseDetails2.getData());
        when(dataMigrationService.migrate(caseDetails3.getData(), caseDetails3.getId())).thenReturn(caseDetails3.getData());
        when(coreCaseDataService.update(USER_TOKEN, caseDetails2.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails2.getData())).thenThrow(new RuntimeException("Internal server error"));
        when(coreCaseDataService.update(USER_TOKEN, caseDetails3.getId().toString(), EVENT_ID, EVENT_SUMMARY, EVENT_DESCRIPTION, caseDetails3.getData())).thenThrow(new RuntimeException("Internal server error"));
        caseMigrationProcessor.processAllCases(USER_TOKEN, USER_ID, false);
        assertThat(caseMigrationProcessor.getFailedCases(), contains(1112L, 1113L));
        assertThat(caseMigrationProcessor.getMigratedCases(), contains(1111L));
    }

    @Test
    public void shouldProcessNoCaseWhenNoCasesAvailable() {
        mockDataFetch();

        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        caseMigrationProcessor.processAllCases(USER_TOKEN, USER_ID, false);
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
        assertThat(caseMigrationProcessor.getFailedCases(), hasSize(0));
    }

    @Test
    public void shouldDoNothingIfNoCasesToProcess() throws InterruptedException {
        SearchResult result = SearchResult.builder()
            .cases(new ArrayList<>())
            .total(0).build();

        MigrationPageParams pageParams = new MigrationPageParams(10, 10);

        when(coreCaseDataService.searchCases(anyString(),
            any(SearchSourceBuilder.class))).thenReturn(result);

        caseMigrationProcessor.fetchAndProcessCases(USER_TOKEN, false, 1, pageParams);

        assertThat(caseMigrationProcessor.getFailedCases().size(), is(0));
        assertThat(caseMigrationProcessor.getMigratedCases().size(),  is(0));
    }

    @Test
    public void shouldUseOverrideIfProvided() throws InterruptedException {
        SearchResult result = SearchResult.builder()
            .cases(List.of(caseDetails1, caseDetails2, caseDetails3))
            .total(3).build();

        MigrationPageParams pageParams = new MigrationPageParams(10, 2);

        when(coreCaseDataService.searchCases(anyString(),
            any(SearchSourceBuilder.class))).thenReturn(result);

        when(coreCaseDataService.fetchNCases(USER_TOKEN, 1,
            caseDetails1.getId())).thenReturn(List.of(caseDetails2));

        caseMigrationProcessor.fetchAndProcessCases(USER_TOKEN, false, 1, pageParams);

        assertThat(caseMigrationProcessor.getFailedCases().size(), is(0));
        assertThat(caseMigrationProcessor.getMigratedCases().size(),  is(2));
    }

    @Test
    public void shouldProcessAllCasesIfNoOverride() throws InterruptedException {
        SearchResult result = SearchResult.builder()
            .cases(List.of(caseDetails1, caseDetails2, caseDetails3))
            .total(3).build();

        MigrationPageParams pageParams = new MigrationPageParams(10, 0);

        when(coreCaseDataService.searchCases(anyString(),
            any(SearchSourceBuilder.class))).thenReturn(result);

        when(coreCaseDataService.fetchNCases(USER_TOKEN, 2,
            caseDetails1.getId())).thenReturn(List.of(caseDetails2, caseDetails3));

        caseMigrationProcessor.fetchAndProcessCases(USER_TOKEN, false, 1, pageParams);

        assertThat(caseMigrationProcessor.getFailedCases().size(), is(0));
        assertThat(caseMigrationProcessor.getMigratedCases().size(),  is(3));
    }

    @Test
    public void shouldBreakWhenNoMoreCasesReturned() throws InterruptedException {
        SearchResult result = SearchResult.builder()
            .cases(List.of(caseDetails1, caseDetails2, caseDetails3))
            .total(3).build();

        MigrationPageParams pageParams = new MigrationPageParams(10, 2);

        when(coreCaseDataService.searchCases(any(String.class),
            any(SearchSourceBuilder.class))).thenReturn(result);

        caseMigrationProcessor.fetchAndProcessCases(USER_TOKEN, false, 1, pageParams);

        assertThat(caseMigrationProcessor.getFailedCases().size(), is(0));
        assertThat(caseMigrationProcessor.getMigratedCases().size(),  is(1));
    }

    @Test
    public void shouldSearchFromLastCaseInPreviousResult() throws InterruptedException {
        SearchResult result = SearchResult.builder()
            .cases(List.of(caseDetails1, caseDetails2, caseDetails3))
            .total(3).build();

        MigrationPageParams pageParams = new MigrationPageParams(1, 0);

        when(coreCaseDataService.searchCases(any(String.class),
            any(SearchSourceBuilder.class))).thenReturn(result);

        when(coreCaseDataService.fetchNCases(USER_TOKEN, 1,
            caseDetails1.getId())).thenReturn(List.of(caseDetails2));

        when(coreCaseDataService.fetchNCases(USER_TOKEN, 1,
            caseDetails2.getId())).thenReturn(List.of(caseDetails3));

        caseMigrationProcessor.fetchAndProcessCases(USER_TOKEN, false, 1, pageParams);

        assertThat(caseMigrationProcessor.getFailedCases().size(), is(0));
        assertThat(caseMigrationProcessor.getMigratedCases().size(),  is(3));
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
