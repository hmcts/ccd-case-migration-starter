package uk.gov.hmcts.reform.migration.queries;

import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

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
                    .should(existsQuery("data.email"))
                        .minimumShouldMatch(1));
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
