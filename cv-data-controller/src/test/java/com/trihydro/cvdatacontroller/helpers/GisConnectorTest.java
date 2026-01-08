package com.trihydro.cvdatacontroller.helpers;

import com.trihydro.library.service.RestTemplateProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

import static org.mockito.Mockito.*;

class GisConnectorTest {
    private final String baseUrl = "https://dtdapps.codot.gov/server/rest/services/LRS/Routes_withDEC/MapServer/exts/LrsServerRounded";

    private final String f = "json";
    private final int sr = 4326;
    private final String routeId = "025A";

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

  @Test
  void testGetRouteById() {
    // prepare
    String expectedTargetUrl = baseUrl + "/Route";
    int outSR = 4326;
    String expectedParams = "?routeId=" + routeId + "&outSR=" + outSR + "&f=" + f;
    HttpHeaders mockHeaders = new HttpHeaders();
    mockHeaders.set("Accept", "application/json");
    HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);
    String mockResponseString = "mockResponseString";
    ResponseEntity<String> mockResponse = ResponseEntity.ok(mockResponseString);
    when(mockRestTemplate.exchange(expectedTargetUrl + expectedParams, HttpMethod.GET, mockEntity, String.class)).thenReturn(mockResponse);

    // execute
    ResponseEntity<String> response = uut.getRouteById(routeId);

    // verify
    Assertions.assertEquals(mockResponse.getStatusCode(), response.getStatusCode());
    Assertions.assertEquals(mockResponseString, response.getBody());
    verify(mockRestTemplate).exchange(expectedTargetUrl + expectedParams, HttpMethod.GET, mockEntity, String.class);
  }

    @Test
    void testGetRouteDetails() {
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
        String mockResponseString = "mockResponseString";
        ResponseEntity<String> mockResponse = ResponseEntity.ok(mockResponseString);
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, String.class)).thenReturn(mockResponse);

        // execute
        ResponseEntity<String> response = uut.getMeasureAtPoint(longitude, latitude);

        // verify
        Assertions.assertEquals(mockResponse.getStatusCode(), response.getStatusCode());
        Assertions.assertEquals(mockResponseString, response.getBody());
        verify(mockRestTemplate).exchange(expectedUri, HttpMethod.GET, mockEntity, String.class);
    }

    @Test
    void testGetRouteBetweenMeasures() {
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
        String mockResponseString = "mockResponseString";
        ResponseEntity<String> mockResponse = ResponseEntity.ok(mockResponseString);
        when(mockRestTemplate.exchange(expectedUri, HttpMethod.GET, mockEntity, String.class)).thenReturn(mockResponse);

        // execute
        ResponseEntity<String> response = uut.getRouteBetweenMeasures(routeId, startMeasure, endMeasure);

        // verify
        Assertions.assertEquals(mockResponse.getStatusCode(), response.getStatusCode());
        Assertions.assertEquals(mockResponseString, response.getBody());
        verify(mockRestTemplate).exchange(expectedUri, HttpMethod.GET, mockEntity, String.class);
    }
}