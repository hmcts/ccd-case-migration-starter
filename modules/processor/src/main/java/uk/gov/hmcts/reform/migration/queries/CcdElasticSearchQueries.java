package uk.gov.hmcts.reform.migration.queries;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

public class CcdElasticSearchQueries {

    private CcdElasticSearchQueries() {
    }

    private static final String CREATED_DATE = "created_date";

    public static final String REFERENCE_KEYWORD = "reference.keyword";

    public static SearchSourceBuilder oldestCaseQuery() {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .sort(CREATED_DATE, SortOrder.ASC)
            .query(QueryBuilders.boolQuery());
    }

    public static SearchSourceBuilder pageQuery(String day, int pageNumber, int pageSize) {
        return SearchSourceBuilder.searchSource()
            .size(pageSize)
            .from(pageNumber * pageSize)
            .query(QueryBuilders.boolQuery()
                .must(matchQuery(CREATED_DATE, day)));
    }

    public static SearchSourceBuilder fetchAllUnmigratedGlobalSearchCasesQuery() {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .query(missingSearchCriteriaFieldsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static SearchSourceBuilder fetchAllCaseNameInternalCasesQuery() {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .query(missingCaseNameInternalFieldsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static SearchSourceBuilder fetchAllUnmigratedCaseFlagsInternalCasesQuery() {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .query(caseFlagsInternalFieldsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static SearchSourceBuilder fetchAllExistingCaseFlagCasesQuery() {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .query(existingCaseFlagsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static SearchSourceBuilder fetchAllExistingLegacyCaseFlagCasesQuery() {
        return SearchSourceBuilder.searchSource()
            .query(existingLegacyCaseFlagsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static BoolQueryBuilder missingSearchCriteriaFieldsQuery() {
        return QueryBuilders.boolQuery()
            .mustNot(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.SearchCriteria")));
    }

    public static BoolQueryBuilder missingCaseNameInternalFieldsQuery() {
        return QueryBuilders.boolQuery()
            .mustNot(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.caseNameHmctsInternal")));
    }

    public static BoolQueryBuilder caseFlagsInternalFieldsQuery() {

        return QueryBuilders.boolQuery()
            .must(existsQuery("data.legacyCaseFlags"))
            .must(QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("data.legacyCaseFlags.value.legacyCaseFlagType", "anonymity"))
                .should(QueryBuilders.matchQuery("data.legacyCaseFlags.value.legacyCaseFlagType", "complexCase"))
                .should(QueryBuilders.matchQuery("data.legacyCaseFlags.value.legacyCaseFlagType", "detainedImmigrationAppeal"))
                .should(QueryBuilders.matchQuery("data.legacyCaseFlags.value.legacyCaseFlagType", "foreignNationalOffender"))
                .should(QueryBuilders.matchQuery("data.legacyCaseFlags.value.legacyCaseFlagType", "potentiallyViolentPerson"))
                .should(QueryBuilders.matchQuery("data.legacyCaseFlags.value.legacyCaseFlagType", "unacceptableCustomerBehaviour"))
                .should(QueryBuilders.matchQuery("data.legacyCaseFlags.value.legacyCaseFlagType", "unaccompaniedMinor"))
                .minimumShouldMatch(1))
            .mustNot(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.caseFlags.details"))
                    .should(existsQuery("data.appellantLevelFlags")));
    }

    public static BoolQueryBuilder existingCaseFlagsQuery() {
        return QueryBuilders.boolQuery()
            .must(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.caseFlags")));
    }

    public static BoolQueryBuilder existingLegacyCaseFlagsQuery() {
        return QueryBuilders.boolQuery()
            .must(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.legacyCaseFlags")))
            .mustNot(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.caseFlags")));
    }

    public static SearchSourceBuilder pageForUnsetCaseAccessManagementFieldsFieldsQuery(int pageSize) {
        return SearchSourceBuilder.searchSource()
            .size(pageSize)
            .query(missingSearchCriteriaFieldsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static SearchSourceBuilder pageForUnsetCaseAccessManagementFieldsFieldsQuery(Long lastCaseId,
                                                                                        int pageSize) {

        return SearchSourceBuilder.searchSource()
            .size(pageSize)
            .query(missingSearchCriteriaFieldsQuery())
            .searchAfter(new Object[] { lastCaseId})
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static SearchSourceBuilder pageForUnsetCaseFieldsFieldsQuery(Long lastCaseId, int pageSize,
                                                                        BoolQueryBuilder queryBuilder) {

        return SearchSourceBuilder.searchSource()
            .size(pageSize)
            .query(queryBuilder)
            .searchAfter(new Object[] { lastCaseId})
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }
}
