package com.trihydro.cvdatacontroller.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.trihydro.library.model.WydotRsu;
import com.trihydro.library.model.WydotRsuTim;

public class RsuControllerTest extends TestBase<RsuController> {

    @Test
    public void selectAllRsus_SUCCESS() throws SQLException {
        // Arrange
        String selectStatement = "SELECT rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "ipv4_address, primary_route, milepost FROM rsus ORDER BY milepost ASC";

        // Act
        ResponseEntity<List<WydotRsu>> data = uut.selectAllRsus();

        // Assert
        Assertions.assertEquals(HttpStatus.OK, data.getStatusCode());

        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).executeQuery();
        verify(mockRs).getInt("RSU_ID");
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getString("PRIMARY_ROUTE");
        verify(mockRs).getDouble("MILEPOST");
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void selectAllRsus_FAIL() throws SQLException {
        // Arrange
        String selectStatement = "SELECT rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "ipv4_address, primary_route, milepost FROM rsus ORDER BY milepost ASC";
        doThrow(new SQLException()).when(mockRs).getInt("RSU_ID");

        // Act
        ResponseEntity<List<WydotRsu>> data = uut.selectAllRsus();

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void getFullRsusTimIsOn_SUCCESS() throws SQLException {
        // Arrange
        Long timId = -1l;
        String selectStatement = "SELECT rsus.rsu_id, rsu_credentials.username AS update_username, "
                + "rsu_credentials.password AS update_password, ST_X(ST_AsText(rsus.geography)) AS longitude, "
                + "ST_Y(ST_AsText(rsus.geography)) AS latitude, rsus.ipv4_address, tim_rsu.rsu_index "
                + "FROM rsus "
                + "INNER JOIN rsu_credentials ON rsu_credentials.credential_id = rsus.credential_id "
                + "INNER JOIN tim_rsu ON tim_rsu.rsu_id = rsus.rsu_id "
                + "WHERE tim_rsu.tim_id = ?";

        // Act
        ResponseEntity<List<WydotRsuTim>> data = uut.getFullRsusTimIsOn(timId);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, data.getStatusCode());
        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).setLong(1, timId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getInt("RSU_INDEX");
        verify(mockRs).getString("UPDATE_USERNAME");
        verify(mockRs).getString("UPDATE_PASSWORD");
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void getFullRsusTimIsOn_FAIL() throws SQLException {
        // Arrange
        Long timId = -1l;
        String selectStatement = "SELECT rsus.rsu_id, rsu_credentials.username AS update_username, "
                + "rsu_credentials.password AS update_password, ST_X(ST_AsText(rsus.geography)) AS longitude, "
                + "ST_Y(ST_AsText(rsus.geography)) AS latitude, rsus.ipv4_address, tim_rsu.rsu_index "
                + "FROM rsus "
                + "INNER JOIN rsu_credentials ON rsu_credentials.credential_id = rsus.credential_id "
                + "INNER JOIN tim_rsu ON tim_rsu.rsu_id = rsus.rsu_id "
                + "WHERE tim_rsu.tim_id = ?";
        doThrow(new SQLException()).when(mockRs).getString("IPV4_ADDRESS");

        // Act
        ResponseEntity<List<WydotRsuTim>> data = uut.getFullRsusTimIsOn(timId);

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).setLong(1, timId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void selectRsusByRoute_SUCCESS() throws SQLException {
        // Arrange
        String route = "I80";
        String selectStatement = "SELECT rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "ipv4_address, primary_route, milepost FROM rsus WHERE primary_route LIKE ?";

        // Act
        ResponseEntity<ArrayList<WydotRsu>> data = uut.selectRsusByRoute(route);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, data.getStatusCode());
        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).setString(1, "%" + route + "%");
        verify(mockPreparedStatement).executeQuery();
        verify(mockRs).getInt("RSU_ID");
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getString("PRIMARY_ROUTE");
        verify(mockRs).getDouble("MILEPOST");
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void selectRsusByRoute_FAIL() throws SQLException {
        // Arrange
        String route = "I80";
        String selectStatement = "SELECT rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "ipv4_address, primary_route, milepost FROM rsus WHERE primary_route LIKE ?";
        doThrow(new SQLException()).when(mockRs).getInt("RSU_ID");

        // Act
        ResponseEntity<ArrayList<WydotRsu>> data = uut.selectRsusByRoute(route);

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).setString(1, "%" + route + "%");
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void getRsuClaimedIndexes_SUCCESS() throws SQLException {
        // Arrange
        when(mockRs.getInt("RSU_INDEX")).thenReturn(-1);
        var statement = "SELECT rsu_index FROM active_tim "
                + "INNER JOIN tim_rsu ON active_tim.tim_id = tim_rsu.tim_id "
                + "WHERE sat_record_id IS NULL AND rsu_id = ?";

        // Act
        var result = uut.getActiveRsuTimIndexes(123);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(1, result.getBody().size());
        Assertions.assertEquals(-1, result.getBody().get(0));
        verify(mockConnection).prepareStatement(statement);
        verify(mockPreparedStatement).setLong(1, 123);
        verify(mockPreparedStatement).executeQuery();
        verify(mockRs).getInt("RSU_INDEX");
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void getRsuClaimedIndexes_FAIL() throws SQLException {
        // Arrange
        var statement = "SELECT rsu_index FROM active_tim "
                + "INNER JOIN tim_rsu ON active_tim.tim_id = tim_rsu.tim_id "
                + "WHERE sat_record_id IS NULL AND rsu_id = ?";
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException());

        // Act
        var result = uut.getActiveRsuTimIndexes(123);

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        verify(mockConnection).prepareStatement(statement);
        verify(mockPreparedStatement).setLong(1, 123);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }

    @Test
    public void selectRsusByGeometry_SUCCESS() throws SQLException {
        // Arrange
        String geometry = "POLYGON((-105 41, -104 41, -104 42, -105 42, -105 41))";
        String selectStatement = "SELECT rsus.rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "primary_route, milepost, ipv4_address, sc.username, sc.password "
                + "FROM rsus "
                + "JOIN snmp_credentials AS sc ON rsus.snmp_credential_id = sc.snmp_credential_id "
                + "JOIN rsu_options AS ro ON rsus.rsu_id = ro.rsu_id "
                + "WHERE ro.tim_deposit = true "
                + "AND ST_Intersects(ST_Buffer(ST_GeomFromText(?), 1), geography)";

        // Act
        ResponseEntity<ArrayList<WydotRsu>> data = uut.selectRsusByGeometry(geometry);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, data.getStatusCode());
        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).setString(1, geometry);
        verify(mockPreparedStatement).executeQuery();
        verify(mockRs).getInt("RSU_ID");
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getString("PRIMARY_ROUTE");
        verify(mockRs).getDouble("MILEPOST");
        verify(mockRs).getString("USERNAME");
        verify(mockRs).getString("PASSWORD");
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void selectRsusByGeometry_FAIL() throws SQLException {
        // Arrange
        String geometry = "POLYGON((-105 41, -104 41, -104 42, -105 42, -105 41))";
        String selectStatement = "SELECT rsus.rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "primary_route, milepost, ipv4_address, sc.username, sc.password "
                + "FROM rsus "
                + "JOIN snmp_credentials AS sc ON rsus.snmp_credential_id = sc.snmp_credential_id "
                + "JOIN rsu_options AS ro ON rsus.rsu_id = ro.rsu_id "
                + "WHERE ro.tim_deposit = true "
                + "AND ST_Intersects(ST_Buffer(ST_GeomFromText(?), 1), geography)";
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException());

        // Act
        ResponseEntity<ArrayList<WydotRsu>> data = uut.selectRsusByGeometry(geometry);

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockConnection).prepareStatement(selectStatement);
        verify(mockPreparedStatement).setString(1, geometry);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }
}