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

    public static SearchSourceBuilder fetchAllUnsetCaseAccessManagementFieldsCasesQuery() {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .query(unsetCaseAccessManagementFieldsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static BoolQueryBuilder unsetCaseAccessManagementFieldsQuery() {
        return QueryBuilders.boolQuery()
            .must(QueryBuilders.boolQuery()
                    .should(existsQuery("data.appeal.appellant.address.postcode"))
                    .should(existsQuery("data.appeal.appellant.appointee.address.postcode"))
                        .minimumShouldMatch(1))
            .mustNot(
                QueryBuilders.boolQuery()
                    .should(existsQuery("data.CaseAccessCategory"))
                    .should(existsQuery("data.caseManagementCategory"))
                    .should(existsQuery("data.caseManagementLocation"))
                    .should(existsQuery("data.caseNameHmctsRestricted"))
                    .should(existsQuery("data.caseNamePublic"))
                    .should(existsQuery("data.caseNameHmctsInternal"))
                    .should(existsQuery("data.ogdType"))
                    .minimumShouldMatch(7));
    }

    public static SearchSourceBuilder pageForUnsetCaseAccessManagementFieldsFieldsQuery(int pageSize) {
        return SearchSourceBuilder.searchSource()
            .size(pageSize)
            .query(unsetCaseAccessManagementFieldsQuery())
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }

    public static SearchSourceBuilder pageForUnsetCaseAccessManagementFieldsFieldsQuery(Long lastCaseId,
                                                                                        int pageSize) {

        return SearchSourceBuilder.searchSource()
            .size(pageSize)
            .query(unsetCaseAccessManagementFieldsQuery())
            .searchAfter(new Object[] { lastCaseId})
            .sort(REFERENCE_KEYWORD, SortOrder.ASC);
    }
}
