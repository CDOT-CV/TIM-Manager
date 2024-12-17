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
import org.springframework.web.client.RestClientException;

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

  public ResponseEntity<String> getRouteById(String routeId) throws RestClientException {
    String targetUrl = baseUrl + "/Route";
    logger.info("Getting route with ID {} from CDOT GIS service at: {}", routeId, targetUrl);
    String params = "?routeId=" + routeId + "&outSR=4326&f=json";
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    return restTemplateProvider.GetRestTemplate().exchange(targetUrl + params, HttpMethod.GET, entity, String.class);
  }
}
