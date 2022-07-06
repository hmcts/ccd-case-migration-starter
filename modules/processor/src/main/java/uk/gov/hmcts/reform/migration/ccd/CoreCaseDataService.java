package uk.gov.hmcts.reform.migration.ccd;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    private static final int PAGE_SIZE = 50;
    private static final String CREATED_DATE = "created_date";

    @Value("${migration.jurisdiction}")
    private String jurisdiction;

    @Value("${migration.caseType}")
    private String caseType;

    @Value("${migration.indexCases:false}")
    private boolean indexCases;

    private final IdamClient idamClient;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;

    public CaseDetails fetchOne(String authorisation, String caseId) {
        return coreCaseDataApi.getCase(authorisation, authTokenGenerator.generate(), caseId);
    }

    public Optional<Stream<CaseDetails>> fetchAllBetweenDates(String authorisation,
                                                              List<LocalDate> listOfDates,
                                                              boolean parallel) {
        Stream<LocalDate> processingStream = parallel
            ? listOfDates.parallelStream()
            : listOfDates.stream();

        return processingStream
            .map(date -> fetchAllForDay(authorisation, date.toString(), parallel))
            .flatMap(Optional::stream)
            .reduce(Stream::concat);
    }

    public Optional<Stream<CaseDetails>> fetchAllForDay(String authorisation, String day, boolean parallel) {
        int total = searchCases(authorisation, singleCaseQuery(day)).getTotal();

        log.info("Total for " + day + " is " + total);

        if (indexCases) {
            return Optional.empty();
        }

        int numberOfPages = (int) Math.ceil((double) total / PAGE_SIZE);

        Stream<Integer> pageStream = IntStream
            .rangeClosed(0, numberOfPages - 1)
            .boxed();

        if (parallel) {
            log.info("Retrieving pages in parallel.. please wait.");
            pageStream = pageStream.parallel();
        }

        return pageStream
            .map(pageNumber -> fetchPage(authorisation, day, pageNumber).stream())
            .reduce(Stream::concat);
    }

    public CaseDetails fetchOldestCase(String authorisation) {
        List<CaseDetails> cases = searchCases(authorisation, oldestCaseQuery())
            .getCases();
        return cases !=null && cases.size() > 0 ? cases
            .get(0) : null;
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

    private List<CaseDetails> fetchPage(String authorisation, String day, int pageNumber) {
        List<CaseDetails> caseDetails = emptyList();
        SearchSourceBuilder searchBuilder = pageQuery(day, pageNumber);

        log.info("Fetching page no. {} for day: {}", pageNumber + 1, day);

        try {
            caseDetails = searchCases(authorisation, searchBuilder).getCases();
        } catch (FeignException fe) {
            log.error("Feign Exception message: {} with search string: {}",
                fe.contentUTF8(), searchBuilder);
        }

        return caseDetails;
    }

    private SearchResult searchCases(String authorisation, SearchSourceBuilder searchBuilder) {
        return coreCaseDataApi.searchCases(
            authorisation,
            authTokenGenerator.generate(),
            caseType,
            searchBuilder.toString());
    }

    private SearchSourceBuilder oldestCaseQuery() {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .sort(CREATED_DATE, SortOrder.ASC)
            .query(QueryBuilders.boolQuery());
    }

    private SearchSourceBuilder singleCaseQuery(String day) {
        return SearchSourceBuilder.searchSource()
            .size(1)
            .query(QueryBuilders.boolQuery()
                .must(matchQuery(CREATED_DATE, day)));
    }

    private SearchSourceBuilder pageQuery(String day, int pageNumber) {
        return SearchSourceBuilder.searchSource()
            .size(PAGE_SIZE)
            .from(pageNumber * PAGE_SIZE)
            .query(QueryBuilders.boolQuery()
                .must(matchQuery(CREATED_DATE, day)));
    }
}
