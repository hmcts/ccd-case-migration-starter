package uk.gov.hmcts.reform.migration.ccd;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MigrationEvent {
    MIGRATE_WORK_ALLOCATION_R3("migrateWorkAllocationR3"),
    MIGRATE_CASE_FLAGS("migrateToStrategicCaseFlags"),
    MIGRATE_TO_LEGACY_CASE_FLAGS("migrateToLegacyCaseFlags"),
    MIGRATE_CLEAR_CASE_FLAGS("migrateClearCaseFlags"),

    MIGRATE_GS_SEARCH_CRITERIA("migrateGlobalSearchCriteria"),
    @JsonEnumDefaultValue
    UNKNOWN_MIGRATION("unknownMigration");

    @JsonValue
    private final String id;

    MigrationEvent(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
