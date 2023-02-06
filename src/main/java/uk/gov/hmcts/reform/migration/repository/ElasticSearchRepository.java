package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.migration.query.ElasticSearchQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class ElasticSearchRepository {

    private final CoreCaseDataApi coreCaseDataApi;

    private final AuthTokenGenerator authTokenGenerator;

    private final int querySize;

    private final int caseProcessLimit;

    @Autowired
    public ElasticSearchRepository(CoreCaseDataApi coreCaseDataApi,
                                   AuthTokenGenerator authTokenGenerator,
                                   @Value("${case-migration.elasticsearch.querySize}") int querySize,
                                   @Value("${case-migration.processing.limit}") int caseProcessLimit) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.authTokenGenerator = authTokenGenerator;
        this.querySize = querySize;
        this.caseProcessLimit = caseProcessLimit;
    }

    public Optional<CaseDetails> findCaseByCaseId(String authorisation, String caseId) {
        try {
            return Optional.ofNullable(coreCaseDataApi.getCase(authorisation, authTokenGenerator.generate(), caseId));
        } catch (Exception ex) {
            log.error("Case {} not found due to: {}", caseId, ex.getMessage());
        }
        return Optional.empty();
    }

    public List<CaseDetails> findCaseByCaseType(String userToken, String caseType) {
        ElasticSearchQuery elasticSearchQuery = ElasticSearchQuery.builder()
            .initialSearch(true)
            .size(querySize)
            .build();

        log.info("Processing the Case Migration search for case type {}.", caseType);
        String authToken = authTokenGenerator.generate();
        SearchResult searchResult = coreCaseDataApi.searchCases(userToken,
                                                                authToken,
                                                                caseType, elasticSearchQuery.getQuery()
        );

        List<CaseDetails> caseDetails = new ArrayList<>();

        if (searchResult != null && searchResult.getTotal() > 0) {
            List<CaseDetails> searchResultCases = searchResult.getCases();
            caseDetails.addAll(searchResultCases);
            String searchAfterValue = searchResultCases.get(searchResultCases.size() - 1).getId().toString();

            boolean keepSearching;
            do {
                ElasticSearchQuery subsequentElasticSearchQuery = ElasticSearchQuery.builder()
                    .initialSearch(false)
                    .size(querySize)
                    .searchAfterValue(searchAfterValue)
                    .build();

                SearchResult subsequentSearchResult =
                    coreCaseDataApi.searchCases(userToken,
                                                authToken,
                                                caseType, subsequentElasticSearchQuery.getQuery()
                    );

                keepSearching = false;
                if (subsequentSearchResult != null) {
                    caseDetails.addAll(subsequentSearchResult.getCases());
                    keepSearching = subsequentSearchResult.getCases().size() > 0;
                    if (keepSearching) {
                        searchAfterValue = caseDetails.get(caseDetails.size() - 1).getId().toString();
                    }
                }
            } while (keepSearching);
        }
        log.info("The Case Migration has processed caseDetails {}.", caseDetails.size());
        return caseDetails;
    }


    public SearchResult fetchFirstPage(String userToken, String caseType, int querySize) {
        ElasticSearchQuery elasticSearchQuery = ElasticSearchQuery.builder()
            .initialSearch(true)
            .size(querySize)
            .build();
        log.info("Fetching the case details from elastic search for case type {}.", caseType);
        String authToken = authTokenGenerator.generate();
        return coreCaseDataApi.searchCases(userToken,
                                           authToken,
                                           caseType, elasticSearchQuery.getQuery()
        );
    }

    public SearchResult fetchNextPage(String userToken,
                                      String caseType,
                                      String searchAfterValue,
                                      int querySize) {

        String authToken = authTokenGenerator.generate();

        ElasticSearchQuery subsequentElasticSearchQuery = ElasticSearchQuery.builder()
            .initialSearch(false)
            .size(querySize)
            .searchAfterValue(searchAfterValue)
            .build();

        SearchResult subsequentSearchResult =
            coreCaseDataApi.searchCases(userToken,
                                        authToken,
                                        caseType, subsequentElasticSearchQuery.getQuery()
            );
        return subsequentSearchResult;
    }
}
