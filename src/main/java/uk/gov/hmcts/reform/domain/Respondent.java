package uk.gov.hmcts.reform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.domain.common.Party;

@Data
@Builder
@AllArgsConstructor
public class Respondent<PARTY extends Party> {
    private final PARTY party;
    private final String leadRespondentIndicator;
}
