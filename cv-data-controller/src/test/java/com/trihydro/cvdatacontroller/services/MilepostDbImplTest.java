package com.trihydro.cvdatacontroller.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.helpers.DbInteractions;
import com.trihydro.library.model.Coordinate;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MilepostDbImplTest {
    private Coordinate startPoint;
    private Coordinate endPoint;
    private WydotTim wydotTim;

    @Mock
    protected Connection mockConnection;
    @Mock
    protected Statement mockStatement;
    @Mock
    protected PreparedStatement mockPreparedStatement;
    @Mock
    protected ResultSet mockRs;

    @Mock
    MilepostDbService mockMilepostDbService;
    @Mock
    protected DbInteractions mockDbInteractions;

    @InjectMocks
    MilepostDbImpl uut;

    @BeforeEach
    public void setupSubTest() throws SQLException {
        startPoint = new Coordinate(BigDecimal.valueOf(-1), BigDecimal.valueOf(-2));
        endPoint = new Coordinate(BigDecimal.valueOf(-3), BigDecimal.valueOf(-4));

        // primary connection
        lenient().when(mockConnection.createStatement()).thenReturn(mockStatement);
        lenient().when(mockConnection.prepareStatement(isA(String.class))).thenReturn(mockPreparedStatement);
        lenient().when(mockConnection.prepareStatement(isA(String.class), isA(String[].class)))
                .thenReturn(mockPreparedStatement);
        lenient().doReturn(mockConnection).when(mockDbInteractions).getConnectionPool();
        lenient().doReturn(-1L).when(mockDbInteractions).executeAndLog(isA(PreparedStatement.class),
                isA(String.class));
        lenient().doReturn(true).when(mockDbInteractions).updateOrDelete(mockPreparedStatement);
        lenient().doReturn(true).when(mockDbInteractions).deleteWithPossibleZero(mockPreparedStatement);
        lenient().when(mockStatement.executeQuery(isA(String.class))).thenReturn(mockRs);
        lenient().when(mockPreparedStatement.executeQuery()).thenReturn(mockRs);
        lenient().when(mockRs.next()).thenReturn(true).thenReturn(false);

        uut = new MilepostDbImpl();
        uut.InjectDependencies(mockMilepostDbService, mockDbInteractions);
    }
    private void setupWydotTim() {
        wydotTim = new WydotTim();
        wydotTim.setDirection("direction");
        wydotTim.setRoute("route");
        wydotTim.setStartPoint(startPoint);
        wydotTim.setEndPoint(endPoint);
    }
    private List<com.trihydro.cvdatacontroller.model.Milepost> getMockMilepostList() {
        com.trihydro.cvdatacontroller.model.Milepost milepost = new com.trihydro.cvdatacontroller.model.Milepost();
        List<com.trihydro.cvdatacontroller.model.Milepost> mileposts = new ArrayList<>();
        milepost.setDirection("b");
        milepost.setCommonName("route");
        milepost.setLatitude(43.247754473874586);
        milepost.setLongitude(-106.3873242335008);
        mileposts.add(milepost);
        return mileposts;
    }

    @Test
    public void getMilepostsByStartEndPoint_FAIL_startPoint() {
        // Arrange
        setupWydotTim();
        wydotTim.setStartPoint(null);

        // Act
        List<Milepost> data = uut.getMilepostsByStartEndPoint(wydotTim);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertEquals(0, data.size());
    }

    @Test
    public void getMilepostsByStartEndPoint_FAIL_endPoint() {
        // Arrange
        setupWydotTim();
        wydotTim.setEndPoint(null);

        // Act
        List<Milepost> data = uut.getMilepostsByStartEndPoint(wydotTim);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertEquals(0, data.size());
    }

    @Test
    public void getMilepostsByStartEndPoint_FAIL_direction() {
        // Arrange
        setupWydotTim();
        wydotTim.setDirection(null);

        // Act
        List<Milepost> data = uut.getMilepostsByStartEndPoint(wydotTim);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertEquals(0, data.size());
    }

    @Test
    public void getMilepostsByStartEndPoint_FAIL_route() {
        // Arrange
        setupWydotTim();
        wydotTim.setRoute(null);

        // Act
        List<Milepost> data = uut.getMilepostsByStartEndPoint(wydotTim);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertEquals(0, data.size());
    }

    @Test
    public void getMilepostsByStartEndPoint_SUCCESS() {
        // Arrange
        setupWydotTim();
        doReturn(getMockMilepostList()).when(mockMilepostDbService).getPathWithBuffer(anyString(), any(), any(), any(),
                any(), anyString());

        // Act
        List<Milepost> data = uut.getMilepostsByStartEndPoint(wydotTim);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.size());
    }

    @Test
    public void getMilepostsByPointWithBuffer_SUCCESS() {
        // Arrange
        when(mockMilepostDbService.getPathWithSpecifiedBuffer(anyString(), any(), any(),
                anyString(), anyDouble())).thenReturn(getMockMilepostList());
        MilepostBuffer mpb = new MilepostBuffer();
        mpb.setCommonName("route");
        mpb.setDirection("direction");
        mpb.setPoint(endPoint);
        mpb.setBufferMiles(1d);

        // Act
        List<Milepost> data = uut.getMilepostsByPointWithBuffer(mpb);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.size());
    }

    @Test
    public void getRoutes_SUCCESS() throws SQLException {
        // Arrange
        doReturn("common name").when(mockRs).getString("COMMON_NAME");

        // Act
        List<String> data = uut.getRoutes();

        // Assert
        Assertions.assertEquals(1, data.size());
    }

    @Test
    public void getRoutes_FAIL() throws SQLException {
        // Arrange
        doThrow(new SQLException()).when(mockRs).getString("COMMON_NAME");

        // Act
        List<String> data = uut.getRoutes();

        // Assert
        Assertions.assertEquals(0, data.size());
    }
}
