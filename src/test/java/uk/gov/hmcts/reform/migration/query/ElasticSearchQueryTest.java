package uk.gov.hmcts.reform.migration.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ElasticSearchQueryTest {

    private static final int QUERY_SIZE = 100;

    @Test
    public void shouldReturnQuery() {
        ElasticSearchQuery elasticSearchQuery =  ElasticSearchQuery.builder()
            .initialSearch(true)
            .size(QUERY_SIZE)
            .build();
        String query = elasticSearchQuery.getQuery();
        assertEquals("{\n"
                         + "  \"query\": {\n"
                         + "    \"match_all\": {}\n"
                         + "  },\n"
                         + "  \"_source\": [\n"
                         + "    \"reference\"\n"
                         + "  ],\n"
                         + "  \"size\": 100,\n"
                         + "  \"sort\": [\n"
                         + "    {\n"
                         + "      \"reference.keyword\": \"asc\"\n"
                         + "    }\n"
                         + "  ]\n"
                         + "\n"
                         + "}", query);
    }

    @Test
    public void shouldReturnSearchAfterQuery() {
        ElasticSearchQuery elasticSearchQuery =  ElasticSearchQuery.builder()
            .initialSearch(false)
            .size(QUERY_SIZE)
            .searchAfterValue("1677777777")
            .build();
        String query = elasticSearchQuery.getQuery();
        assertEquals("{\n"
                         + "  \"query\": {\n"
                         + "    \"match_all\": {}\n"
                         + "  },\n"
                         + "  \"_source\": [\n"
                         + "    \"reference\"\n"
                         + "  ],\n"
                         + "  \"size\": 100,\n"
                         + "  \"sort\": [\n"
                         + "    {\n"
                         + "      \"reference.keyword\": \"asc\"\n"
                         + "    }\n"
                         + "  ]\n"
                         + ",\"search_after\": [1677777777]\n"
                         + "}", query);
    }
}
