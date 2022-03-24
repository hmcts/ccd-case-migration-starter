package uk.gov.hmcts.reform.migration.ccd;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.*;
import uk.gov.hmcts.reform.migration.auth.AuthUtil;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;


@Slf4j
@Service
public class CoreCaseDataService {

    private String jurisdiction;
    @Value("${migration.caseType}")
    private String caseType;

    @Autowired
    private IdamClient idamClient;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    int pagesize = 50;

    public CaseDetails fetchOne(String authorisation, String caseId) {
        return coreCaseDataApi.getCase(authorisation, authTokenGenerator.generate(), caseId);
    }

    public List<CaseDetails> fetchAll(String authorisation, String userId) {
        int numberOfPages = getNumberOfPages(authorisation, userId, new HashMap<>());
        return IntStream.rangeClosed(1, numberOfPages).boxed()
            .flatMap(pageNumber -> fetchPage(authorisation, userId, pageNumber).stream())
            .collect(Collectors.toList());
    }

    public List<CaseDetails> fetchAllForDay(String authorisation, String userId, String day) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.size(1);
        searchBuilder.query(QueryBuilders.boolQuery().must(matchQuery(
            "created_date", day)));

        SearchResult searchResult = coreCaseDataApi.searchCases(authorisation, authTokenGenerator.generate(), "Benefit", searchBuilder.toString());
        int total = searchResult.getTotal();
        log.info("Total for " + day + " is " + total);
        searchBuilder.from(pagesize);
        int numberOfPages = total/pagesize;

        List<CaseDetails> caseDetails = IntStream.rangeClosed(0, numberOfPages).boxed()
            .flatMap(pageNumber -> fetchPageEs(authorisation, userId, pageNumber, day).stream())
            .collect(Collectors.toList());
        return caseDetails;
    }

    private int getNumberOfPages(String authorisation, String userId, Map<String, String> searchCriteria) {
        PaginatedSearchMetadata metadata = coreCaseDataApi.getPaginationInfoForSearchForCaseworkers(authorisation,
            authTokenGenerator.generate(), userId, jurisdiction, caseType, searchCriteria);
        return metadata.getTotalPagesCount();
    }

    private List<CaseDetails> fetchPage(String authorisation, String userId, int pageNumber) {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("page", String.valueOf(pageNumber));
        return coreCaseDataApi.searchForCaseworker(authorisation, authTokenGenerator.generate(), userId, jurisdiction,
            caseType, searchCriteria);
    }

    private List<CaseDetails> fetchPageEs(String authorisation, String userId, int pageNumber, String day) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.size(pagesize);
        searchBuilder.from(pageNumber * pagesize);
        searchBuilder.query(QueryBuilders.boolQuery().must(matchQuery(
            "created_date", day)));

        List<CaseDetails> caseDetails = coreCaseDataApi.searchCases(authorisation, authTokenGenerator.generate(), "Benefit", searchBuilder.toString()).getCases();
        return caseDetails;
    }

    public CaseDetails fetchOldestCase(String authorisation, String userId) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.size(pagesize);
        searchBuilder.sort("created_date", SortOrder.ASC);
        searchBuilder.query(QueryBuilders.boolQuery());

        List<CaseDetails> caseDetails = coreCaseDataApi.searchCases(authorisation, authTokenGenerator.generate(), "Benefit", searchBuilder.toString()).getCases();
        return caseDetails.get(0);
    }

    public CaseDetails update(String authorisation, String caseId, String eventId, String eventSummary, String eventDescription, Object data) {
        UserDetails userDetails = idamClient.getUserDetails(AuthUtil.getBearerToken(authorisation));

        StartEventResponse startEventResponse = coreCaseDataApi.startEventForCaseWorker(
            AuthUtil.getBearerToken(authorisation),
            authTokenGenerator.generate(),
            userDetails.getId(),
            jurisdiction,
            caseType,
            caseId,
            eventId);

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(
                Event.builder()
                    .id(startEventResponse.getEventId())
                    .summary(eventSummary)
                    .description(eventDescription)
                    .build()
            ).data(data)
            .build();

        return coreCaseDataApi.submitEventForCaseWorker(
            AuthUtil.getBearerToken(authorisation),
            authTokenGenerator.generate(),
            userDetails.getId(),
            jurisdiction,
            caseType,
            caseId,
            true,
            caseDataContent);
    }
}
