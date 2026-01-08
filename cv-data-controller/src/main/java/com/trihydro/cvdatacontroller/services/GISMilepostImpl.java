package com.trihydro.cvdatacontroller.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trihydro.cvdatacontroller.model.gisResponse.GisResponse;
import com.trihydro.cvdatacontroller.model.gisResponse.Attributes;
import com.trihydro.cvdatacontroller.helpers.GISConnector;
import com.trihydro.cvdatacontroller.model.gisResponse.GisRoutesResponse;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
            GisRoutesResponse routes = gisConnector.getAllRoutes().getBody();

            if (routes == null || routes.getRoutes() == null) {
                log.error("GIS Service returned null response.");
                return new ArrayList<>();
            }

            return routes.getRoutes().stream().map(Attributes::getRoute).collect(Collectors.toList());
        } catch (RestClientException e) {
            log.error("Failed to get Routes from GIS service.", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Milepost> getMilepostsByStartEndPoint(WydotTim wydotTim) {
        BigDecimal startLat = wydotTim.getStartPoint().getLatitude();
        BigDecimal startLong = wydotTim.getStartPoint().getLongitude();
        BigDecimal endLat = wydotTim.getEndPoint().getLatitude();
        BigDecimal endLong = wydotTim.getEndPoint().getLongitude();
        String routeId = wydotTim.getRoute().replace('-', '_');

        try {
            GisResponse startGisResponseDetails = gisConnector.getMeasureAtPoint(startLong, startLat).getBody();

            if (checkGisMeasureResponse(startGisResponseDetails)) {
                return new ArrayList<>();
            }

            GisResponse endGisResponseDetails = gisConnector.getMeasureAtPoint(endLong, endLat).getBody();

            if (checkGisMeasureResponse(endGisResponseDetails)) {
                return new ArrayList<>();
            }

            String startRoute = startGisResponseDetails.getFeatures().get(0).getAttributes().getRoute();
            double startMeasure = startGisResponseDetails.getFeatures().get(0).getAttributes().getMeasure();
            String endRoute = endGisResponseDetails.getFeatures().get(0).getAttributes().getRoute();
            double endMeasure = endGisResponseDetails.getFeatures().get(0).getAttributes().getMeasure();

            if (!startRoute.equals(endRoute) || !startRoute.equals(routeId)) {
                log.warn("Unable to find route. Generated route does not match.");
                return new ArrayList<>();
            }

            if (startMeasure == endMeasure) {
                endMeasure = getBufferedMeasure(routeId, startGisResponseDetails, 1.0);
            }

            GisResponse response = gisConnector.getRouteBetweenMeasures(startRoute, startMeasure, endMeasure).getBody();

            return getMilepostsFromResponse(response, routeId);

        } catch (RestClientException e) {
            log.error("Failed to get route from GIS service.", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Milepost> getMilepostsByPointWithBuffer(MilepostBuffer milepostBuffer) {
        try {
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
            GisResponse measureDetails = gisConnector.getMeasureAtPoint(milepost.getLongitude(), milepost.getLatitude()).getBody();

            if (checkGisMeasureResponse(measureDetails)) {
                return new ArrayList<>();
            }

            String milepostRoute = measureDetails.getFeatures().get(0).getAttributes().getRoute();
            if (!milepostRoute.equals(milepostBuffer.getCommonName())) {
                log.warn("Unable to find measure on route");
                return new ArrayList<>();
            }

            double milepostMeasure = measureDetails.getFeatures().get(0).getAttributes().getMeasure();
            double bufferMilepost = getBufferedMeasure(milepostRoute, measureDetails, milepostBuffer.getBufferMiles());

            GisResponse response = gisConnector.getRouteBetweenMeasures(milepostRoute, milepostMeasure, bufferMilepost).getBody();
            return getMilepostsFromResponse(response, milepostRoute);
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
        if (checkGisRouteResponse(response)) {
            return new ArrayList<>();
        }

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
