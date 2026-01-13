package com.trihydro.cvdatacontroller.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trihydro.cvdatacontroller.model.gisResponse.GisResponse;
import com.trihydro.cvdatacontroller.helpers.GISConnector;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name="config.milepostProvider", havingValue="gis")
public class GISMilepostImpl implements MilepostService {
    private final GISConnector gisConnector;

    @Override
    public List<String> getRoutes() {
        return gisConnector.getAllRoutes();
    }

    @Override
    public List<Milepost> getMilepostsByStartEndPoint(WydotTim wydotTim) {
        BigDecimal startLat = wydotTim.getStartPoint().getLatitude();
        BigDecimal startLong = wydotTim.getStartPoint().getLongitude();
        BigDecimal endLat = wydotTim.getEndPoint().getLatitude();
        BigDecimal endLong = wydotTim.getEndPoint().getLongitude();
        String routeId = wydotTim.getRoute().replace('-', '_');

        GisResponse startGisResponseDetails = gisConnector.getMeasureAtPoint(startLong, startLat);

        if (startGisResponseDetails == null) {
            return new ArrayList<>();
        }

        GisResponse endGisResponseDetails = gisConnector.getMeasureAtPoint(endLong, endLat);

        if (endGisResponseDetails == null) {
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

        return gisConnector.getRouteBetweenMeasures(startRoute, startMeasure, endMeasure);
    }

    @Override
    public List<Milepost> getMilepostsByPointWithBuffer(MilepostBuffer milepostBuffer) {
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
        GisResponse measureDetails = gisConnector.getMeasureAtPoint(milepost.getLongitude(), milepost.getLatitude());

        if (measureDetails == null) {
            return new ArrayList<>();
        }

        String milepostRoute = measureDetails.getFeatures().get(0).getAttributes().getRoute();
        if (!milepostRoute.equals(milepostBuffer.getCommonName())) {
            log.warn("Unable to find measure on route");
            return new ArrayList<>();
        }

        double milepostMeasure = measureDetails.getFeatures().get(0).getAttributes().getMeasure();
        double bufferMilepost = getBufferedMeasure(milepostRoute, measureDetails, milepostBuffer.getBufferMiles());

        return gisConnector.getRouteBetweenMeasures(milepostRoute, milepostMeasure, bufferMilepost);
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
