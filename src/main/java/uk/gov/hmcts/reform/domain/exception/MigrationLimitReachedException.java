package uk.gov.hmcts.reform.domain.exception;

public class MigrationLimitReachedException extends RuntimeException {
    public MigrationLimitReachedException(String message) {
        super(message);
    }
}
