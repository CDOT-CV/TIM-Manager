package com.trihydro.cvdatacontroller.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trihydro.cvdatacontroller.model.gisResponse.GisResponse;
import com.trihydro.cvdatacontroller.model.gisResponse.GisRoutesResponse;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.service.RestTemplateProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.*;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class GisConnectorTest {
    private final String baseUrl = "https://dtdapps.codot.gov/server/rest/services/LRS/Routes_withDEC/MapServer/exts/LrsServerRounded";
    private final String PATH_TO_MEASURE_AT_POINT_Data =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotMeasureAtPointResponse_DescendingRoute.json";
    private final String PATH_TO_ROUTE_JSON_TEST_DATA =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotRouteResponseForI25_First30Mileposts.json";
    private final String ROUTES_LIST =
            "src/test/resources/com/trihydro/cvdatacontroller/controller/cdotRoutesList.json";
    private final String DESCENDING_ROUTE_ID = "025A_DEC";

    private final String f = "json";
    private final int sr = 4326;
    private final String routeId = "025A";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
    @Mock
    RestTemplateProvider mockRestTemplateProvider = Mockito.mock(RestTemplateProvider.class);;

    @InjectMocks
    GISConnector uut;

    @BeforeEach
    void setUp() {
        when(mockRestTemplateProvider.GetRestTemplate()).thenReturn(mockRestTemplate);
        uut = new GISConnector(mockRestTemplateProvider);
    }

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

    @Test
    void testGetRouteById_Success() throws IOException {
        // prepare
        String expectedTargetUrl = baseUrl + "/Route";
        var expectedUri = UriComponentsBuilder.fromUriString(expectedTargetUrl)
          .queryParam("routeId", routeId)
          .queryParam("outSR", 4326)
          .queryParam("f", f)
          .build(true)
          .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        String mockResponseString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_Data)));
        GisResponse mockGisResponse = objectMapper.readValue(mockResponseString, GisResponse.class);
        ResponseEntity<GisResponse> mockResponse = ResponseEntity.ok(mockGisResponse);
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenReturn(mockResponse);

        // execute
        List<Milepost> response = uut.getRouteById(routeId);

        // verify
        Assertions.assertEquals(0, response.size());
        verify(mockRestTemplate).exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class);
    }

    @Test
    void testGetRouteById_Fail_NullValueResponse() {
        // prepare
        String expectedTargetUrl = baseUrl + "/Route";
        var expectedUri = UriComponentsBuilder.fromUriString(expectedTargetUrl)
          .queryParam("routeId", routeId)
          .queryParam("outSR", 4326)
          .queryParam("f", f)
          .build(true)
          .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        ResponseEntity<GisResponse> mockResponse = ResponseEntity.ok(new GisResponse());
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenReturn(mockResponse);

        // execute
        List<Milepost> response = uut.getRouteById(routeId);

        // verify
        Assertions.assertEquals(0, response.size());
        verify(mockRestTemplate).exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class);
    }

    @Test
    void testGetRouteById_Fail_BadRequest() {
        // prepare
        String expectedTargetUrl = baseUrl + "/Route";

        var expectedTargetUri = UriComponentsBuilder.fromUriString(expectedTargetUrl)
          .queryParam("routeId", routeId)
          .queryParam("outSR", 4326)
          .queryParam("f", f)
          .build(true)
          .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        when(mockRestTemplate.exchange(expectedTargetUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenThrow(
                HttpClientErrorException.BadRequest.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        null,
                        null
                )
        );

        // execute
        List<Milepost> response = uut.getRouteById(routeId);

        // verify
        Assertions.assertEquals(0, response.size());
        verify(mockRestTemplate).exchange(expectedTargetUri, HttpMethod.GET, mockEntity, GisResponse.class);
    }

    @Test
    void testGetAllRoutes_Success() throws IOException {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/Routes/query");
        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("f", "json")
                .build(true)
                .toUri();

        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        String routesJsonString =
                new String(Files.readAllBytes(Paths.get(ROUTES_LIST)));
        GisRoutesResponse routesResponse = objectMapper.readValue(routesJsonString, GisRoutesResponse.class);

        ResponseEntity<GisRoutesResponse> mockResponse = ResponseEntity.ok(routesResponse);
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisRoutesResponse.class)).thenReturn(mockResponse);

        // execute
        List<String> routes = uut.getAllRoutes();

        // Verify
        Assertions.assertNotNull(routes);
        Assertions.assertEquals(576, routes.size());
    }

    @Test
    void testGetAllRoutes_Fail_NullValueResponse() {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/Routes/query");
        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("f", "json")
                .build(true)
                .toUri();

        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        when(mockRestTemplate.exchange(
                expectedUri,
                HttpMethod.GET,
                mockEntity,
                GisRoutesResponse.class
        )).thenReturn(ResponseEntity.ok(new GisRoutesResponse()));


        // execute
        List<String> routes = uut.getAllRoutes();

        // Verify
        Assertions.assertNotNull(routes);
        Assertions.assertEquals(0, routes.size());
    }

    @Test
    void testGetAllRoutes_Fail_BadRequest() {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/Routes/query");
        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("f", "json")
                .build(true)
                .toUri();

        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        when(mockRestTemplate.exchange(
                expectedUri,
                HttpMethod.GET,
                mockEntity,
                GisRoutesResponse.class
        )).thenThrow(
                HttpClientErrorException.BadRequest.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        null,
                        null
                )
        );


        // execute
        List<String> routes = uut.getAllRoutes();

        // Verify
        Assertions.assertNotNull(routes);
        Assertions.assertEquals(0, routes.size());
    }

    @Test
    void testGetMeasureAtPoint_Success() throws IOException {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/MeasureAtPoint?");
        BigDecimal latitude = new BigDecimal("123.456");
        BigDecimal longitude = new BigDecimal("234.567");
        int tolerance = 10000;
        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("x", longitude.toPlainString())
                .queryParam("y", latitude.toPlainString())
                .queryParam("tolerance", tolerance)
                .queryParam("outSR", sr)
                .queryParam("inSR", sr)
                .queryParam("f", "json")
                .build(true)
                .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        String mockResponseString =
                new String(Files.readAllBytes(Paths.get(PATH_TO_MEASURE_AT_POINT_Data)));
        GisResponse mockGisResponse = objectMapper.readValue(mockResponseString, GisResponse.class);
        ResponseEntity<GisResponse> mockResponse = ResponseEntity.ok(mockGisResponse);
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenReturn(mockResponse);

        // execute
        GisResponse response = uut.getMeasureAtPoint(longitude, latitude);

        // verify
        Assertions.assertEquals(mockGisResponse, response);
        verify(mockRestTemplate).exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class);
    }

    @Test
    void testGetMeasureAtPoint_Fail_NullValueResponse() {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/MeasureAtPoint?");
        BigDecimal latitude = new BigDecimal("123.456");
        BigDecimal longitude = new BigDecimal("234.567");
        int tolerance = 10000;
        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("x", longitude.toPlainString())
                .queryParam("y", latitude.toPlainString())
                .queryParam("tolerance", tolerance)
                .queryParam("outSR", sr)
                .queryParam("inSR", sr)
                .queryParam("f", "json")
                .build(true)
                .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        GisResponse mockGisResponse = new GisResponse();
        ResponseEntity<GisResponse> mockResponse = ResponseEntity.ok(mockGisResponse);
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenReturn(mockResponse);

        // execute
        GisResponse response = uut.getMeasureAtPoint(longitude, latitude);

        // verify
        Assertions.assertNull(response);
        verify(mockRestTemplate).exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class);
    }

    @Test
    void testGetMeasureAtPoint_Fail_BadRequest() {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/MeasureAtPoint");
        BigDecimal latitude = new BigDecimal("123.456");
        BigDecimal longitude = new BigDecimal("234.567");
        int tolerance = 10000;

        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("x", longitude.toPlainString())
                .queryParam("y", latitude.toPlainString())
                .queryParam("tolerance", tolerance)
                .queryParam("outSR", sr)
                .queryParam("inSR", sr)
                .queryParam("f", "json")
                .build(true)
                .toUri();

        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        when(mockRestTemplate.exchange(
                expectedUri,
                HttpMethod.GET,
                mockEntity,
                GisResponse.class
        )).thenThrow(
                HttpClientErrorException.BadRequest.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        null,
                        null
                )
        );

        // execute
        GisResponse response = uut.getMeasureAtPoint(longitude, latitude);

        // verify
        Assertions.assertNull(response);
        verify(mockRestTemplate).exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class);
    }

    @Test
    void testGetRouteBetweenMeasures_Success() throws IOException{
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/RouteBetweenMeasures?");
        double startMeasure = 123.456;
        double endMeasure = 123.456;

        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("routeId", routeId)
                .queryParam("fromMeasure", startMeasure)
                .queryParam("toMeasure", endMeasure)
                .queryParam("outSR", sr)
                .queryParam("f", f)
                .build(true)
                .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        String mockResponseString = new String(Files.readAllBytes(Paths.get(PATH_TO_ROUTE_JSON_TEST_DATA)));
        GisResponse mockGisResponse = objectMapper.readValue(mockResponseString, GisResponse.class);
        ResponseEntity<GisResponse> mockResponse = ResponseEntity.ok(mockGisResponse);
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenReturn(mockResponse);

        List<Milepost> expectedMileposts = getMockMileposts();

        // execute
        List<Milepost> response = uut.getRouteBetweenMeasures(routeId, startMeasure, endMeasure);

        // verify
        Assertions.assertNotNull(response);
        Assertions.assertEquals(expectedMileposts.size(), response.size());
        for (int i = 0; i < expectedMileposts.size(); i++) {
            Milepost expected = expectedMileposts.get(i);
            Milepost actual = response.get(i);
            Assertions.assertNull(actual.getMilepost());
            Assertions.assertNull(actual.getDirection());
            Assertions.assertEquals(expected.getLatitude(), actual.getLatitude());
            Assertions.assertEquals(expected.getLongitude(), actual.getLongitude());
        }
    }

    @Test
    void testGetRouteBetweenMeasures_Fail_NullValueResponse() {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/RouteBetweenMeasures?");
        double startMeasure = 123.456;
        double endMeasure = 123.456;

        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("routeId", routeId)
                .queryParam("fromMeasure", startMeasure)
                .queryParam("toMeasure", endMeasure)
                .queryParam("outSR", sr)
                .queryParam("f", f)
                .build(true)
                .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        ResponseEntity<GisResponse> mockResponse = ResponseEntity.ok(new GisResponse());
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenReturn(mockResponse);

        // execute
        List<Milepost> response = uut.getRouteBetweenMeasures(routeId, startMeasure, endMeasure);

        // verify
        Assertions.assertNotNull(response);
        Assertions.assertEquals(0, response.size());
    }

    @Test
    void testGetRouteBetweenMeasures_Fail_BadRequest() {
        // prepare
        URI expectedTargetUrl = URI.create(baseUrl + "/RouteBetweenMeasures?");
        double startMeasure = 123.456;
        double endMeasure = 123.456;

        URI expectedUri = UriComponentsBuilder
                .fromUri(expectedTargetUrl)
                .queryParam("routeId", routeId)
                .queryParam("fromMeasure", startMeasure)
                .queryParam("toMeasure", endMeasure)
                .queryParam("outSR", sr)
                .queryParam("f", f)
                .build(true)
                .toUri();
        HttpHeaders mockHeaders = new HttpHeaders();
        mockHeaders.set("Accept", "application/json");
        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);

        ResponseEntity<GisResponse> mockResponse = ResponseEntity.ok(new GisResponse());
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, GisResponse.class)).thenThrow(
                HttpClientErrorException.BadRequest.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        null,
                        null
                )
        );

        // execute
        List<Milepost> response = uut.getRouteBetweenMeasures(routeId, startMeasure, endMeasure);

        // verify
        Assertions.assertNotNull(response);
        Assertions.assertEquals(0, response.size());
    }
}