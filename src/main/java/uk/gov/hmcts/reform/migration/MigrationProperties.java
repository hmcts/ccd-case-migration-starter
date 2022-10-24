package uk.gov.hmcts.reform.migration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    private String jurisdiction;
    private String caseType;

    private Integer maxCasesToProcess;
    private Integer numThreads;
    private boolean dryRun;
    private String caseIds;
}
