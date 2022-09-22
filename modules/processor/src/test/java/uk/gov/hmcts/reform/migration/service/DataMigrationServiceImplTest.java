package uk.gov.hmcts.reform.migration.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class DataMigrationServiceImplTest {

    private DataMigrationServiceImpl service = new DataMigrationServiceImpl();

    @Test
    public void shouldReturnNullWhenAccepts() {
        assertNull(service.accepts());
    }

    @Test
    public void shouldReturnNullWhenMigrate() {
        assertNull(service.migrate(new HashMap<>()));
    }
}
