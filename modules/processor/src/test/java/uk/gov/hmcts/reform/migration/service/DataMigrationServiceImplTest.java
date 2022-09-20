package uk.gov.hmcts.reform.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DataMigrationServiceImplTest {

    private DataMigrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DataMigrationServiceImpl();
    }

    @Test
    void accepts() {
        assertNull(service.accepts());
    }

    @Test
    void migrate() {
        assertNull(service.migrate(new HashMap<>()));
    }
}
