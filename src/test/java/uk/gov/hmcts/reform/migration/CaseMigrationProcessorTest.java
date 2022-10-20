package uk.gov.hmcts.reform.migration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.domain.exception.CaseMigrationException;
import uk.gov.hmcts.reform.migration.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.migration.repository.ElasticSearchRepository;
import uk.gov.hmcts.reform.migration.repository.IdamRepository;
import uk.gov.hmcts.reform.migration.service.DataMigrationService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class CaseMigrationProcessorTest {

    private static final String USER_TOKEN = "Bearer eeeejjjttt";
    private static final String EVENT_ID = "migrateCase";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESCRIPTION = "Migrate Case";
    private static final String CASE_TYPE = "Test_Case_Type";

    @InjectMocks
    private CaseMigrationProcessor caseMigrationProcessor;

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private DataMigrationService dataMigrationService;

    @Mock
    private ElasticSearchRepository elasticSearchRepository;

    @Mock
    private IdamRepository idamRepository;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(caseMigrationProcessor, "caseProcessLimit", 1);
    }

    @Test
    public void shouldMigrateCasesOfACaseType() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details))
            .thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
    }

    @Test
    public void shouldMigrateOnlyLimitedNumberOfCases() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        CaseDetails details1 = mock(CaseDetails.class);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        caseDetails.add(details1);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details))
            .thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
    }

    @Test
    public void shouldThrowExceptionWhenCaseTypeNull() {
        assertThrows(CaseMigrationException.class, () -> caseMigrationProcessor.migrateCases(null));
    }

    @Test
    public void shouldThrowExceptionWhenMultipleCaseTypesPassed() {
        assertThrows(CaseMigrationException.class, () ->
            caseMigrationProcessor.migrateCases("Cast_Type1,Cast_Type2"));
    }
}
