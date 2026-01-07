package com.trihydro.cvdatacontroller.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trihydro.cvdatacontroller.model.gisResponse.GisResponse;
import com.trihydro.cvdatacontroller.model.gisResponse.Attributes;
import com.trihydro.library.helpers.GISConnector;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name="config.milepostProvider", havingValue="gis")
public class GISMilepostImpl implements MilepostService {
    private final GISConnector gisConnector;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> getRoutes() {
        try {
            ResponseEntity<String> routesJson = gisConnector.getAllRoutes();

            List<Attributes> routes = objectMapper
                    .readerFor(new TypeReference<List<Attributes>>() {})
                    .readValue(
                            objectMapper.readTree(routesJson.getBody()).at("/routes")
                    );

            return routes.stream().map(Attributes::getRoute).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to parse JSON response from GIS service: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Milepost> getMilepostsByStartEndPoint(WydotTim wydotTim) throws JsonProcessingException {
        BigDecimal startLat = wydotTim.getStartPoint().getLatitude();
        BigDecimal startLong = wydotTim.getStartPoint().getLongitude();
        BigDecimal endLat = wydotTim.getEndPoint().getLatitude();
        BigDecimal endLong = wydotTim.getEndPoint().getLongitude();
        String routeId = wydotTim.getRoute().replace('-', '_');

        ResponseEntity<String> startMeasureDetailsJson = gisConnector.getMeasureAtPoint(startLong, startLat);
        GisResponse startGisResponseDetails =
                objectMapper.readValue(startMeasureDetailsJson.getBody(), GisResponse.class);

        if (checkGisMeasureResponse(startGisResponseDetails)) {
            return new ArrayList<>();
        }

        String startRoute = startGisResponseDetails.getFeatures().get(0).getAttributes().getRoute();
        double startMeasure = startGisResponseDetails.getFeatures().get(0).getAttributes().getMeasure();

        ResponseEntity<String> endMeasureDetailsJson = gisConnector.getMeasureAtPoint(endLong, endLat);
        GisResponse endGisResponseDetails =
                objectMapper.readValue(endMeasureDetailsJson.getBody(), GisResponse.class);

        if (checkGisMeasureResponse(endGisResponseDetails)) {
            return new ArrayList<>();
        }

        String endRoute = endGisResponseDetails.getFeatures().get(0).getAttributes().getRoute();
        double endMeasure = endGisResponseDetails.getFeatures().get(0).getAttributes().getMeasure();

        if (!startRoute.equals(endRoute) || !startRoute.equals(routeId)) {
            log.warn("Unable to find route. Generated route does not match.");
            return new ArrayList<>();
        }

        if (startMeasure == endMeasure) {
            endMeasure = getBufferedMeasure(routeId, startGisResponseDetails, 1.0);
        }

        ResponseEntity<String> response = gisConnector.getRouteBetweenMeasures(startRoute, startMeasure, endMeasure);
        return getMilepostsFromResponse(response, routeId);
    }

    @Override
    public List<Milepost> getMilepostsByPointWithBuffer(MilepostBuffer milepostBuffer)
            throws JsonProcessingException, RestClientException {

        // check startPoint
        if (milepostBuffer.getPoint() == null || milepostBuffer.getPoint().getLatitude() == null
                || milepostBuffer.getPoint().getLongitude() == null) {
            return new ArrayList<>();
        }

        // check direction, route
        if (milepostBuffer.getDirection() == null || milepostBuffer.getCommonName() == null) {
            return new ArrayList<>();
        }

        var milepost = milepostBuffer.getPoint();
        ResponseEntity<String> measureDetailsJson = gisConnector.getMeasureAtPoint(milepost.getLongitude(), milepost.getLatitude());
        GisResponse gisResponseDetails =
                objectMapper.readValue(measureDetailsJson.getBody(), GisResponse.class);

        if (checkGisMeasureResponse(gisResponseDetails)) {
            return new ArrayList<>();
        }

        String milepostRoute = gisResponseDetails.getFeatures().get(0).getAttributes().getRoute();
        if (!milepostRoute.equals(milepostBuffer.getCommonName())) {
            log.warn("Unable to find measure on route");
            return new ArrayList<>();
        }

        double milepostMeasure = gisResponseDetails.getFeatures().get(0).getAttributes().getMeasure();
        double bufferMilepost = getBufferedMeasure(milepostRoute, gisResponseDetails, milepostBuffer.getBufferMiles());

        ResponseEntity<String> response = gisConnector.getRouteBetweenMeasures(milepostRoute, milepostMeasure, bufferMilepost);
        return getMilepostsFromResponse(response, milepostRoute);
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

    private List<Milepost> getMilepostsFromResponse(ResponseEntity<String> response, String routeId) throws JsonProcessingException {
        GisResponse routeDetails =
                objectMapper.readValue(response.getBody(), GisResponse.class);

        if (checkGisRouteResponse(routeDetails)) {
            return new ArrayList<>();
        }

        List<List<Double>> path = routeDetails.getFeatures().get(0).getGeometry().getPaths().get(0);
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

    private double getBufferedMeasure(String route, GisResponse gisResponseDetails, double bufferMiles) throws RestClientException {
        var attributes = gisResponseDetails.getFeatures().get(0).getAttributes();
        double measure = attributes.getMeasure();
        double bufferMeasure;
        if (route.toLowerCase().endsWith("_dec")) {
            bufferMeasure = measure - bufferMiles;
        } else {
            bufferMeasure = measure + bufferMiles;
        }

        double mMin = attributes.getMMin();
        double mMax = attributes.getMMax();
        if (bufferMeasure < mMin) {
            bufferMeasure = mMin;
        }
        if (bufferMeasure > mMax) {
            bufferMeasure = mMax;
        }
        return bufferMeasure;
    }
}
