package uk.gov.hmcts.reform.domain.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
