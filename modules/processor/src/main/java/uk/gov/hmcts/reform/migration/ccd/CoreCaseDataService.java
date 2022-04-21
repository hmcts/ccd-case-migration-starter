package uk.gov.hmcts.reform.migration.ccd;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import feign.FeignException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.migration.auth.AuthUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreCaseDataService {

    @Value("${migration.jurisdiction}")
    private String jurisdiction;
    @Value("${migration.caseType}")
    private String caseType;

    private final IdamClient idamClient;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;

    int pagesize = 50;

    public CaseDetails fetchOne(String authorisation, String caseId) {
        return coreCaseDataApi.getCase(authorisation, authTokenGenerator.generate(), caseId);
    }

    public List<CaseDetails> fetchAllBetweenDates(String authorisation,
                                                  List<LocalDate> listOfDates,
                                                  boolean parallel) {
        Stream<LocalDate> processingStream = parallel
            ? listOfDates.parallelStream()
            : listOfDates.stream();

        return processingStream
            .map(date -> fetchAllForDay(authorisation, date.toString(), parallel))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public List<CaseDetails> fetchAllForDay(String authorisation, String day, boolean parallel) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.size(1);
        searchBuilder.query(QueryBuilders.boolQuery().must(matchQuery(
            "created_date", day)));

        SearchResult searchResult = coreCaseDataApi.searchCases(authorisation, authTokenGenerator.generate(), "Benefit", searchBuilder.toString());
        int total = searchResult.getTotal();
        log.info("Total for " + day + " is " + total);
        searchBuilder.from(pagesize);
        int numberOfPages = total/pagesize;

        Stream<Integer> pageStream = IntStream
            .rangeClosed(0, numberOfPages - 1)
            .boxed();

        if (parallel) {
            log.info("Retrieving pages in parallel.. please wait.");
            pageStream = pageStream.parallel();
        }

        return pageStream
            .flatMap(pageNumber -> fetchPage(authorisation, pageNumber, day).stream())
            .collect(Collectors.toList());
    }

    private List<CaseDetails> fetchPage(String authorisation, int pageNumber, String day) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.size(pagesize);
        searchBuilder.from(pageNumber * pagesize);
        searchBuilder.query(QueryBuilders.boolQuery().must(matchQuery(
            "created_date", day)));

        List<CaseDetails> caseDetails = emptyList();

        log.info("Fetching page no. {} for day: {}", pageNumber + 1, day);

        try {
            caseDetails = coreCaseDataApi.searchCases(
                authorisation,
                authTokenGenerator.generate(),
                "Benefit",
                searchBuilder.toString())
                .getCases();

        } catch (FeignException fe) {
            log.error("Feign Exception message: {} with search string: {}",
                fe.contentUTF8(), searchBuilder);
        }

        return caseDetails;
    }

    public CaseDetails fetchOldestCase(String authorisation) {
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
