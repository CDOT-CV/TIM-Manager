package com.trihydro.cvdatacontroller.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trihydro.cvdatacontroller.model.Measure.Measure;
import com.trihydro.cvdatacontroller.model.Route.Attributes;
import com.trihydro.cvdatacontroller.model.Route.Route;
import com.trihydro.library.helpers.GISConnector;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
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
@ConditionalOnProperty(name="config.milepostProvider", havingValue="gis")
public class GISMilepostImpl implements MilepostService {
    private GISConnector gisConnector;
    private ObjectMapper objectMapper;

    @Autowired
    public void InjectDependencies(GISConnector _gisConnector, ObjectMapper _objectMapper) {
        this.gisConnector = _gisConnector;
        this.objectMapper = _objectMapper;
    }

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
            log.error("Error parsing JSON response from GIS service: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Milepost> getMilepostsByStartEndPoint(WydotTim wydotTim) throws JsonProcessingException {
        BigDecimal startLat = wydotTim.getStartPoint().getLatitude();
        BigDecimal startLong = wydotTim.getStartPoint().getLongitude();
        BigDecimal endLat = wydotTim.getEndPoint().getLatitude();
        BigDecimal endLong = wydotTim.getEndPoint().getLongitude();
        String routeId = wydotTim.getRoute().replace('-', '_');

        ResponseEntity<String> startMeasureDetailsJson = gisConnector.getMeasureAtPoint(startLong, startLat);
        Measure startMeasureDetails =
                objectMapper.readValue(startMeasureDetailsJson.getBody(), Measure.class);
        String startRoute = startMeasureDetails.getFeatures().get(0).getAttributes().getRoute();
        double startMeasure = startMeasureDetails.getFeatures().get(0).getAttributes().getMeasure();

        // Measure endMeasureDetails = milepostService.getMeasureAtPoint(endLong, endLat);
        ResponseEntity<String> endMeasureDetailsJson = gisConnector.getMeasureAtPoint(endLong, endLat);
        Measure endMeasureDetails =
                objectMapper.readValue(endMeasureDetailsJson.getBody(), Measure.class);
        String endRoute = endMeasureDetails.getFeatures().get(0).getAttributes().getRoute();
        double endMeasure = endMeasureDetails.getFeatures().get(0).getAttributes().getMeasure();

        if (!startRoute.equals(endRoute) || !startRoute.equals(routeId)) {
            log.warn("Unable to find route. Generated route does not match.");
            return new ArrayList<>();
        }

        if (startMeasure == endMeasure) {
            endMeasure = getBufferedMeasure(routeId, startMeasureDetails, 1.0);
        }

        // String routeId = milepostService.getRouteBetweenMeasures(startRoute, startMeasure, endMeasure);
        ResponseEntity<String> response = gisConnector.getRouteBetweenMeasures(startRoute, startMeasure, endMeasure);
        return getMilepostsFromResponse(response, routeId);
    }

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
        Measure measureDetails =
                objectMapper.readValue(measureDetailsJson.getBody(), Measure.class);

        String milepostRoute = measureDetails.getFeatures().get(0).getAttributes().getRoute();
        if (!milepostRoute.equals(milepostBuffer.getCommonName())) {
            log.warn("Unable to find measure on route");
            return new ArrayList<>();
        }

        double milepostMeasure = measureDetails.getFeatures().get(0).getAttributes().getMeasure();
        double bufferMilepost = getBufferedMeasure(milepostRoute, measureDetails, milepostBuffer.getBufferMiles());

        ResponseEntity<String> response = gisConnector.getRouteBetweenMeasures(milepostRoute, milepostMeasure, bufferMilepost);
        return getMilepostsFromResponse(response, milepostRoute);
    }

    private List<Milepost> getMilepostsFromResponse(ResponseEntity<String> response, String routeId) throws JsonProcessingException {
        Route routeDetails =
                objectMapper.readValue(response.getBody(), Route.class);
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

    private double getBufferedMeasure(String route, Measure measureDetails, double bufferMiles) throws RestClientException {
        var attributes = measureDetails.getFeatures().get(0).getAttributes();
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
