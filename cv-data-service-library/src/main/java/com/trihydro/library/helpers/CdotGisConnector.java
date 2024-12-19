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
  private final String baseUrl = "https://dtdapps.coloradodot.info/arcgis/rest/services/LRS/Routes_withDEC/MapServer/exts/CdotLrsAccessRounded";

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

  /**
   * Retrieves the route information from the CDOT GIS service by route ID.
   *
   * <p>This method sends a GET request to the CDOT GIS service to retrieve the route information
   * in JSON format. The JSON response includes every latitude and longitude point associated
   * with the specified route, such as I-25.</p>
   *
   * @param routeId the ID of the route to retrieve
   * @return a ResponseEntity containing the JSON response from the CDOT GIS service
   * @throws RestClientException if an error occurs while making the request
   */
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
