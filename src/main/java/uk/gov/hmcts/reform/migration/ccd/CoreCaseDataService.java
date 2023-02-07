package uk.gov.hmcts.reform.migration.ccd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.PaginatedSearchMetadata;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.migration.auth.AuthUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CoreCaseDataService {

    @Autowired
    private IdamClient idamClient;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private CoreCaseDataApi coreCaseDataApi;


    public int getNumberOfPages(String authorisation, String userId, Map<String, String> searchCriteria) {
        PaginatedSearchMetadata metadata = coreCaseDataApi.getPaginationInfoForSearchForCaseworkers(
            authorisation,
            authTokenGenerator.generate(),
            userId,
            "PROBATE",
            "GrantOfRepresentation",
            searchCriteria
        );
        return metadata.getTotalPagesCount();
    }

    public List<CaseDetails> fetchPage(String authorisation, String userId, int pageNumber) {
        try {
            Map<String, String> searchCriteria = new HashMap<>();
            searchCriteria.put("page", String.valueOf(pageNumber));
            return coreCaseDataApi.searchForCaseworker(authorisation, authTokenGenerator.generate(), userId,
                                                       "PROBATE",
                                                       "GrantOfRepresentation", searchCriteria
            );
        } catch (Exception e) {
            log.error("Fetching of cases failed for the page no {} due to: {}", pageNumber, e.getMessage());
        }
        return Collections.emptyList();
    }

    public CaseDetails update(String authorisation, String eventId,
                              String eventSummary,
                              String eventDescription,
                              String caseType,
                              CaseDetails caseDetails) {
        String caseId = String.valueOf(caseDetails.getId());
        UserDetails userDetails = idamClient.getUserDetails(AuthUtil.getBearerToken(authorisation));

        StartEventResponse startEventResponse = coreCaseDataApi.startEventForCaseWorker(
            AuthUtil.getBearerToken(authorisation),
            authTokenGenerator.generate(),
            userDetails.getId(),
            caseDetails.getJurisdiction(),
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
            caseDetails.getJurisdiction(),
            caseType,
            caseId,
            true,
            caseDataContent);
    }
}
