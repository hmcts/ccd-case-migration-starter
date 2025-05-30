package uk.gov.hmcts.reform.migration.auth;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AuthUtilTest {

    @Test
    public void shouldGetBearToken() {
        assertThat(AuthUtil.getBearerToken("aaaa"), is("Bearer aaaa"));
    }

    @Test
    public void shouldReturnGetBearToken() {
        assertThat(AuthUtil.getBearerToken("Bearer aaaa"), is("Bearer aaaa"));
    }

    @Test
    public void shouldReturnBlankToken() {
        assertThat(AuthUtil.getBearerToken(""), is(""));
    }
}
