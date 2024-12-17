package com.trihydro.library.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.trihydro.library.service.RestTemplateProvider;
import org.springframework.stereotype.Component;

@Component
public class CdotGisConnector {
  private final String baseUrl = "https://dtdapps.coloradodot.info/arcgis/rest/services/LRS/Routes/MapServer/exts/CdotLrsAccessRounded";

  private final RestTemplateProvider restTemplateProvider;

  private final Logger logger = LoggerFactory.getLogger(CdotGisConnector.class);

  @Autowired
  public CdotGisConnector(RestTemplateProvider _restTemplateProvider) {
    this.restTemplateProvider = _restTemplateProvider;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public RestTemplateProvider getRestTemplateProvider() {
    return restTemplateProvider;
  }

  public ResponseEntity<String> getRouteById(String routeId) {
    String targetUrl = baseUrl + "/Route";
    logger.info("Getting route with ID {} from CDOT GIS service at: {}", routeId, targetUrl);
    String params = "?routeId=" + routeId + "&outSR=4326&f=json";
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response;
    try {
      response = restTemplateProvider.GetRestTemplate().exchange(targetUrl + params, HttpMethod.GET, entity, String.class);
    } catch (Exception e) {
        logger.error("Error getting route with ID {} from CDOT GIS service: {}", routeId, e.getMessage(), e);
        return null;
    }
    if (response.getBody() == null) {
        logger.warn("Received null response body from CDOT GIS service");
    }
    return response;
  }
}
