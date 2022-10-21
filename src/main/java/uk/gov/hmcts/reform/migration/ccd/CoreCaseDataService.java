package uk.gov.hmcts.reform.migration.ccd;

import lombok.extern.slf4j.Slf4j;
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

import java.util.Optional;

@Slf4j
@Service
public class CoreCaseDataService {

    @Autowired
    private IdamClient idamClient;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    public Optional<CaseDetails> fetchOne(String authorisation, String caseId) {
        try {
            return Optional.ofNullable(coreCaseDataApi.getCase(authorisation, authTokenGenerator.generate(), caseId));
        } catch (Exception ex) {
            log.error("Case {} not found due to: {}", caseId, ex.getMessage());
        }
        return Optional.empty();
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
