package uk.gov.hmcts.reform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.domain.common.Party;

@Data
@Builder
@AllArgsConstructor
@SuppressWarnings("ClassTypeParameterName")
public class Respondent<P extends Party> {
    private final P party;
    private final String leadRespondentIndicator;
}
