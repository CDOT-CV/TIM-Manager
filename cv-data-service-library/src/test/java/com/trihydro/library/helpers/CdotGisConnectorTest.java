package com.trihydro.library.helpers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.trihydro.library.service.BaseServiceTest;

import java.math.BigDecimal;

class CdotGisConnectorTest extends BaseServiceTest {

  @InjectMocks
  private CdotGisConnector uut;

  private final String expectedBaseUrl = "https://dtdapps.coloradodot.info/arcgis/rest/services/LRS/Routes_withDEC/MapServer/exts/CdotLrsAccessRounded";

  @Test
  void testGetBaseUrl() {
    Assertions.assertEquals(expectedBaseUrl, uut.getBaseUrl());
  }

  @Test
  void testGetRouteById() {
    // prepare
    String expectedTargetUrl = expectedBaseUrl + "/Route";
    String routeId = "025A";
    int outSR = 4326;
    String f = "json";
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
//
//    @Test
//    void testGetRouteDetails() {
//        // prepare
//        String expectedTargetUrl = expectedBaseUrl + "/MeasureAtPoint";
//        BigDecimal latitude = new BigDecimal("123.456");
//        BigDecimal longitude = new BigDecimal("234.567");
//        int tolerance = 10000;
//        int SR = 4326;
//        String f = "json";
//        String expectedParams = "?x=" + latitude + "&y=" + longitude + "&tolerance=" + tolerance + "&outSR=" + SR + "&outSR=" + SR + "&f=" + f;
//        HttpHeaders mockHeaders = new HttpHeaders();
//        mockHeaders.set("Accept", "application/json");
//        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);
//        String mockResponseString = "mockResponseString";
//        ResponseEntity<String> mockResponse = ResponseEntity.ok(mockResponseString);
//        when(mockRestTemplate.exchange(expectedTargetUrl + expectedParams, HttpMethod.GET, mockEntity, String.class)).thenReturn(mockResponse);
//
//        // execute
//        ResponseEntity<String> response = uut.getRouteDetails(latitude, longitude);
//
//        // verify
//        Assertions.assertEquals(mockResponse.getStatusCode(), response.getStatusCode());
//        Assertions.assertEquals(mockResponseString, response.getBody());
//        verify(mockRestTemplate).exchange(expectedTargetUrl + expectedParams, HttpMethod.GET, mockEntity, String.class);
//    }
//
//    @Test
//    void testGetRouteBetweenMeasures() {
//        // prepare
//        String expectedTargetUrl = expectedBaseUrl + "/Route";
//        String routeId = "025A";
//        int outSR = 4326;
//        String f = "json";
//        String expectedParams = "?routeId=" + routeId + "&outSR=" + outSR + "&f=" + f;
//        HttpHeaders mockHeaders = new HttpHeaders();
//        mockHeaders.set("Accept", "application/json");
//        HttpEntity<String> mockEntity = new HttpEntity<>(mockHeaders);
//        String mockResponseString = "mockResponseString";
//        ResponseEntity<String> mockResponse = ResponseEntity.ok(mockResponseString);
//        when(mockRestTemplate.exchange(expectedTargetUrl + expectedParams, HttpMethod.GET, mockEntity, String.class)).thenReturn(mockResponse);
//
//        // execute
//        ResponseEntity<String> response = uut.getRouteById(routeId);
//
//        // verify
//        Assertions.assertEquals(mockResponse.getStatusCode(), response.getStatusCode());
//        Assertions.assertEquals(mockResponseString, response.getBody());
//        verify(mockRestTemplate).exchange(expectedTargetUrl + expectedParams, HttpMethod.GET, mockEntity, String.class);
//    }

}