package uk.gov.hmcts.reform.domain.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TelephoneNumber {
    private final String telephoneNumber;
    private final String telephoneUsageType;
    private final String contactDirection;
}
