package com.trihydro.cvdatacontroller.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trihydro.library.helpers.GISConnector;
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
    private final String ROUTES_LIST =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotRoutesList.json";
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
        JsonNode rootNode = objectMapper.readTree(routeJsonString);
        JsonNode pathNode = rootNode.path("features").get(0).path("geometry").path("paths").get(0);
        List<Milepost> mileposts = new ArrayList<>();
        for (JsonNode node : pathNode) {
            Milepost milepost = new Milepost();
            milepost.setCommonName(DESCENDING_ROUTE_ID);
            BigDecimal latitude = new BigDecimal(node.get(1).asText()).setScale(14, RoundingMode.HALF_UP);
            BigDecimal longitude =
                    new BigDecimal(node.get(0).asText()).setScale(14, RoundingMode.HALF_UP);
            milepost.setLatitude(latitude);
            milepost.setLongitude(longitude);
            mileposts.add(milepost);
        }
        return mileposts;
    }

    @BeforeEach
    void setUp() {
        uut = new GISMilepostImpl(gisConnector, objectMapper);
    }

    @Test
    void testGetRoutesList() throws IOException {
        // prepare
        String routesJsonString =
                new String(Files.readAllBytes(Paths.get(ROUTES_LIST)));
        ResponseEntity<String> routesResponse =
                new ResponseEntity<>(routesJsonString, HttpStatus.OK);
        when(gisConnector.getAllRoutes()).thenReturn(routesResponse);

        // execute
        List<String> routes = uut.getRoutes();

        // Verify
        Assertions.assertNotNull(routes);
        Assertions.assertEquals(576, routes.size());
    }

    @Test
    void testGetMilepostsByStartEndPoint() throws IOException {
        // prepare
        String firstMeasureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        String secondMeasureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(SECOND_PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
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

        ResponseEntity<String> firstMeasureResponse =
                new ResponseEntity<>(firstMeasureAtPointJsonString, HttpStatus.OK);
        ResponseEntity<String> secondMeasureResponse =
                new ResponseEntity<>(secondMeasureAtPointJsonString, HttpStatus.OK);
        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(firstMeasureResponse).thenReturn(secondMeasureResponse);
        ResponseEntity<String> routeResponse =
                new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(routeResponse);

        List<Milepost> expectedMileposts = getMockMileposts();


        // execute
        List<Milepost> mileposts = uut.getMilepostsByStartEndPoint(wydotTim);

        // verify
        Assertions.assertNotNull(mileposts);
        Assertions.assertEquals(expectedMileposts.size(), mileposts.size());
        for (int i = 0; i < expectedMileposts.size(); i++) {
            Milepost expected = expectedMileposts.get(i);
            Milepost actual = mileposts.get(i);
            Assertions.assertEquals(expected.getCommonName(), actual.getCommonName());
            Assertions.assertNull(actual.getMilepost());
            Assertions.assertNull(actual.getDirection());
            Assertions.assertEquals(expected.getLatitude(), actual.getLatitude());
            Assertions.assertEquals(expected.getLongitude(), actual.getLongitude());
        }
    }

    @Test
    void testGetMilepostsByStartEndPoint_MismatchingRoutes() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        WydotTim wydotTim = new WydotTim();
        BigDecimal fromLatitude = new BigDecimal("39.613210472000048");
        BigDecimal fromLongitude = new BigDecimal("-104.89573840099996");
        Coordinate fromPoint = new Coordinate(fromLongitude, fromLatitude);
        BigDecimal toLatitude = new BigDecimal("40.73654117648727");
        BigDecimal toLongitude = new BigDecimal("-104.99349024587299");
        Coordinate toPoint = new Coordinate(toLongitude, toLatitude);
        wydotTim.setStartPoint(fromPoint);
        wydotTim.setEndPoint(toPoint);
        wydotTim.setRoute(ASCENDING_ROUTE_ID);

        ResponseEntity<String> measureResponse =
                new ResponseEntity<>(measureAtPointJsonString, HttpStatus.OK);
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

        ResponseEntity<String> measureResponse =
                new ResponseEntity<>(measureAtPointJsonString, HttpStatus.OK);
        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);
        ResponseEntity<String> routeResponse =
                new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(routeResponse);

        List<Milepost> expectedMileposts = getMockMileposts();

        // execute
        List<Milepost> mileposts = uut.getMilepostsByStartEndPoint(wydotTim);

        // verify
        Assertions.assertNotNull(mileposts);
        Assertions.assertEquals(expectedMileposts.size(), mileposts.size());
        for (int i = 0; i < expectedMileposts.size(); i++) {
            Milepost expected = expectedMileposts.get(i);
            Milepost actual = mileposts.get(i);
            Assertions.assertEquals(expected.getCommonName(), actual.getCommonName());
            Assertions.assertNull(actual.getMilepost());
            Assertions.assertNull(actual.getDirection());
            Assertions.assertEquals(expected.getLatitude(), actual.getLatitude());
            Assertions.assertEquals(expected.getLongitude(), actual.getLongitude());
        }
    }

    @Test
    void testGetMilepostsByPointWithBufferForDescendingRoute() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA)));
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
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

        ResponseEntity<String> measureResponse =
                new ResponseEntity<>(measureAtPointJsonString, HttpStatus.OK);
        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);
        ResponseEntity<String> routeResponse =
                new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(routeResponse);

        List<Milepost> expectedMileposts = getMockMileposts();

        // execute
        List<Milepost> mileposts = uut.getMilepostsByPointWithBuffer(mpb);

        // verify
        verify(gisConnector).getRouteBetweenMeasures(
                eq(DESCENDING_ROUTE_ID),
                doubleThat(d -> Math.abs(d - originalMeasure) < 0.0001),
                doubleThat(d -> Math.abs(d - bufferedMeasure) < 0.0001)
        );
        Assertions.assertNotNull(mileposts);
        Assertions.assertEquals(expectedMileposts.size(), mileposts.size());
        for (int i = 0; i < expectedMileposts.size(); i++) {
            Milepost expected = expectedMileposts.get(i);
            Milepost actual = mileposts.get(i);
            Assertions.assertEquals(expected.getCommonName(), actual.getCommonName());
            Assertions.assertNull(actual.getMilepost());
            Assertions.assertNull(actual.getDirection());
            Assertions.assertEquals(expected.getLatitude(), actual.getLatitude());
            Assertions.assertEquals(expected.getLongitude(), actual.getLongitude());
        }
    }

    @Test
    void testGetMilepostsByPointWithBufferForAscendingRoute() throws IOException {
        // prepare
        String measureAtPointJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_ASCENDING_ROUTE_JSON_TEST_DATA)));
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
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

        String expectedCommonName = mpb.getCommonName();

        ResponseEntity<String> measureResponse =
                new ResponseEntity<>(measureAtPointJsonString, HttpStatus.OK);
        when(gisConnector.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);
        ResponseEntity<String> routeResponse =
                new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(gisConnector.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(routeResponse);

        List<Milepost> expectedMileposts = getMockMileposts();

        // execute
        List<Milepost> mileposts = uut.getMilepostsByPointWithBuffer(mpb);

        // verify
        verify(gisConnector).getRouteBetweenMeasures(
                eq(ASCENDING_ROUTE_ID),
                doubleThat(d -> Math.abs(d - originalMeasure) < 0.0001),
                doubleThat(d -> Math.abs(d - bufferedMeasure) < 0.0001)
        );
        Assertions.assertNotNull(mileposts);
        Assertions.assertEquals(expectedMileposts.size(), mileposts.size());
        for (int i = 0; i < expectedMileposts.size(); i++) {
            Milepost expected = expectedMileposts.get(i);
            Milepost actual = mileposts.get(i);
            Assertions.assertEquals(expectedCommonName, actual.getCommonName());
            Assertions.assertNull(actual.getMilepost());
            Assertions.assertNull(actual.getDirection());
            Assertions.assertEquals(expected.getLatitude(), actual.getLatitude());
            Assertions.assertEquals(expected.getLongitude(), actual.getLongitude());
        }
    }
}
