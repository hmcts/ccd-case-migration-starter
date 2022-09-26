package uk.gov.hmcts.reform.migration.ccd;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.migration.queries.CcdElasticSearchQueries.oldestCaseQuery;
import static uk.gov.hmcts.reform.migration.queries.CcdElasticSearchQueries.pageForUnsetCaseAccessManagementFieldsFieldsQuery;

import feign.FeignException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.migration.auth.AuthUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreCaseDataService {

    private static final String SSCS_CASE_TYPE = "Asylum";

    @Value("${migration.jurisdiction}")
    private String jurisdiction;

    @Value("${migration.caseType}")
    private String caseType;

    private final IdamClient idamClient;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;

    public CaseDetails fetchOne(String authorisation, String caseId) {
        return coreCaseDataApi.getCase(authorisation, authTokenGenerator.generate(), caseId);
    }

    public List<CaseDetails> fetchNCases(String authorisation, int casesToFetch, long searchFrom) {

        StopWatch stopWatch = StopWatch.createStarted();

        List<CaseDetails> page = fetchPage(authorisation,
            pageForUnsetCaseAccessManagementFieldsFieldsQuery(searchFrom, casesToFetch));

        stopWatch.stop();

        log.info("Case search with page size: {} completed in: {} minutes ({} seconds).", casesToFetch,
            stopWatch.getTime(TimeUnit.MINUTES), stopWatch.getTime(TimeUnit.SECONDS));

        return page;

    }

    private List<CaseDetails> fetchPage(String authorisation, SearchSourceBuilder searchSourceBuilder) {
        List<CaseDetails> caseDetails = emptyList();

        try {
            caseDetails = searchCases(authorisation, searchSourceBuilder).getCases();
        } catch (FeignException fe) {
            log.error("Feign Exception message: {} with search string: {}",
                fe.contentUTF8(), searchSourceBuilder);
        }

        return caseDetails;
    }

    public CaseDetails fetchOldestCase(String authorisation) {
        return searchCases(authorisation, oldestCaseQuery())
            .getCases()
            .get(0);
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

    public SearchResult searchCases(String authorisation, SearchSourceBuilder searchBuilder) {
        return coreCaseDataApi.searchCases(
            authorisation,
            authTokenGenerator.generate(),
            SSCS_CASE_TYPE,
            searchBuilder.toString());
    }


    public List<CaseDetails> fetchAll(String authorisation, String userId) {
        int numberOfPages = getNumberOfPages(authorisation, userId, new HashMap<>());
        return IntStream.rangeClosed(1, numberOfPages).boxed()
            .flatMap(pageNumber -> fetchPage(authorisation, userId, pageNumber).stream())
            .collect(Collectors.toList());
    }

    private List<CaseDetails> fetchPage(String authorisation, String userId, int pageNumber) {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("page", String.valueOf(pageNumber));
        return coreCaseDataApi.searchForCaseworker(authorisation, authTokenGenerator.generate(), userId, jurisdiction,
            caseType, searchCriteria);
    }

    private int getNumberOfPages(String authorisation, String userId, Map<String, String> searchCriteria) {
        PaginatedSearchMetadata metadata = coreCaseDataApi.getPaginationInfoForSearchForCaseworkers(authorisation,
            authTokenGenerator.generate(), userId, jurisdiction, caseType, searchCriteria);
        return metadata.getTotalPagesCount();
    }

}
