package com.trihydro.cvdatacontroller.helpers;

import com.trihydro.cvdatacontroller.model.gisResponse.Attributes;
import com.trihydro.cvdatacontroller.model.gisResponse.GisResponse;
import com.trihydro.cvdatacontroller.model.gisResponse.GisRoutesResponse;

import com.trihydro.library.model.Milepost;
import com.trihydro.library.service.RestTemplateProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GISConnector {
  private static final String BASE_URL = "https://dtdapps.codot.gov/server/rest/services/LRS/Routes_withDEC/MapServer/exts/LrsServerRounded";
  private static final int TOLERANCE = 10000;
  private static final int SPATIAL_REFERENCE = 4326;
  private static final String FORMAT = "json";

  private final RestTemplateProvider restTemplateProvider;

  /**
   * Retrieves the route information from the CDOT GIS service by route ID.
   * @param routeId The route identifier.
   * @return A list of Milepost objects representing the mileposts on the route.
   */
  public List<Milepost> getRouteById(String routeId) {
    URI targetUrl = buildUri("/Route")
      .queryParam("routeId", routeId)
      .queryParam("outSR", SPATIAL_REFERENCE)
      .queryParam("f", FORMAT)
      .build(true)
      .toUri();

    return getMileposts(routeId, targetUrl);
  }


  /**
   * Retrieves all route names from the CDOT GIS service.
   * @return A list of route names as strings.
   */
  public List<String> getAllRoutes() {
    URI targetUrl = buildUri("/Routes/query")
      .queryParam("f", FORMAT)
      .build(true)
      .toUri();

    try {
      ResponseEntity<GisRoutesResponse> responseEntity = executeGet(targetUrl, GisRoutesResponse.class);

      GisRoutesResponse routes = responseEntity.getBody();
      if (routes != null && routes.getRoutes() != null) {
        return routes.getRoutes().stream()
          .map(Attributes::getRoute)
          .collect(Collectors.toList());
      }

      return Collections.emptyList();

    } catch (RestClientException e) {
      log.error("Failed to get routes from GIS service.");
      return Collections.emptyList();
    }
  }

  /**
   * Retrieves the route details for a point from the CDOT GIS service.
   * @param longitude The longitude of the point.
   * @param latitude  The latitude of the point.
   * @return A GisResponse object containing the route details at the specified point, or null if not found.
   */
  public GisResponse getMeasureAtPoint(BigDecimal longitude, BigDecimal latitude) {
    URI targetUrl = buildUri("/MeasureAtPoint")
      .queryParam("x", longitude.toPlainString())
      .queryParam("y", latitude.toPlainString())
      .queryParam("tolerance", TOLERANCE)
      .queryParam("outSR", SPATIAL_REFERENCE)
      .queryParam("inSR", SPATIAL_REFERENCE)
      .queryParam("f", FORMAT)
      .build(true)
      .toUri();

    try {
      ResponseEntity<GisResponse> responseEntity = executeGet(targetUrl, GisResponse.class);

      GisResponse gisResponse = responseEntity.getBody();
      if (isGisMeasureValid(gisResponse)) {
        log.warn("Unable to find measure at point. The API return value may have changed.");
        return null;
      }

      return gisResponse;
    } catch (RestClientException e) {
      log.error("Failed to get measure at point from GIS service.");
      return null;
    }
  }

  /**
   * Retrieves all the mileposts on a route between the start and end measure.
   * @param routeId     The route identifier.
   * @param fromMeasure The starting measure on the route.
   * @param toMeasure   The ending measure on the route.
   * @return A list of Milepost objects representing the mileposts on the route between the specified measures.
   */
  public List<Milepost> getRouteBetweenMeasures(String routeId, double fromMeasure, double toMeasure) {
    log.info("Getting route between measure {} to {} on route {}", fromMeasure, toMeasure, routeId);

    URI targetUrl = buildUri("/RouteBetweenMeasures")
      .queryParam("routeId", routeId)
      .queryParam("fromMeasure", fromMeasure)
      .queryParam("toMeasure", toMeasure)
      .queryParam("outSR", SPATIAL_REFERENCE)
      .queryParam("f", FORMAT)
      .build(true)
      .toUri();

    return getMileposts(routeId, targetUrl);
  }

  private UriComponentsBuilder buildUri(String path) {
    return UriComponentsBuilder.fromUriString(BASE_URL + path);
  }

  private <T> ResponseEntity<T> executeGet(URI uri, Class<T> responseType) {
    var entity = new HttpEntity<>(createDefaultHeaders());
    return restTemplateProvider.GetRestTemplate().exchange(uri, HttpMethod.GET, entity, responseType);
  }

  private HttpHeaders createDefaultHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return headers;
  }


  /**
   * Retrieves mileposts from the GIS service using the provided target URL.
   * @param routeId  The route identifier.
   * @param targetUrl The target URL for the GIS service request.
   * @return A list of Milepost objects representing the mileposts on the route.
   */
  private List<Milepost> getMileposts(String routeId, URI targetUrl) {
    try {
      ResponseEntity<GisResponse> responseEntity = executeGet(targetUrl, GisResponse.class);

      GisResponse route = responseEntity.getBody();
      if (route == null || isGisRouteValid(route)) {
        log.warn("Unable to find route at point. The API return value may have changed.");
        return Collections.emptyList();
      }

      return getMilepostsFromResponse(route, routeId);

    } catch (RestClientException e) {
      log.error("Failed to get route from GIS service.", e);
      return Collections.emptyList();
    }
  }

  /*
    * Validates the GIS response for measure at point requests.
    * @return true if the response is invalid; false otherwise.
   */
  private boolean isGisMeasureValid(GisResponse gisResponse) {
    return gisResponse == null
      || gisResponse.getFeatures() == null
      || gisResponse.getFeatures().isEmpty()
      || gisResponse.getFeatures().get(0) == null
      || gisResponse.getFeatures().get(0).getAttributes() == null
      || gisResponse.getFeatures().get(0).getAttributes().getRoute() == null
      || gisResponse.getFeatures().get(0).getAttributes().getMeasure() == null;
  }

  /*
    * Validates the GIS response for route requests.
    * @return true if the response is invalid; false otherwise.
   */
  private boolean isGisRouteValid(GisResponse gisResponse) {
    return gisResponse == null
      || gisResponse.getFeatures() == null
      || gisResponse.getFeatures().isEmpty()
      || gisResponse.getFeatures().get(0) == null
      || gisResponse.getFeatures().get(0).getGeometry() == null
      || gisResponse.getFeatures().get(0).getGeometry().getPaths() == null
      || gisResponse.getFeatures().get(0).getGeometry().getPaths().get(0) == null;
  }

  private List<Milepost> getMilepostsFromResponse(GisResponse response, String routeId) {
    List<List<Double>> path = response.getFeatures().get(0).getGeometry().getPaths().get(0);
    List<Milepost> mileposts = new ArrayList<>();
    for (List<Double> coordinate : path) {
      Milepost milepost = new Milepost();
      milepost.setCommonName(routeId);
      milepost.setLatitude(coordinate.get(1));
      milepost.setLongitude(coordinate.get(0));
      mileposts.add(milepost);
    }
    return mileposts;
  }
}
