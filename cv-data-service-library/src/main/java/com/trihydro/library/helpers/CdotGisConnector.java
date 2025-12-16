package com.trihydro.library.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.trihydro.library.service.RestTemplateProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

@Component
public class CdotGisConnector {
    private final String baseUrl = "https://dtdapps.codot.gov/server/rest/services/LRS/Routes_withDEC/MapServer/exts/LrsServerRounded";
    private final int tolerance = 10000;
    private final int SR = 4326;

    private final RestTemplateProvider restTemplateProvider;

    private final Logger logger = LoggerFactory.getLogger(CdotGisConnector.class);

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
        String params = "?routeId=" + routeId + "&outSR=" + SR + "&f=json";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplateProvider.GetRestTemplate().exchange(targetUrl + params, HttpMethod.GET, entity, String.class);
    }

    /**
     * Retrieves the route details for a point from the CDOT GIS service
     *
     * <p>This method sends a GET request to the CDOT GIS service to retrieve the route details
     * in JSON format. The JSON response includes the measure of the point which is used in
     * getting the mileposts of a route from one point to another</p>
     *
     * @param latitude  latitude of a point
     * @param longitude longitude of a point
     * @return a ResponseEntity containing the JSON response from the CDOT GIS service
     * @throws RestClientException if an error occurs while making the request
     */
    public ResponseEntity<String> getMeasureAtPoint(BigDecimal latitude, BigDecimal longitude) throws RestClientException {
        URI base = URI.create(baseUrl + "/MeasureAtPoint");

        URI targetUrl = UriComponentsBuilder
                .fromUri(base)
                .queryParam("x", longitude.toPlainString())
                .queryParam("y", latitude.toPlainString())
                .queryParam("tolerance", tolerance)
                .queryParam("outSR", SR)
                .queryParam("inSR", SR)
                .queryParam("f", "pjson")
                .build(true)
                .toUri();

        // Log entire request URL
        logger.warn("Requesting URL: {}", targetUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplateProvider.GetRestTemplate().exchange(targetUrl, HttpMethod.GET, entity, String.class);
    }

    /**
     * Retrieves the all the mileposts on a route between the start and end measure
     *
     * <p>Sends a GET request to the CDOT GIS service to retrieve every latitude and longitude point on the specified
     * route that falls between the provided begin and end measures.</p>
     *
     * @param routeId      GIS server route ID
     * @param startMeasure Start measure on route (miles)
     * @param endMeasure   End measure on route (miles)
     * @return a ResponseEntity containing the JSON response from the CDOT GIS service
     * @throws RestClientException if an error occurs while making the request
     */
    public ResponseEntity<String> getRouteBetweenMeasures(String routeId, double startMeasure, double endMeasure) throws RestClientException {

        if (isRouteDescending(startMeasure, endMeasure)) {
            routeId = routeId.replace("_DEC", "") + "_DEC";
        }

        if (isRouteIDDescending(routeId)) {
            if (startMeasure < endMeasure) {
                double newEndMeasure = startMeasure;
                startMeasure = endMeasure;
                endMeasure = newEndMeasure;
            }
        } else {
            if (startMeasure > endMeasure) {
                double newEndMeasure = startMeasure;
                startMeasure = endMeasure;
                endMeasure = newEndMeasure;
            }
        }

        String targetUrl = baseUrl + "/RouteBetweenMeasures?";
        logger.warn("hitting: " + targetUrl);
        logger.info("Getting route route between measures {} and {} from CDOT GIS service at: {}", startMeasure, endMeasure, targetUrl);
        List<String> parameters = new ArrayList<>();
        parameters.add("routeId=" + routeId);
        parameters.add("fromMeasure=" + startMeasure);
        parameters.add("toMeasure=" + endMeasure);
        parameters.add("outSR=" + SR);
        parameters.add("f=pjson");
        String paramString = String.join("&", parameters);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplateProvider.GetRestTemplate().exchange(targetUrl + paramString, HttpMethod.GET, entity, String.class);
    }

    private boolean isRouteDescending(double startMeasure, double endMeasure) {
        return endMeasure < startMeasure;
    }

    private boolean isRouteIDDescending(String routeId) {
        return routeId.toLowerCase().endsWith("_dec");
    }
}
