package uk.gov.hmcts.reform.migration.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.migration.auth.AuthUtil;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CoreCaseDataService {

    @Autowired
    private IdamClient idamClient;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    public CaseDetails fetchOne(String authorisation, String caseId) {
        return coreCaseDataApi.getCase(authorisation, authTokenGenerator.generate(), caseId);
    }

    public List<CaseDetails> fetchAll(String authorisation, String userId, String caseType) {
        int numberOfPages = getNumberOfPages(authorisation, userId, caseType, new HashMap<>());
        return IntStream.rangeClosed(1, numberOfPages).boxed()
            .flatMap(pageNumber -> fetchPage(authorisation, userId, caseType, pageNumber).stream())
            .collect(Collectors.toList());
    }

    private int getNumberOfPages(String authorisation, String userId,  String caseType, Map<String, String> searchCriteria) {
        PaginatedSearchMetadata metadata = coreCaseDataApi.getPaginationInfoForSearchForCaseworkers(authorisation,
            authTokenGenerator.generate(), userId, null, caseType, searchCriteria);
        return metadata.getTotalPagesCount();
    }

    private List<CaseDetails> fetchPage(String authorisation, String userId,  String caseType, int pageNumber) {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("page", String.valueOf(pageNumber));
        return coreCaseDataApi.searchForCaseworker(authorisation, authTokenGenerator.generate(), userId, null,
            caseType, searchCriteria);
    }

    public CaseDetails update(String authorisation, String eventId, String eventSummary, String eventDescription, String caseType, CaseDetails caseDetails) {
        String caseId = String.valueOf(caseDetails.getId());
        UserDetails userDetails = idamClient.getUserDetails(AuthUtil.getBearerToken(authorisation));

        StartEventResponse startEventResponse = coreCaseDataApi.startEventForCaseWorker(
            AuthUtil.getBearerToken(authorisation),
            authTokenGenerator.generate(),
            userDetails.getId(),
            null,
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
            ).data(caseDetails.getData())
            .build();

        return coreCaseDataApi.submitEventForCaseWorker(
            AuthUtil.getBearerToken(authorisation),
            authTokenGenerator.generate(),
            userDetails.getId(),
            null,
            caseType,
            caseId,
            true,
            caseDataContent);
    }
}
