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
            .must(existsQuery("data.caseFlags"))
            .should(QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("data.caseFlags.value.caseFlagType", "anonymity"))
                .should(QueryBuilders.matchQuery("data.caseFlags.value.caseFlagType", "detainedImmigrationAppeal"))
                .minimumShouldMatch(1))
            .mustNot(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.caseLevelFlags"))
                    .should(existsQuery("data.appellantLevelFlags")));
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
