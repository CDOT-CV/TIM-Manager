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

class CdotGisConnectorTest extends BaseServiceTest {

  @InjectMocks
  private CdotGisConnector uut;

  private final String expectedBaseUrl = "https://dtdapps.coloradodot.info/arcgis/rest/services/LRS/Routes/MapServer/exts/CdotLrsAccessRounded";

  @Test
  void testInitialization() {
    Assertions.assertNotNull(uut);
    Assertions.assertEquals(expectedBaseUrl, uut.getBaseUrl());
    Assertions.assertNotNull(uut.getRestTemplateProvider());
  }

  @Test
  void testGetBaseUrl() {
    Assertions.assertEquals("https://dtdapps.coloradodot.info/arcgis/rest/services/LRS/Routes/MapServer/exts/CdotLrsAccessRounded", uut.getBaseUrl());
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

}