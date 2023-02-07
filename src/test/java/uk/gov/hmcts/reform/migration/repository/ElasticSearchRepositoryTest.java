package uk.gov.hmcts.reform.migration.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchRepositoryTest {

    private static final String USER_TOKEN = "TEST_USER_TOKEN";

    private static final String CASE_TYPE = "CASE_TYPE";

    private static final String AUTH_TOKEN = "Test_Auth_Token";

    private static final String INITIAL_QUERY = """
        {
          "query": {
            "bool": {
               "must_not": {
                 "exists": {
                   "field": "data.caseHandedOffToLegacySite"
                 }
               },
                "should": [
                     {"match": { "state": "CaseCreated" }},
                     {"match": { "state": "CasePaymentFailed" }},
                     {"match": { "state": "Stopped" }},
                     {"match": { "state": "Dormant" }},
                     {"match": { "state": "CasePrinted" }},
                     {"match": { "state": "BOReadyForExamination" }},
                     {"match": { "state": "BOExamining" }},
                     {"match": { "state": "BOCaseStopped" }},
                     {"match": { "state": "BOCaveatPermenant" }},
                     {"match": { "state": "BORegistrarEscalation" }},
                     {"match": { "state": "BOReadyToIssue" }},
                     {"match": { "state": "BOCaseQA" }},
                     {"match": { "state": "BOCaseMatchingIssueGrant" }},
                     {"match": { "state": "BOCaseMatchingExamining" }},
                     {"match": { "state": "BOCaseClosed" }},
                     {"match": { "state": "applyforGrantPaperApplication" }},
                     {"match": { "state": "BOCaseImported" }},
                     {"match": { "state": "BOExaminingReissue" }},
                     {"match": { "state": "BOCaseMatchingReissue" }},
                     {"match": { "state": "BOCaseStoppedReissue" }},
                     {"match": { "state": "BOCaseStoppedAwaitRedec" }},
                     {"match": { "state": "BOCaseMatchingIssueGrant" }},
                     {"match": { "state": "BORedecNotificationSent" }},
                     {"match": { "state": "BOSotGenerated" }}
                ]
            }
          },
          "size": 100,
          "sort": [
            {
              "reference.keyword": "asc"
            }
          ]

            }""";

    private static final String SEARCH_AFTER_QUERY = """
        {
          "query": {
            "bool": {
               "must_not": {
                 "exists": {
                   "field": "data.caseHandedOffToLegacySite"
                 }
               },
                "should": [
                     {"match": { "state": "CaseCreated" }},
                     {"match": { "state": "CasePaymentFailed" }},
                     {"match": { "state": "Stopped" }},
                     {"match": { "state": "Dormant" }},
                     {"match": { "state": "CasePrinted" }},
                     {"match": { "state": "BOReadyForExamination" }},
                     {"match": { "state": "BOExamining" }},
                     {"match": { "state": "BOCaseStopped" }},
                     {"match": { "state": "BOCaveatPermenant" }},
                     {"match": { "state": "BORegistrarEscalation" }},
                     {"match": { "state": "BOReadyToIssue" }},
                     {"match": { "state": "BOCaseQA" }},
                     {"match": { "state": "BOCaseMatchingIssueGrant" }},
                     {"match": { "state": "BOCaseMatchingExamining" }},
                     {"match": { "state": "BOCaseClosed" }},
                     {"match": { "state": "applyforGrantPaperApplication" }},
                     {"match": { "state": "BOCaseImported" }},
                     {"match": { "state": "BOExaminingReissue" }},
                     {"match": { "state": "BOCaseMatchingReissue" }},
                     {"match": { "state": "BOCaseStoppedReissue" }},
                     {"match": { "state": "BOCaseStoppedAwaitRedec" }},
                     {"match": { "state": "BOCaseMatchingIssueGrant" }},
                     {"match": { "state": "BORedecNotificationSent" }},
                     {"match": { "state": "BOSotGenerated" }}
                ]
            }
          },
          "size": 100,
          "sort": [
            {
              "reference.keyword": "asc"
            }
          ]
        ,\"search_after\": [1677777777]
            }""";

    private static final int QUERY_SIZE = 100;
    private static final int CASE_PROCESS_LIMIT = 100;

    private ElasticSearchRepository elasticSearchRepository;

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Before
    public void setUp() {
        elasticSearchRepository = new ElasticSearchRepository(coreCaseDataApi,
                                                              authTokenGenerator,
                                                              QUERY_SIZE,
                                                              CASE_PROCESS_LIMIT);
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
    }

    @Test
    public void shouldReturnSearchResultsForCaseTypeElasticSearch() {
        SearchResult searchResult = mock(SearchResult.class);
        when(coreCaseDataApi.searchCases(
            USER_TOKEN,
            AUTH_TOKEN,
            CASE_TYPE,
            INITIAL_QUERY
        )).thenReturn(searchResult);
        List<CaseDetails> caseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldNotReturnCaseDetailsForCaseTypeWhenSearchResultIsNull() {
        when(coreCaseDataApi.searchCases(
            USER_TOKEN,
            AUTH_TOKEN,
            CASE_TYPE,
            INITIAL_QUERY
        )).thenReturn(null);
        List<CaseDetails> caseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(caseDetails);
        assertEquals(0, caseDetails.size());
    }

    @Test
    public void shouldReturnSearchResultsAndCaseDetailsForCaseTypeElasticSearch() {
        SearchResult searchResult = mock(SearchResult.class);
        List<CaseDetails> caseDetails = new ArrayList<>();
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        caseDetails.add(details);
        when(searchResult.getCases()).thenReturn(caseDetails);
        when(searchResult.getTotal()).thenReturn(1);
        when(coreCaseDataApi.searchCases(
            USER_TOKEN,
            AUTH_TOKEN,
            CASE_TYPE,
            INITIAL_QUERY
        )).thenReturn(searchResult);

        SearchResult searchAfterResult = mock(SearchResult.class);
        when(coreCaseDataApi.searchCases(
            USER_TOKEN,
            AUTH_TOKEN,
            CASE_TYPE,
            SEARCH_AFTER_QUERY
        )).thenReturn(searchAfterResult);
        List<CaseDetails> caseDetails1 = new ArrayList<>();
        CaseDetails details1 = mock(CaseDetails.class);
        caseDetails1.add(details1);
        when(searchAfterResult.getCases()).thenReturn(caseDetails1);

        List<CaseDetails> returnCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(returnCaseDetails);

        verify(authTokenGenerator, times(1)).generate();

        verify(coreCaseDataApi, times(1)).searchCases(USER_TOKEN,
                                                      AUTH_TOKEN,
                                                      CASE_TYPE,
                                                      INITIAL_QUERY);
        verify(coreCaseDataApi, times(1)).searchCases(USER_TOKEN,
                                                      AUTH_TOKEN,
                                                      CASE_TYPE,
                                                      SEARCH_AFTER_QUERY);

        assertEquals(2, returnCaseDetails.size());
    }

    @Test
    public void shouldReturnOnlyInitialCaseDetailsWhenSearchAfterReturnsNullSearchResults() {
        SearchResult searchResult = mock(SearchResult.class);
        List<CaseDetails> caseDetails = new ArrayList<>();
        CaseDetails details = mock(CaseDetails.class);
        when(details.getId()).thenReturn(1677777777L);
        caseDetails.add(details);
        when(searchResult.getCases()).thenReturn(caseDetails);
        when(searchResult.getTotal()).thenReturn(1);
        when(coreCaseDataApi.searchCases(
            USER_TOKEN,
            AUTH_TOKEN,
            CASE_TYPE,
            INITIAL_QUERY
        )).thenReturn(searchResult);

        when(coreCaseDataApi.searchCases(
            USER_TOKEN,
            AUTH_TOKEN,
            CASE_TYPE,
            SEARCH_AFTER_QUERY
        )).thenReturn(null);

        List<CaseDetails> returnCaseDetails = elasticSearchRepository.findCaseByCaseType(USER_TOKEN, CASE_TYPE);
        assertNotNull(returnCaseDetails);

        verify(authTokenGenerator, times(1)).generate();

        verify(coreCaseDataApi, times(1)).searchCases(USER_TOKEN,
                                                      AUTH_TOKEN,
                                                      CASE_TYPE,
                                                      INITIAL_QUERY);
        verify(coreCaseDataApi, times(1)).searchCases(USER_TOKEN,
                                                      AUTH_TOKEN,
                                                      CASE_TYPE,
                                                      SEARCH_AFTER_QUERY);

        assertEquals(1, returnCaseDetails.size());
    }
}
