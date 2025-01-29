package uk.gov.hmcts.reform.migration.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdamRepositoryTest {

    private static final String IDAM_USER_NAME = "User123";
    private static final String IDAM_PASS = "Pass";

    private IdamRepository idamRepository;

    @Mock
    private IdamClient idamClient;

    @BeforeEach
    public void setUp() {
        idamRepository = new IdamRepository(IDAM_USER_NAME, IDAM_PASS, idamClient);
    }

    @Test
    public void shouldGenerateUserToken() {
        when(idamClient.getAccessToken(anyString(), anyString())).thenReturn("Test_Auth");
        String authToken = idamRepository.generateUserToken();
        verify(idamClient, times(1)).getAccessToken(IDAM_USER_NAME, IDAM_PASS);
        assertNotNull(authToken);
    }
}
