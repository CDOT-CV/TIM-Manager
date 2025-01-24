package com.trihydro.loggerkafkaconsumer.app.services;

import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.trihydro.library.model.WydotRsu;

public class RsuServiceTest extends TestBase<RsuService> {

    @Test
    public void getRsus_SUCCESS() throws SQLException {
        // Arrange

        // Act
        ArrayList<WydotRsu> data = uut.getRsus();

        // Assert
        Assertions.assertEquals(1, data.size());
        verify(mockStatement).executeQuery(
                "select rsu_id, ST_X(ST_AsText(geography)) as longitude, ST_Y(ST_AsText(geography)) " + 
                "as latitude, ipv4_address, primary_route, milepost from rsus order by milepost asc");
        verify(mockRs).getInt("RSU_ID");
        verify(mockRs).getString("IPV4_ADDRESS");
        verify(mockRs).getBigDecimal("LATITUDE");
        verify(mockRs).getBigDecimal("LONGITUDE");
        verify(mockRs).getString("ROUTE");
        verify(mockRs).getDouble("MILEPOST");
        verify(mockStatement).close();
        verify(mockRs).close();
        verify(mockConnection).close();
    }

    @Test
    public void getRsus_FAIL() throws SQLException {
        // Arrange
        doThrow(new SQLException()).when(mockRs).getInt("RSU_ID");
        // Act
        ArrayList<WydotRsu> data = uut.getRsus();

        // Assert
        Assertions.assertEquals(0, data.size());
        verify(mockStatement).executeQuery(
            "select rsu_id, ST_X(ST_AsText(geography)) as longitude, ST_Y(ST_AsText(geography)) " + 
            "as latitude, ipv4_address, primary_route, milepost from rsus order by milepost asc");
        verify(mockStatement).close();
        verify(mockRs).close();
        verify(mockConnection).close();
    }
}