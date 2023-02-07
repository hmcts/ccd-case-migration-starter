package uk.gov.hmcts.reform.migration.query;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.TestCase.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchQueryTest {

    private static final int QUERY_SIZE = 100;

    @Test
    public void shouldReturnQuery() {
        ElasticSearchQuery elasticSearchQuery =  ElasticSearchQuery.builder()
            .initialSearch(true)
            .size(QUERY_SIZE)
            .build();
        String query = elasticSearchQuery.getQuery();
        assertEquals("""
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

            }""", query);
    }

    @Test
    public void shouldReturnSearchAfterQuery() {
        ElasticSearchQuery elasticSearchQuery =  ElasticSearchQuery.builder()
            .initialSearch(false)
            .size(QUERY_SIZE)
            .searchAfterValue("1677777777")
            .build();
        String query = elasticSearchQuery.getQuery();
        assertEquals("""
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
            }""", query);
    }
}
