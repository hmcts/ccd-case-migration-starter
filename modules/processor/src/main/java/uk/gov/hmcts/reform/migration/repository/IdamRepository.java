package uk.gov.hmcts.reform.migration.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@Repository
@Slf4j
public class IdamRepository {

    private final IdamClient idamClient;

    @Value("${migration.idam.username}")
    private String idamUsername;

    @Value("${migration.idam.password}")
    private String idamPassword;

    public IdamRepository(IdamClient idamClient) {
        this.idamClient = idamClient;
    }

    public String generateUserToken() {
        return idamClient.authenticateUser(idamUsername, idamPassword);
    }

}
