package com.examplatform.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HealthControllerTest {

    @Test
    void reportsDatabaseUpWhenConnectionIsValid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        var response = new HealthController(dataSource).health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("UP", response.getBody().get("database"));
    }

    @Test
    void reportsOnlyGenericDatabaseStatusWhenConnectionFails()
            throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("sensitive connection detail"));

        var response = new HealthController(dataSource).health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals("DOWN", response.getBody().get("database"));
    }
}
