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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GISConnector {
    private final String baseUrl = "https://dtdapps.codot.gov/server/rest/services/LRS/Routes_withDEC/MapServer/exts/LrsServerRounded";
    private final int tolerance = 10000;
    private final int sr = 4326;
    private final String format = "json";

    private final RestTemplateProvider restTemplateProvider;

    /**
     * Retrieves the route information from the CDOT GIS service by route ID.
     *
     * <p>This method sends a GET request to the CDOT GIS service to retrieve the route information
     * in JSON format. The JSON response includes every latitude and longitude point associated
     * with the specified route, such as I-25. This information is then extracted into the `Milepost` model and
     * returned as a list of mileposts.</p>
     *
     * @param routeId the ID of the route to retrieve
     * @return a list of mileposts along the specified route converted from the JSON response of the CDOT GIS service
     */
    public List<Milepost> getRouteById(String routeId) {
        String targetUrl = baseUrl + "/Route";
        String params = "?routeId=" + routeId + "&outSR=" + sr + "&f=" + format;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GisResponse> responseEntity = restTemplateProvider.GetRestTemplate().exchange(targetUrl + params, HttpMethod.GET, entity, GisResponse.class);

            GisResponse route = responseEntity.getBody();
            if (route == null || checkGisRouteResponse(route)) {
                return new ArrayList<>();
            }

            return getMilepostsFromResponse(route, routeId);

        } catch (RestClientException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves all route names from the CDOT GIS service
     *
     * <p>This method sends a GET request to the CDOT GIS service to retrieve the all route names
     * in JSON format. This is converted into a GisResponse.</p>
     *
     * @return a list of Route names converted from the JSON response of the CDOT GIS service
     */
    public List<String> getAllRoutes() {
        URI base = URI.create(
                baseUrl +
                        "/Routes/query"
        );

        URI targetUrl = UriComponentsBuilder
                .fromUri(base)
                .queryParam("f", format)
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GisRoutesResponse> routesResponseEntity = restTemplateProvider.GetRestTemplate().exchange(targetUrl, HttpMethod.GET, entity, GisRoutesResponse.class);
            GisRoutesResponse routes = routesResponseEntity.getBody();
            if (routes != null && routes.getRoutes() != null) {
                return routes.getRoutes().stream().map(Attributes::getRoute).collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (RestClientException e) {
            log.error("Failed to get Routes from GIS service.");
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves the route details for a point from the CDOT GIS service
     *
     * <p>This method sends a GET request to the CDOT GIS service to retrieve the route details
     * in JSON format. This is converted into a GisResponse. The response includes the measure
     * of the point which is used in getting the mileposts of a route from one point to another</p>
     *
     * @param longitude longitude of a point
     * @param latitude  latitude of a point
     * @return a GisResponse converted from the JSON response of the CDOT GIS service or null if an error occurs
     */
    public GisResponse getMeasureAtPoint(BigDecimal longitude, BigDecimal latitude) {
        URI base = URI.create(baseUrl + "/MeasureAtPoint");

        URI targetUrl = UriComponentsBuilder
                .fromUri(base)
                .queryParam("x", longitude.toPlainString())
                .queryParam("y", latitude.toPlainString())
                .queryParam("tolerance", tolerance)
                .queryParam("outSR", sr)
                .queryParam("inSR", sr)
                .queryParam("f", format)
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<GisResponse> responseEntity = restTemplateProvider.GetRestTemplate().exchange(targetUrl, HttpMethod.GET, entity, GisResponse.class);
            GisResponse gisResponseDetails = responseEntity.getBody();
            if (checkGisMeasureResponse(gisResponseDetails)) {
                return null;
            }
            return gisResponseDetails;
        } catch (RestClientException e) {
            log.error("Failed to get measure at point from GIS service.");
            return null;
        }
    }

    /**
     * Retrieves all the mileposts on a route between the start and end measure
     *
     * <p>Sends a GET request to the CDOT GIS service to retrieve every latitude and longitude point on the specified
     * route that falls between the provided start and end measures. This information is then extracted into the
     * `Milepost` model and returned as a list of mileposts.
     * </p>
     *
     * @param routeId      GIS server route ID
     * @param fromMeasure Start measure on route (miles)
     * @param toMeasure   End measure on route (miles)
     * @return a list of mileposts from the CDOT GIS service and null if an error occurs
     */
    public List<Milepost> getRouteBetweenMeasures(String routeId, double fromMeasure, double toMeasure) {
        log.info("Getting route between measure {} to {} on route {}", fromMeasure, toMeasure, routeId);
        URI base = URI.create(baseUrl + "/RouteBetweenMeasures?");
        URI targetUrl = UriComponentsBuilder
                .fromUri(base)
                .queryParam("routeId", routeId)
                .queryParam("fromMeasure", fromMeasure)
                .queryParam("toMeasure", toMeasure)
                .queryParam("outSR", sr)
                .queryParam("f", format)
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GisResponse> responseEntity = restTemplateProvider.GetRestTemplate().exchange(targetUrl, HttpMethod.GET, entity, GisResponse.class);

            GisResponse route = responseEntity.getBody();
            if (route == null || checkGisRouteResponse(route)) {
                return new ArrayList<>();
            }

            return getMilepostsFromResponse(route, routeId);

        } catch (RestClientException e) {
            log.error("Failed to get route from GIS service.", e);
            return new ArrayList<>();
        }
    }

    private boolean checkGisMeasureResponse(GisResponse gisResponseDetails) {
        if (gisResponseDetails == null
                || gisResponseDetails.getFeatures() == null
                || gisResponseDetails.getFeatures().isEmpty()
                || gisResponseDetails.getFeatures().get(0) == null
                || gisResponseDetails.getFeatures().get(0).getAttributes() == null
                || gisResponseDetails.getFeatures().get(0).getAttributes().getRoute() == null
                || gisResponseDetails.getFeatures().get(0).getAttributes().getMeasure() == null) {
            log.warn("Unable to find measure at point. The API return value may have changed.");
            return true;
        }
        return false;
    }

    private boolean checkGisRouteResponse(GisResponse gisResponseDetails) {
        if (gisResponseDetails == null
                || gisResponseDetails.getFeatures() == null
                || gisResponseDetails.getFeatures().isEmpty()
                || gisResponseDetails.getFeatures().get(0) == null
                || gisResponseDetails.getFeatures().get(0).getGeometry() == null
                || gisResponseDetails.getFeatures().get(0).getGeometry().getPaths() == null
                || gisResponseDetails.getFeatures().get(0).getGeometry().getPaths().get(0) == null) {
            log.warn("Unable to find route at point. The API return value may have changed.");
            return true;
        }
        return false;
    }

    private List<Milepost> getMilepostsFromResponse(GisResponse response, String routeId) {
        List<List<Double>> path = response.getFeatures().get(0).getGeometry().getPaths().get(0);
        List<Milepost> mileposts = new ArrayList<>();
        for (List<Double> coordinate : path) {
            Milepost milepost = new Milepost();
            milepost.setCommonName(routeId);
            BigDecimal latitude = new BigDecimal(coordinate.get(1).toString()).setScale(14, RoundingMode.HALF_UP);
            BigDecimal longitude =
                    new BigDecimal(coordinate.get(0).toString()).setScale(14, RoundingMode.HALF_UP);
            milepost.setLatitude(latitude);
            milepost.setLongitude(longitude);
            mileposts.add(milepost);
        }
        return mileposts;
    }
}
