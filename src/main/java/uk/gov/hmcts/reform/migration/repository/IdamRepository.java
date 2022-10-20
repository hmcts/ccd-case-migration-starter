package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@Repository
@Slf4j
public class IdamRepository {

    private final IdamClient idamClient;

    private final String idamUsername;

    private final String idamPassword;

    @Autowired
    public IdamRepository(@Value("${migration.idam.username}") String idamUsername,
                          @Value("${migration.idam.password}") String idamPassword,
                          IdamClient idamClient) {
        this.idamUsername = idamUsername;
        this.idamPassword = idamPassword;
        this.idamClient = idamClient;
    }

    public String generateUserToken() {
        return idamClient.authenticateUser(idamUsername, idamPassword);
    }

}
