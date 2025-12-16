package com.trihydro.cvdatacontroller.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.trihydro.cvdatacontroller.controller.CdotUpstreamPathController.DistanceCalculator;
import com.trihydro.cvdatacontroller.controller.CdotUpstreamPathController.PathDirection;
import com.trihydro.library.helpers.CdotGisConnector;
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

class CdotUpstreamPathControllerTest {
    private final String DESCENDING_ROUTE_ID = "025A_DEC"; // I-25
    private final String ASCENDING_ROUTE_ID = "025A_DEC";
    private final String PATH_TO_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotRouteResponseForI25_First30Mileposts.json";
    private final String PATH_TO_MEASURE_AT_POINT_DESCENDING_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotMeasureAtPointResponse_DescendingRoute.json";
    private final String PATH_TO_MEASURE_AT_POINT_ASCENDING_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotMeasureAtPointResponse_AscendingRouteRoute.json";

    @Mock
    CdotGisConnector cdotGisService = Mockito.mock(CdotGisConnector.class);

    @InjectMocks
    CdotUpstreamPathController uut;

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
        uut = new CdotUpstreamPathController(cdotGisService);
    }

    @Test
    void testGetMilepostsForRoute() throws IOException {
        // prepare
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        ResponseEntity<String> mockResponse = new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteById(DESCENDING_ROUTE_ID)).thenReturn(mockResponse);
        List<Milepost> expectedMileposts = getMockMileposts();

        // execute
        List<Milepost> mileposts = uut.getMilepostsForRoute(DESCENDING_ROUTE_ID);

        // verify
        verify(cdotGisService).getRouteById(DESCENDING_ROUTE_ID);
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
    void testGetMilepostsByStartEndPoint() throws IOException {
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
        when(cdotGisService.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);
        ResponseEntity<String> routeResponse =
                new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(routeResponse);

        List<Milepost> expectedMileposts = getMockMileposts();


        // execute
        List<Milepost> mileposts = uut.getMilepostsByStartEndPoint(wydotTim).getBody();

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
        mpb.setDirection("I");
        mpb.setPoint(point);

        ResponseEntity<String> measureResponse =
                new ResponseEntity<>(measureAtPointJsonString, HttpStatus.OK);
        when(cdotGisService.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);
        ResponseEntity<String> routeResponse =
                new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(routeResponse);

        List<Milepost> expectedMileposts = getMockMileposts();

        // execute
        List<Milepost> mileposts = uut.getMilepostsByPointWithBuffer(mpb).getBody();

        // verify
        verify(cdotGisService).getRouteBetweenMeasures(
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

        ResponseEntity<String> measureResponse =
                new ResponseEntity<>(measureAtPointJsonString, HttpStatus.OK);
        when(cdotGisService.getMeasureAtPoint(
                any(),
                any()
        )).thenReturn(measureResponse);
        ResponseEntity<String> routeResponse =
                new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteBetweenMeasures(
                anyString(),
                anyDouble(),
                anyDouble()
        )).thenReturn(routeResponse);

        List<Milepost> expectedMileposts = getMockMileposts();

        // execute
        List<Milepost> mileposts = uut.getMilepostsByPointWithBuffer(mpb).getBody();

        // verify
        verify(cdotGisService).getRouteBetweenMeasures(
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
    void testGetPathDirection_ASCENDING()
            throws IOException, CdotUpstreamPathController.MilepostNotFoundException,
            CdotUpstreamPathController.NotEnoughMilepostsException {
        // prepare
        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(0);
        Milepost mp2 = allMileposts.get(1);
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        // execute
        PathDirection direction = uut.getPathDirection(pathMileposts, allMileposts);

        // verify
        Assertions.assertEquals(PathDirection.ASCENDING, direction);
    }

    @Test
    void testGetPathDirection_DESCENDING()
            throws IOException, CdotUpstreamPathController.MilepostNotFoundException,
            CdotUpstreamPathController.NotEnoughMilepostsException {
        // prepare
        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(1);
        Milepost mp2 = allMileposts.get(0);
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        // execute
        PathDirection direction = uut.getPathDirection(pathMileposts, allMileposts);

        // verify
        Assertions.assertEquals(PathDirection.DESCENDING, direction);
    }

    @Test
    void testGetPathDirection_NOT_ENOUGH_MILEPOSTS()
            throws IOException {
        // prepare
        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(0);
        List<Milepost> pathMileposts = List.of(mp1);

        // execute
        assertThrows(CdotUpstreamPathController.NotEnoughMilepostsException.class, () -> {
            uut.getPathDirection(pathMileposts, allMileposts);
        });
    }

    @Test
    void testGetPathDirection_FirstMilepostOfPathNotInAllMileposts()
            throws IOException {
        // prepare
        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = new Milepost();
        mp1.setCommonName(DESCENDING_ROUTE_ID);
        mp1.setLatitude(new BigDecimal("30.12").setScale(14, RoundingMode.HALF_UP));
        mp1.setLongitude(new BigDecimal("-100.34").setScale(14, RoundingMode.HALF_UP));
        Milepost mp2 = allMileposts.get(1);
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        // execute
        assertThrows(CdotUpstreamPathController.MilepostNotFoundException.class, () -> {
            uut.getPathDirection(pathMileposts, allMileposts);
        });
    }

    @Test
    void testGetPathDirection_SecondMilepostOfPathNotInAllMileposts()
            throws IOException {
        // prepare
        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(0);
        Milepost mp2 = new Milepost();
        mp2.setCommonName(DESCENDING_ROUTE_ID);
        mp1.setLatitude(new BigDecimal("30.12").setScale(14, RoundingMode.HALF_UP));
        mp1.setLongitude(new BigDecimal("-100.34").setScale(14, RoundingMode.HALF_UP));
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        // execute
        assertThrows(CdotUpstreamPathController.MilepostNotFoundException.class, () -> {
            uut.getPathDirection(pathMileposts, allMileposts);
        });
    }

    @Test
    void testGetBufferForPath_Ascending_Success() throws IOException {
        // prepare
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        ResponseEntity<String> mockResponse = new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteById(DESCENDING_ROUTE_ID)).thenReturn(mockResponse);

        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(20);
        Milepost mp2 = allMileposts.get(21);
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        double desiredDistanceInMiles = 0.5;

        // execute
        ResponseEntity<List<Milepost>> response =
                uut.getBufferForPath(pathMileposts, DESCENDING_ROUTE_ID, desiredDistanceInMiles);

        // verify
        List<Milepost> buffer = response.getBody();
        assert buffer != null;
        Assertions.assertFalse(buffer.isEmpty());
        double distanceInMiles = getDistanceInMiles(buffer);
        Assertions.assertTrue(distanceInMiles >=
                desiredDistanceInMiles); // buffer should be at least as long as desired distance
        Assertions.assertTrue(distanceInMiles <=
                desiredDistanceInMiles + 1); // buffer should not be much longer than desired distance
    }

    @Test
    void testGetBufferForPath_Ascending_Failure_EndOfAllMilepostsReached() throws IOException {
        // prepare
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        ResponseEntity<String> mockResponse = new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteById(DESCENDING_ROUTE_ID)).thenReturn(mockResponse);

        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(1);
        Milepost mp2 = allMileposts.get(2);
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        double desiredDistanceInMiles = 5.0;

        // execute
        ResponseEntity<List<Milepost>> response =
                uut.getBufferForPath(pathMileposts, DESCENDING_ROUTE_ID, desiredDistanceInMiles);

        // verify
        List<Milepost> buffer = response.getBody();
        Assertions.assertNull(buffer);
    }

    @Test
    void testGetBufferForPath_Descending_Success() throws IOException {
        // prepare
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        ResponseEntity<String> mockResponse = new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteById(DESCENDING_ROUTE_ID)).thenReturn(mockResponse);

        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(1);
        Milepost mp2 = allMileposts.get(0);
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        double desiredDistanceInMiles = 0.5;

        // execute
        ResponseEntity<List<Milepost>> response =
                uut.getBufferForPath(pathMileposts, DESCENDING_ROUTE_ID, desiredDistanceInMiles);

        // verify
        List<Milepost> buffer = response.getBody();
        assert buffer != null;
        Assertions.assertFalse(buffer.isEmpty());
        double distanceInMiles = getDistanceInMiles(buffer);
        Assertions.assertTrue(distanceInMiles >=
                desiredDistanceInMiles); // buffer should be at least as long as desired distance
        Assertions.assertTrue(distanceInMiles <=
                desiredDistanceInMiles + 1); // buffer should not be much longer than desired distance
    }

    @Test
    void testGetBufferForPath_Descending_Failure_EndOfAllMilepostsReached() throws IOException {
        // prepare
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        ResponseEntity<String> mockResponse = new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteById(DESCENDING_ROUTE_ID)).thenReturn(mockResponse);

        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(28);
        Milepost mp2 = allMileposts.get(27);
        List<Milepost> pathMileposts = List.of(mp1, mp2);

        double desiredDistanceInMiles = 5.0;

        // execute
        ResponseEntity<List<Milepost>> response =
                uut.getBufferForPath(pathMileposts, DESCENDING_ROUTE_ID, desiredDistanceInMiles);

        // verify
        List<Milepost> buffer = response.getBody();
        Assertions.assertNull(buffer);
    }

    @Test
    void testGetBufferForPath_PathDirectionIsNull() throws IOException {
        // prepare
        String routeJsonString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        ResponseEntity<String> mockResponse = new ResponseEntity<>(routeJsonString, HttpStatus.OK);
        when(cdotGisService.getRouteById(DESCENDING_ROUTE_ID)).thenReturn(mockResponse);

        List<Milepost> allMileposts = getMockMileposts();
        Milepost mp1 = allMileposts.get(0);
        List<Milepost> pathMileposts = List.of(mp1);

        int desiredDistanceInMiles = 5;

        // execute
        ResponseEntity<List<Milepost>> response =
                uut.getBufferForPath(pathMileposts, DESCENDING_ROUTE_ID, desiredDistanceInMiles);

        // verify
        List<Milepost> buffer = response.getBody();
        Assertions.assertNull(buffer);
    }

    private double getDistanceInMiles(List<Milepost> buffer) {
        return DistanceCalculator.calculateDistanceInMiles(buffer);
    }

}