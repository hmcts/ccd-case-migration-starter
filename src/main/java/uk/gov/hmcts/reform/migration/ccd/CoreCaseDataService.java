package uk.gov.hmcts.reform.migration.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.migration.auth.AuthUtil;

@Service
public class CoreCaseDataService {

    @Autowired
    private IdamClient idamClient;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

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
