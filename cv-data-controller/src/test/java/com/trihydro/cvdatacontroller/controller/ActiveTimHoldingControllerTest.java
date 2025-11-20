package com.trihydro.cvdatacontroller.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.trihydro.library.helpers.DateStringNotInISO8601FormatException;
import com.trihydro.library.helpers.DateTimeHelper;
import com.trihydro.library.helpers.DateTimeHelperImpl;
import com.trihydro.library.model.ActiveTimHoldingDeleteModel;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;

import com.trihydro.library.helpers.SQLNullHandler;
import com.trihydro.library.model.ActiveTimHolding;
import com.trihydro.library.model.Coordinate;
import com.trihydro.library.tables.TimDbTables;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ActiveTimHoldingControllerTest extends TestBase<ActiveTimHoldingController> {
    @Spy
    private TimDbTables mockTimDbTables = new TimDbTables();
    @Mock
    private SQLNullHandler mockSqlNullHandler;
    @Mock
    private DateTimeHelper dateTimeHelper;

    private Coordinate startCoord;
    private Coordinate endCoord;

    private final DateTimeHelper actualDateTimeHelper = new DateTimeHelperImpl();

    @BeforeEach
    public void setupSubTest() {
        uut.InjectDependencies(mockTimDbTables, mockSqlNullHandler, dateTimeHelper);
        startCoord = new Coordinate(BigDecimal.valueOf(1), BigDecimal.valueOf(2));
        endCoord = new Coordinate(BigDecimal.valueOf(5), BigDecimal.valueOf(6));
    }

    private void setupInsertQueryStatement() {
        doReturn("insert query statement").when(mockTimDbTables).buildInsertQueryStatement(any(), any());
    }

    private void setupPreparedStatement() throws SQLException {
        doReturn(mockPreparedStatement).when(mockConnection).prepareStatement("insert query statement", new String[] {"active_tim_holding_id"});
    }

    @Test
    public void InsertActiveTimHolding_SUCCESS() throws SQLException {
        // Arrange
        setupInsertQueryStatement();
        setupPreparedStatement();
        ActiveTimHolding activeTimHolding = new ActiveTimHolding();
        activeTimHolding.setStartPoint(startCoord);
        activeTimHolding.setEndPoint(endCoord);
        activeTimHolding.setExpirationDateTime("2021-MAR-16'T'09:22'Z'");
        activeTimHolding.setDateCreated("2021-01-01T00:00:00.000Z");

        var now = Instant.parse(activeTimHolding.getDateCreated());
        java.util.Date date_created = java.util.Date.from(now);
        doReturn(date_created).when(dateTimeHelper).convertDate(any());
        when(mockUtility.getTimestampFormat()).thenReturn(timestampFormat);
        Timestamp timestampDateCreated = new Timestamp(date_created.getTime());

        // Act
        ResponseEntity<Long> data = uut.InsertActiveTimHolding(activeTimHolding);

        // Assert
        assertEquals(HttpStatus.OK, data.getStatusCode());
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 2, activeTimHolding.getClientId());// CLIENT_ID
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 3, activeTimHolding.getDirection());// DIRECTION
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 4, activeTimHolding.getRsuTarget());// RSU_TARGET
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 5, activeTimHolding.getSatRecordId());// SAT_RECORD_ID
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 6, activeTimHolding.getStartPoint().getLatitude());// START_LATITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 7, activeTimHolding.getStartPoint().getLongitude());// START_LONGITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 8, activeTimHolding.getEndPoint().getLatitude());// END_LATITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 9, activeTimHolding.getEndPoint().getLongitude());// END_LONGITUDE
        verify(mockSqlNullHandler).setIntegerOrNull(mockPreparedStatement, 10, activeTimHolding.getRsuIndex());// RSU_INDEX
        verify(mockSqlNullHandler).setTimestampOrNull(mockPreparedStatement, 11, timestampDateCreated);// DATE_CREATED
        verify(mockSqlNullHandler).setIntegerOrNull(mockPreparedStatement, 12, activeTimHolding.getProjectKey());// PROJECT_KEY
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 13, timestampFormat.format(date_created));// EXPIRATION_DATE
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 14, activeTimHolding.getPacketId());// PACKET_ID
    }

    @Test
    public void InsertActiveTimHolding_ExistingSDX() throws SQLException {
        // Arrange
        setupInsertQueryStatement();
        setupPreparedStatement();
        ActiveTimHolding activeTimHolding = new ActiveTimHolding();
        activeTimHolding.setSatRecordId("satRecordId");
        activeTimHolding.setClientId("clientId");
        activeTimHolding.setDirection("direction");
        activeTimHolding.setStartPoint(startCoord);
        activeTimHolding.setEndPoint(endCoord);
        activeTimHolding.setDateCreated("2021-01-01T00:00:00.000Z");
        doReturn(null).when(mockDbInteractions).executeAndLog(mockPreparedStatement, "active tim holding");
        doReturn(-99l).when(mockRs).getLong("ACTIVE_TIM_HOLDING_ID");

        var now = Instant.parse(activeTimHolding.getDateCreated());
        java.util.Date date_created = java.util.Date.from(now);
        doReturn(date_created).when(dateTimeHelper).convertDate(activeTimHolding.getDateCreated());
        Timestamp timestampDateCreated = new Timestamp(date_created.getTime());

        String query = "select active_tim_holding_id from active_tim_holding";
        query += " where sat_record_id = '" + activeTimHolding.getSatRecordId();
        query += "' and client_id = '" + activeTimHolding.getClientId();
        query += "' and direction = '" + activeTimHolding.getDirection() + "'";

        // Act
        ResponseEntity<Long> data = uut.InsertActiveTimHolding(activeTimHolding);

        // Assert
        assertEquals(HttpStatus.OK, data.getStatusCode());
        assertEquals(Long.valueOf(-99), data.getBody());
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 2, activeTimHolding.getClientId());// CLIENT_ID
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 3, activeTimHolding.getDirection());// DIRECTION
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 4, activeTimHolding.getRsuTarget());// RSU_TARGET
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 5, activeTimHolding.getSatRecordId());// SAT_RECORD_ID
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 6, activeTimHolding.getStartPoint().getLatitude());// START_LATITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 7, activeTimHolding.getStartPoint().getLongitude());// START_LONGITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 8, activeTimHolding.getEndPoint().getLatitude());// END_LATITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 9, activeTimHolding.getEndPoint().getLongitude());// END_LONGITUDE
        verify(mockSqlNullHandler).setIntegerOrNull(mockPreparedStatement, 10, activeTimHolding.getRsuIndex());// RSU_INDEX
        verify(mockSqlNullHandler).setTimestampOrNull(mockPreparedStatement, 11, timestampDateCreated);// DATE_CREATED
        verify(mockSqlNullHandler).setIntegerOrNull(mockPreparedStatement, 12, activeTimHolding.getProjectKey());

        verify(mockStatement).executeQuery(query);
    }

    @Test
    public void InsertActiveTimHolding_ExistingRSU() throws SQLException {
        // Arrange
        setupInsertQueryStatement();
        setupPreparedStatement();
        ActiveTimHolding activeTimHolding = new ActiveTimHolding();
        activeTimHolding.setRsuTarget("10.10.10.1");
        activeTimHolding.setClientId("clientId");
        activeTimHolding.setDirection("direction");
        activeTimHolding.setStartPoint(startCoord);
        activeTimHolding.setEndPoint(endCoord);
        activeTimHolding.setDateCreated("2021-01-01T00:00:00.000Z");
        doReturn(null).when(mockDbInteractions).executeAndLog(mockPreparedStatement, "active tim holding");
        doReturn(-99l).when(mockRs).getLong("ACTIVE_TIM_HOLDING_ID");

        var now = Instant.parse(activeTimHolding.getDateCreated());
        java.util.Date date_created = java.util.Date.from(now);
        doReturn(date_created).when(dateTimeHelper).convertDate(activeTimHolding.getDateCreated());
        Timestamp timestampDateCreated = new Timestamp(date_created.getTime());

        String query = "select active_tim_holding_id from active_tim_holding";
        query += " where rsu_target = '" + activeTimHolding.getRsuTarget();
        query += "' and client_id = '" + activeTimHolding.getClientId();
        query += "' and direction = '" + activeTimHolding.getDirection() + "'";

        // Act
        ResponseEntity<Long> data = uut.InsertActiveTimHolding(activeTimHolding);

        // Assert
        assertEquals(HttpStatus.OK, data.getStatusCode());
        assertEquals(Long.valueOf(-99), data.getBody());
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 2, activeTimHolding.getClientId());// CLIENT_ID
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 3, activeTimHolding.getDirection());// DIRECTION
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 4, activeTimHolding.getRsuTarget());// RSU_TARGET
        verify(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 5, activeTimHolding.getSatRecordId());// SAT_RECORD_ID
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 6, activeTimHolding.getStartPoint().getLatitude());// START_LATITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 7, activeTimHolding.getStartPoint().getLongitude());// START_LONGITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 8, activeTimHolding.getEndPoint().getLatitude());// END_LATITUDE
        verify(mockSqlNullHandler).setBigDecimalOrNull(mockPreparedStatement, 9, activeTimHolding.getEndPoint().getLongitude());// END_LONGITUDE
        verify(mockSqlNullHandler).setIntegerOrNull(mockPreparedStatement, 10, activeTimHolding.getRsuIndex());// RSU_INDEX
        verify(mockSqlNullHandler).setTimestampOrNull(mockPreparedStatement, 11, timestampDateCreated);// DATE_CREATED

        verify(mockStatement).executeQuery(query);
    }

    @Test
    public void InsertActiveTimHolding_FAIL() throws SQLException {
        // Arrange
        setupInsertQueryStatement();
        setupPreparedStatement();
        ActiveTimHolding activeTimHolding = new ActiveTimHolding();
        activeTimHolding.setStartPoint(startCoord);
        activeTimHolding.setEndPoint(endCoord);
        doThrow(new SQLException()).when(mockSqlNullHandler).setStringOrNull(mockPreparedStatement, 2, activeTimHolding.getClientId());

        // Act
        ResponseEntity<Long> data = uut.InsertActiveTimHolding(activeTimHolding);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        verify(mockPreparedStatement).close();
        verify(mockConnection).close();

    }

    @Test
    public void testInsertActiveTimHolding_TimEndInTableFormat_Success_ReturnsId() throws DateStringNotInISO8601FormatException, ParseException {
        // Arrange
        ActiveTimHolding activeTimHolding = new ActiveTimHolding();
        activeTimHolding.setDateCreated("2021-01-01T00:00:00.000Z");
        activeTimHolding.setDesiredEndDateTime("2021-01-10 00:00:00");
        doReturn(actualDateTimeHelper.convertDate(activeTimHolding.getDateCreated())).when(dateTimeHelper).convertDate(activeTimHolding.getDateCreated());
        doReturn(actualDateTimeHelper.isInTableFormat(activeTimHolding.getDesiredEndDateTime())).when(dateTimeHelper).isInTableFormat(activeTimHolding.getDesiredEndDateTime());
        doReturn(actualDateTimeHelper.convertDateStringFromTableFormatIntoISO8601Format(activeTimHolding.getDesiredEndDateTime())).when(dateTimeHelper).convertDateStringFromTableFormatIntoISO8601Format(activeTimHolding.getDesiredEndDateTime());
        doReturn(actualDateTimeHelper.convertDateStringFromISO8601FormatIntoTimestampObject("2021-01-10T00:00:00.000Z")).when(dateTimeHelper).convertDateStringFromISO8601FormatIntoTimestampObject("2021-01-10T00:00:00.000Z");
        doReturn(10L).when(mockDbInteractions).executeAndLog(mockPreparedStatement, "active tim holding");

        // Act
        ResponseEntity<Long> response = uut.InsertActiveTimHolding(activeTimHolding);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody());
    }

    @Test
    public void testInsertActiveTimHolding_TimEndInTableFormat_FailedToConvert_ReturnsNegative2() throws ParseException {
        // Arrange
        ActiveTimHolding activeTimHolding = new ActiveTimHolding();
        activeTimHolding.setDateCreated("2021-01-01T00:00:00.000Z");
        activeTimHolding.setDesiredEndDateTime("2021-01-10 00:00:00");
        doReturn(actualDateTimeHelper.convertDate(activeTimHolding.getDateCreated())).when(dateTimeHelper).convertDate(any());
        doReturn(actualDateTimeHelper.isInTableFormat(activeTimHolding.getDesiredEndDateTime())).when(dateTimeHelper).isInTableFormat(activeTimHolding.getDesiredEndDateTime());
        doThrow(new RuntimeException()).when(dateTimeHelper).convertDateStringFromTableFormatIntoISO8601Format(activeTimHolding.getDesiredEndDateTime());

        // Act
        ResponseEntity<Long> response = uut.InsertActiveTimHolding(activeTimHolding);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(-2L, response.getBody());
    }

    @Test
    public void testInsertActiveTimHolding_TimEndInInvalidFormat_ReturnsNegativeThree() throws ParseException,
        DateStringNotInISO8601FormatException {
        // Arrange
        ActiveTimHolding activeTimHolding = new ActiveTimHolding();
        activeTimHolding.setDateCreated("2021-01-01T00:00:00.000Z");
        activeTimHolding.setDesiredEndDateTime("banana");
        doReturn(actualDateTimeHelper.convertDate(activeTimHolding.getDateCreated())).when(dateTimeHelper).convertDate(any());
        doReturn(actualDateTimeHelper.isInTableFormat(activeTimHolding.getDesiredEndDateTime())).when(dateTimeHelper).isInTableFormat(activeTimHolding.getDesiredEndDateTime());
        doThrow(new DateStringNotInISO8601FormatException("invalid format")).when(dateTimeHelper).convertDateStringFromISO8601FormatIntoTimestampObject(activeTimHolding.getDesiredEndDateTime());

        // Act
        ResponseEntity<Long> response = uut.InsertActiveTimHolding(activeTimHolding);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(-3L, response.getBody());
    }

    @Test
    public void getActiveTimHoldingForRsu_SUCCESS() throws SQLException {
        // Arrange

        // Act
        ResponseEntity<List<ActiveTimHolding>> data = uut.getActiveTimHoldingForRsu("ipv4Address");

        // Assert
        assertEquals(HttpStatus.OK, data.getStatusCode());
        Assertions.assertNotNull(data.getBody());
        assertEquals(1, data.getBody().size());
        verify(mockRs).getLong("ACTIVE_TIM_HOLDING_ID");
        verify(mockRs).getString("CLIENT_ID");
        verify(mockRs).getString("DIRECTION");
        verify(mockRs).getString("RSU_TARGET");
        verify(mockRs).getString("SAT_RECORD_ID");
        verify(mockRs).getBigDecimal("START_LATITUDE");
        verify(mockRs).getBigDecimal("START_LONGITUDE");
        verify(mockRs).getBigDecimal("END_LATITUDE");
        verify(mockRs).getBigDecimal("END_LONGITUDE");
        verify(mockRs).getString("DATE_CREATED");
        verify(mockRs).getInt("RSU_INDEX");
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    public void getActiveTimHoldingForRsu_FAIL() throws SQLException {
        // Arrange
        doThrow(new SQLException()).when(mockRs).getLong("ACTIVE_TIM_HOLDING_ID");

        // Act
        ResponseEntity<List<ActiveTimHolding>> data = uut.getActiveTimHoldingForRsu("ipv4Address");

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, data.getStatusCode());
        Assertions.assertNotNull(data.getBody());
        assertEquals(0, data.getBody().size());
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    void getAllRecords_SuccessfulRetrieval_ShouldReturnRecords() throws SQLException {
        // execute
        ResponseEntity<List<ActiveTimHolding>> response = uut.getAllRecords();

        // verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(mockRs).getLong("ACTIVE_TIM_HOLDING_ID");
        verify(mockRs).getString("CLIENT_ID");
        verify(mockRs).getString("DIRECTION");
        verify(mockRs).getString("RSU_TARGET");
        verify(mockRs).getString("SAT_RECORD_ID");
        verify(mockRs).getBigDecimal("START_LATITUDE");
        verify(mockRs).getBigDecimal("START_LONGITUDE");
        verify(mockRs).getBigDecimal("END_LATITUDE");
        verify(mockRs).getBigDecimal("END_LONGITUDE");
        verify(mockRs).getString("DATE_CREATED");
        verify(mockRs).getInt("RSU_INDEX");
        verify(mockStatement).close();
        verify(mockConnection).close();
        verify(mockRs).close();
    }

    @Test
    void deleteActiveTimHolding_SuccessfulExecution_ShouldReturnTrue() {
        // execute
        ResponseEntity<Boolean> response = uut.deleteActiveTimHoldingRecords(new ActiveTimHoldingDeleteModel(List.of(137L, 138L)));

        // verify
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody());
    }
}