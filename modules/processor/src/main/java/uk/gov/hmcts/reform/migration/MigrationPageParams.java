package uk.gov.hmcts.reform.migration;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MigrationPageParams {

    private final int pageSize;

    private final int maxCasesToProcess;
}
