package com.trihydro.library.helpers;

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

  private RestTemplateProvider restTemplateProvider;

  @Autowired
  public void InjectDependencies(RestTemplateProvider _restTemplateProvider) {
    this.restTemplateProvider = _restTemplateProvider;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public RestTemplateProvider getRestTemplateProvider() {
    return restTemplateProvider;
  }

  public ResponseEntity<String> getAllRoutes() {
    String targetUrl = baseUrl + "/Routes";
    String params = "?f=json";
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    return restTemplateProvider.GetRestTemplate().exchange(targetUrl + params, HttpMethod.GET, entity, String.class);
  }

  public ResponseEntity<String> getRouteById(String routeId) {
    String targetUrl = baseUrl + "/Route";
    String params = "?routeId=" + routeId + "&outSR=4326&f=json";
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    return restTemplateProvider.GetRestTemplate().exchange(targetUrl + params, HttpMethod.GET, entity, String.class);
  }
}
