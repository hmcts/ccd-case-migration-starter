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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class CaseMigrationProcessorTest {

    private static final String USER_TOKEN = "Bearer eeeejjjttt";
    private static final String EVENT_ID = "boHistoryCorrection";
    private static final String EVENT_SUMMARY = "Data migration - hand off flag change";
    private static final String EVENT_DESCRIPTION = "Data migration - hand off flag change";
    private static final String CASE_TYPE = "GrantOfRepresentation";

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
    public void shouldMigrateCasesOfSolGopTrustCorp() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        Map<String, Object> caseDatas = new HashMap<>();
        caseDatas.put("applicationType","Solicitor");
        caseDatas.put("caseType", "gop");
        caseDatas.put("titleAndClearingType", "TCTTrustCorpResWithSDJ");
        when(details.getData()).thenReturn(caseDatas);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details)).thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
        assertEquals(details.getData().get("caseHandedOffToLegacySite"),"Yes");
    }

    @Test
    public void shouldMigrateCasesOfSolIntestacyDeceasedDomicileInEngWales() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        Map<String, Object> caseDatas = new HashMap<>();
        caseDatas.put("applicationType","Solicitor");
        caseDatas.put("caseType", "intestacy");
        caseDatas.put("deceasedDomicileInEngWales", "No");
        when(details.getData()).thenReturn(caseDatas);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details)).thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
        assertEquals(details.getData().get("caseHandedOffToLegacySite"),"Yes");
    }

    @Test
    public void shouldMigrateCasesOfSolAdmonWillWillAccess() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        Map<String, Object> caseDatas = new HashMap<>();
        caseDatas.put("applicationType","Solicitor");
        caseDatas.put("caseType", "admonWill");
        caseDatas.put("willAccessOriginal", "No");
        caseDatas.put("willAccessNotarial", "Yes");
        when(details.getData()).thenReturn(caseDatas);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details)).thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
        assertEquals(details.getData().get("caseHandedOffToLegacySite"),"Yes");
    }

    @Test
    public void shouldMigrateCasesOfSolIntestacySolsApplicantRelationshipToDeceased() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        Map<String, Object> caseDatas = new HashMap<>();
        caseDatas.put("applicationType","Solicitor");
        caseDatas.put("caseType", "intestacy");
        caseDatas.put("solsApplicantRelationshipToDeceased", "Yes");
        when(details.getData()).thenReturn(caseDatas);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details)).thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
        assertEquals(details.getData().get("caseHandedOffToLegacySite"),"Yes");
    }

    @Test
    public void shouldMigrateCasesOfPersonalIntestacy() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        Map<String, Object> caseDatas = new HashMap<>();
        caseDatas.put("applicationType","Personal");
        caseDatas.put("caseType", "intestacy");
        caseDatas.put("primaryApplicantRelationshipToDeceased", "adoptedChild");
        caseDatas.put("primaryApplicantAdoptionInEnglandOrWales", "Yes");
        when(details.getData()).thenReturn(caseDatas);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details)).thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
        assertEquals(details.getData().get("caseHandedOffToLegacySite"),"Yes");
    }

    @Test
    public void shouldMigrateCasesOfOtherToDefalult() {
        when(dataMigrationService.accepts()).thenReturn(candidate -> true);
        when(idamRepository.generateUserToken()).thenReturn(USER_TOKEN);
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        Map<String, Object> caseDatas = new HashMap<>();
        caseDatas.put("applicationType","Personal");
        caseDatas.put("caseType", "intestacy");
        caseDatas.put("primaryApplicantRelationshipToDeceased", "adoptedChild");
        caseDatas.put("primaryApplicantAdoptionInEnglandOrWales", "No");
        when(details.getData()).thenReturn(caseDatas);
        List<CaseDetails> caseDetails = new ArrayList<>();
        caseDetails.add(details);
        when(elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE)).thenReturn(caseDetails);
        List<CaseDetails> listOfCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(listOfCaseDetails);
        when(coreCaseDataService.update(USER_TOKEN, EVENT_ID, EVENT_SUMMARY,
                                        EVENT_DESCRIPTION, CASE_TYPE, details)).thenReturn(details);
        caseMigrationProcessor.migrateCases(CASE_TYPE);
        verify(coreCaseDataService, times(1))
            .update(USER_TOKEN,
                    EVENT_ID,
                    EVENT_SUMMARY,
                    EVENT_DESCRIPTION,
                    CASE_TYPE,
                    details);
        assertEquals(details.getData().get("caseHandedOffToLegacySite"),"No");
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
