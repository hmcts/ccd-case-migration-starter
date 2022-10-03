package uk.gov.hmcts.reform.migration.ccd;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CoreCaseDataServiceTest {

    private static final String EVENT_ID = "migrateCase";
    private static final String CASE_TYPE = "CARE_SUPERVISION_EPO";
    private static final String CASE_ID = "123456789";
    private static final String USER_ID = "30";
    private static final String AUTH_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJubGJoN";
    private static final String EVENT_TOKEN = "Bearer aaaadsadsasawewewewew";
    private static final String EVENT_SUMMARY = "Migrate Case";
    private static final String EVENT_DESC = "Migrate Case";

    @InjectMocks
    private CoreCaseDataService underTest;

    @Mock
    CoreCaseDataApi coreCaseDataApi;

    @Mock
    private IdamClient idamClient;

    @Mock
    private AuthTokenGenerator authTokenGenerator;


    @Before
    public void setUp() {
    }

    @Test
    public void shouldUpdateTheCase() {
        // given
        UserDetails userDetails = UserDetails.builder()
            .id("30")
            .email("test@test.com")
            .forename("Test")
            .surname("Surname")
            .build();

        CaseDetails caseDetails3 = createCaseDetails(CASE_ID, "case-3");
        setupMocks(userDetails, caseDetails3.getData());

        //when
        CaseDetails update = underTest.update(AUTH_TOKEN, EVENT_ID, EVENT_SUMMARY, EVENT_DESC, CASE_TYPE, caseDetails3);
        //then
        assertThat(update.getId(), is(Long.parseLong(CASE_ID)));
        assertThat(update.getData().get("solicitorEmail"), is("Padmaja.Ramisetti@hmcts.net"));
        assertThat(update.getData().get("solicitorName"), is("PADMAJA"));
        assertThat(update.getData().get("solicitorReference"), is("LL02"));
        assertThat(update.getData().get("applicantLName"), is("Mamidi"));
        assertThat(update.getData().get("applicantFMName"), is("Prashanth"));
        assertThat(update.getData().get("appRespondentFMName"), is("TestRespondant"));
    }

    private CaseDetails createCaseDetails(String id, String value) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("solicitorEmail", "Padmaja.Ramisetti@hmcts.net");
        data.put("solicitorName", "PADMAJA");
        data.put("solicitorReference", "LL02");
        data.put("applicantLName", "Mamidi");
        data.put("applicantFMName", "Prashanth");
        data.put("appRespondentFMName", "TestRespondant");
        return CaseDetails.builder()
            .id(Long.valueOf(id))
            .data(data)
            .build();
    }

    private void setupMocks(UserDetails userDetails, Map<String, Object> data) {
        when(idamClient.getUserDetails(AUTH_TOKEN)).thenReturn(userDetails);

        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);

        StartEventResponse startEventResponse = StartEventResponse.builder()
            .eventId(EVENT_ID)
            .token(EVENT_TOKEN)
            .build();

        when(coreCaseDataApi.startEventForCaseWorker(AUTH_TOKEN, AUTH_TOKEN, "30",
                                                     null, CASE_TYPE, CASE_ID, EVENT_ID
        ))
            .thenReturn(startEventResponse);

        CaseDataContent caseDataContent = CaseDataContent.builder()
            .event(Event.builder()
                       .id(EVENT_ID)
                       .description(EVENT_DESC)
                       .summary(EVENT_SUMMARY)
                       .build())
            .eventToken(EVENT_TOKEN)
            .data(data)
            .ignoreWarning(false)
            .build();

        CaseDetails caseDetails = CaseDetails.builder()
            .id(123456789L)
            .data(data)
            .build();
        when(coreCaseDataApi.submitEventForCaseWorker(AUTH_TOKEN, AUTH_TOKEN, USER_ID, null,
                                                      CASE_TYPE, CASE_ID, true, caseDataContent
        )).thenReturn(caseDetails);
    }
}
