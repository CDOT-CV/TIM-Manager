package com.trihydro.cvdatacontroller.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trihydro.cvdatacontroller.helpers.GISConnector;
import com.trihydro.cvdatacontroller.model.gisResponse.GisResponse;
import com.trihydro.library.model.Coordinate;
import com.trihydro.library.model.Milepost;

import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class GISMilepostImplTest {
    private final String DESCENDING_ROUTE_ID = "025A_DEC"; // I-25
    private final String ASCENDING_ROUTE_ID = "025A";
    private final String PATH_TO_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotRouteResponseForI25_First30Mileposts.json";
    private final String PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotMeasureAtPointResponse_DescendingRoute.json";
    private final String PATH_TO_MEASURE_AT_POINT_ASCENDING_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotMeasureAtPointResponse_AscendingRoute.json";
    private final String SECOND_PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotMeasureAtPointResponse_DescendingRoute_MatchingMeasure.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    GISConnector gisConnector = Mockito.mock(GISConnector.class);

    @InjectMocks
    GISMilepostImpl uut;

    List<Milepost> getMockMileposts() throws IOException {
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        ObjectMapper objectMapper = new ObjectMapper();
        GisResponse gisResponse = objectMapper.readValue(routeJsonString, GisResponse.class);
        List<List<Double>> path = gisResponse.getFeatures().get(0).getGeometry().getPaths().get(0);
        List<Milepost> mileposts = new ArrayList<>();
        for (List<Double> coordinate : path) {
            Milepost milepost = new Milepost();
            milepost.setCommonName(DESCENDING_ROUTE_ID);
            milepost.setLatitude(coordinate.get(1));
            milepost.setLongitude(coordinate.get(0));
            mileposts.add(milepost);
        }
        return mileposts;
    }

    private WydotTim getMockWydotTim() {
        WydotTim wydotTim = new WydotTim();
        BigDecimal fromLatitude = new BigDecimal("39.613210472000048");
        BigDecimal fromLongitude = new BigDecimal("-104.89573840099996");
        Coordinate fromPoint = new Coordinate(fromLongitude, fromLatitude);
        BigDecimal toLatitude = new BigDecimal("40.73654117648727");
        BigDecimal toLongitude = new BigDecimal("-104.99349024587299");
        Coordinate toPoint = new Coordinate(toLongitude, toLatitude);
        wydotTim.setStartPoint(fromPoint);
        wydotTim.setEndPoint(toPoint);
        wydotTim.setRoute(DESCENDING_ROUTE_ID);
        return wydotTim;
    }

    @BeforeEach
    void setUp() {
        uut = new GISMilepostImpl(gisConnector);
    }

    @Test
    void testGetRoutesList_Success() throws IOException {
        // prepare
        when(gisConnector.getAllRoutes()).thenReturn(new ArrayList<>());

        // execute
        List<String> routes = uut.getRoutes();

        // Verify
        Assertions.assertNotNull(routes);
    }

    @Test
    void testGetMilepostsByStartEndPoint_Success() throws IOException {
        // prepare
        String firstMeasureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        String secondMeasureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(SECOND_PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));

        GisResponse firstMeasureResponse = objectMapper.readValue(firstMeasureAtPointJsonString, GisResponse.class);
        GisResponse secondMeasureResponse = objectMapper.readValue(secondMeasureAtPointJsonString, GisResponse.class);

        WydotTim wydotTim = getMockWydotTim();

        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(firstMeasureResponse).thenReturn(secondMeasureResponse);

        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(getMockMileposts());

        List<Milepost> expectedMileposts = getMockMileposts();


        // execute
        List<Milepost> mileposts = uut.getMilepostsByStartEndPoint(wydotTim);

        // verify
        Assertions.assertNotNull(mileposts);
        Assertions.assertEquals(expectedMileposts.size(), mileposts.size());
    }

    @Test
    void testGetMilepostsByStartEndPoint_Fail() throws IOException {
        // prepare
        WydotTim wydotTim = getMockWydotTim();

        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(null);

        // execute
        List<Milepost> mileposts = uut.getMilepostsByStartEndPoint(wydotTim);

        // verify
        Assertions.assertNotNull(mileposts);
        Assertions.assertEquals(0, mileposts.size());
    }



    @Test
    void testGetMilepostsByStartEndPoint_MismatchingRoutes() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        GisResponse measureResponse = objectMapper.readValue(measureAtPointJsonString, GisResponse.class);
        WydotTim wydotTim = getMockWydotTim();
        wydotTim.setRoute(ASCENDING_ROUTE_ID);

        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);

        // execute
        List<Milepost> mileposts = uut.getMilepostsByStartEndPoint(wydotTim);

        // verify
        Assertions.assertEquals(0, mileposts.size());
    }

    @Test
    void testGetMilepostsByStartEndPoint_SameMeasure() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));

        GisResponse measureResponse = objectMapper.readValue(measureAtPointJsonString, GisResponse.class);
        GisResponse routeGisResponse = objectMapper.readValue(routeJsonString, GisResponse.class);
        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);

        ResponseEntity<GisResponse> routeResponse =
                new ResponseEntity<>(routeGisResponse, HttpStatus.OK);
        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(getMockMileposts());

        WydotTim wydotTim = getMockWydotTim();

        // execute
        List<Milepost> mileposts = uut.getMilepostsByStartEndPoint(wydotTim);

        // verify
        Assertions.assertNotNull(mileposts);
    }

    @Test
    void testGetMilepostsByPointWithBuffer_DescendingRoute() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        GisResponse measureResponse = objectMapper.readValue(measureAtPointJsonString, GisResponse.class);

        double originalMeasure = 198.61099999999999;
        double bufferedMeasure = 198.61099999999999 - 1.0;
        BigDecimal latitude = new BigDecimal("39.613210472000048");
        BigDecimal longitude = new BigDecimal("-104.89573840099996");
        Coordinate point = new Coordinate(latitude, longitude);
        MilepostBuffer mpb = new MilepostBuffer();
        mpb.setBufferMiles(1.0);
        mpb.setCommonName(DESCENDING_ROUTE_ID);
        mpb.setDirection("D");
        mpb.setPoint(point);

        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);

        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(getMockMileposts());

        // execute
        List<Milepost> mileposts = uut.getMilepostsByPointWithBuffer(mpb);

        // verify
        verify(gisConnector).getRouteBetweenMeasures(
                eq(DESCENDING_ROUTE_ID),
                doubleThat(d -> Math.abs(d - originalMeasure) < 0.0001),
                doubleThat(d -> Math.abs(d - bufferedMeasure) < 0.0001)
        );
        Assertions.assertNotNull(mileposts);
    }

    @Test
    void testGetMilepostsByPointWithBuffer_AscendingRoute() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_ASCENDING_ROUTE_JSON_TEST_DATA)));
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));

        GisResponse measureResponse = objectMapper.readValue(measureAtPointJsonString, GisResponse.class);
        GisResponse routeGisResponse = objectMapper.readValue(routeJsonString, GisResponse.class);
        double originalMeasure = 198.61099999999999;
        double bufferedMeasure = 198.61099999999999 + 1.0;
        BigDecimal latitude = new BigDecimal("39.613210472000048");
        BigDecimal longitude = new BigDecimal("-104.89573840099996");
        Coordinate point = new Coordinate(latitude, longitude);
        MilepostBuffer mpb = new MilepostBuffer();
        mpb.setBufferMiles(1.0);
        mpb.setCommonName(ASCENDING_ROUTE_ID);
        mpb.setDirection("I");
        mpb.setPoint(point);

        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);

        ResponseEntity<GisResponse> routeResponse =
                new ResponseEntity<>(routeGisResponse, HttpStatus.OK);
        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(getMockMileposts());

        // execute
        List<Milepost> mileposts = uut.getMilepostsByPointWithBuffer(mpb);

        // verify
        verify(gisConnector).getRouteBetweenMeasures(
                eq(ASCENDING_ROUTE_ID),
                doubleThat(d -> Math.abs(d - originalMeasure) < 0.0001),
                doubleThat(d -> Math.abs(d - bufferedMeasure) < 0.0001)
        );
        Assertions.assertNotNull(mileposts);
    }

    @Test
    void testGetMilepostsByPointWithBuffer_BufferExceedsMax() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_ASCENDING_ROUTE_JSON_TEST_DATA)));
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));

        GisResponse measureResponse = objectMapper.readValue(measureAtPointJsonString, GisResponse.class);
        GisResponse routeGisResponse = objectMapper.readValue(routeJsonString, GisResponse.class);

        double originalMeasure = 198.61099999999999;
        double bufferedMeasureMax = 298.87900000000002;
        BigDecimal latitude = new BigDecimal("39.613210472000048");
        BigDecimal longitude = new BigDecimal("-104.89573840099996");
        Coordinate point = new Coordinate(latitude, longitude);
        MilepostBuffer mpb = new MilepostBuffer();
        mpb.setBufferMiles(150.0);
        mpb.setCommonName(ASCENDING_ROUTE_ID);
        mpb.setDirection("I");
        mpb.setPoint(point);

        String expectedCommonName = mpb.getCommonName();

        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);

        ResponseEntity<GisResponse> routeResponse =
                new ResponseEntity<>(routeGisResponse, HttpStatus.OK);
        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(getMockMileposts());

        // execute
        List<Milepost> mileposts = uut.getMilepostsByPointWithBuffer(mpb);

        // verify
        verify(gisConnector).getRouteBetweenMeasures(
                eq(ASCENDING_ROUTE_ID),
                doubleThat(d -> Math.abs(d - originalMeasure) < 0.0001),
                doubleThat(d -> Math.abs(d - bufferedMeasureMax) < 0.0001)
        );
        Assertions.assertNotNull(mileposts);
    }
}
