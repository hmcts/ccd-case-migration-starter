package uk.gov.hmcts.reform.migration.auth;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SuppressWarnings("squid:S1118")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthUtil {

    private static final String BEARER = "Bearer ";

    public static String getBearerToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }

        return token.startsWith(BEARER) ? token : BEARER.concat(token);
    }
}

