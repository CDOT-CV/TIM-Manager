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
    public void SelectAllRsus_SUCCESS() throws SQLException {
        // Arrange
        String selectStatement = "select rsu_id, ST_X(ST_AsText(geography)) as longitude, ST_Y(ST_AsText(geography)) as latitude, ipv4_address, primary_route, milepost from rsus order by milepost asc";

        // Act
        ResponseEntity<List<WydotRsu>> data = uut.SelectAllRsus();

        // Assert
        Assertions.assertEquals(HttpStatus.OK, data.getStatusCode());
        verify(mockStatement).executeQuery(selectStatement);
        verify(mockRs).getInt("RSU_ID");
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getString("PRIMARY_ROUTE");
        verify(mockRs).getDouble("MILEPOST");
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void SelectAllRsus_FAIL() throws SQLException {
        // Arrange
        String selectStatement = "select rsu_id, ST_X(ST_AsText(geography)) as longitude, ST_Y(ST_AsText(geography)) as latitude, ipv4_address, primary_route, milepost from rsus order by milepost asc";
        doThrow(new SQLException()).when(mockRs).getInt("RSU_ID");
        // Act
        ResponseEntity<List<WydotRsu>> data = uut.SelectAllRsus();

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockStatement).executeQuery(selectStatement);
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void GetFullRsusTimIsOn_SUCCESS() throws SQLException {
        // Arrange
        Long timId = -1l;
        String selectStatement = "select rsus.rsu_id, rsu_credentials.username as update_username, " + 
        "rsu_credentials.password as update_password, ST_X(ST_AsText(rsus.geography)) as longitude, " + 
        "ST_Y(ST_AsText(rsus.geography)) as latitude, rsus.ipv4_address, tim_rsu.rsu_index from rsus " + 
        "inner join rsu_credentials on rsu_credentials.credential_id = rsus.credential_id inner join " + 
        "tim_rsu on tim_rsu.rsu_id = rsus.rsu_id where tim_rsu.tim_id = " + timId;

        // Act
        ResponseEntity<List<WydotRsuTim>> data = uut.GetFullRsusTimIsOn(timId);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, data.getStatusCode());
        verify(mockStatement).executeQuery(selectStatement);
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getInt("RSU_INDEX");
        verify(mockRs).getString("UPDATE_USERNAME");
        verify(mockRs).getString("UPDATE_PASSWORD");
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void GetFullRsusTimIsOn_FAIL() throws SQLException {
        // Arrange
        Long timId = -1l;
        String selectStatement = "select rsus.rsu_id, rsu_credentials.username as update_username, " + 
        "rsu_credentials.password as update_password, ST_X(ST_AsText(rsus.geography)) as longitude, " + 
        "ST_Y(ST_AsText(rsus.geography)) as latitude, rsus.ipv4_address, tim_rsu.rsu_index from rsus " + 
        "inner join rsu_credentials on rsu_credentials.credential_id = rsus.credential_id inner join " + 
        "tim_rsu on tim_rsu.rsu_id = rsus.rsu_id where tim_rsu.tim_id = " + timId;
        doThrow(new SQLException()).when(mockRs).getString("IPV4_ADDRESS");

        // Act
        ResponseEntity<List<WydotRsuTim>> data = uut.GetFullRsusTimIsOn(timId);

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockStatement).executeQuery(selectStatement);
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void SelectRsusByRoute_SUCCESS() throws SQLException {
        // Arrange
        String route = "I80";
        String selectStatement = "select rsu_id, ST_X(ST_AsText(geography)) as longitude, " + 
        "ST_Y(ST_AsText(geography)) as latitude, ipv4_address, primary_route, milepost from rsus " + 
        "where primary_route like %'%" + route + "%'";
        // Act
        ResponseEntity<ArrayList<WydotRsu>> data = uut.SelectRsusByRoute(route);

        // Assert
        Assertions.assertEquals(HttpStatus.OK, data.getStatusCode());
        verify(mockStatement).executeQuery(selectStatement);
        verify(mockRs).getInt("RSU_ID");
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getString("ROUTE");
        verify(mockRs).getDouble("MILEPOST");
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void SelectRsusByRoute_FAIL() throws SQLException {
        // Arrange
        String route = "I80";
        String selectStatement = "select rsu_id, ST_X(ST_AsText(geography)) as longitude, " + 
        "ST_Y(ST_AsText(geography)) as latitude, ipv4_address, primary_route, milepost from rsus " + 
        "where primary_route like %'%" + route + "%'";
        doThrow(new SQLException()).when(mockRs).getInt("RSU_ID");
        // Act
        ResponseEntity<ArrayList<WydotRsu>> data = uut.SelectRsusByRoute(route);

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockStatement).executeQuery(selectStatement);
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void GetRsuClaimedIndexes_SUCCESS() throws SQLException {
        // Arrange
        when(mockRs.getInt("RSU_INDEX")).thenReturn(-1);
        var statement = "select rsu_index from active_tim inner join tim_rsu on active_tim.tim_id = tim_rsu.tim_id"
                + " where sat_record_id is null and rsu_id = ?";

        // Act
        var result = uut.GetActiveRsuTimIndexes(123);

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
    public void GetRsuClaimedIndexes_FAIL() throws SQLException {
        // Arrange
        var statement = "select rsu_index from active_tim inner join tim_rsu on active_tim.tim_id = tim_rsu.tim_id"
                + " where sat_record_id is null and rsu_id = ?";
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException());

        // Act
        var result = uut.GetActiveRsuTimIndexes(123);

        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        verify(mockConnection).prepareStatement(statement);
        verify(mockPreparedStatement).setLong(1, 123);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();
    }
}