package com.trihydro.cvdatacontroller.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.trihydro.cvdatacontroller.model.Measure.Measure;
import com.trihydro.cvdatacontroller.model.Route.Route;
import com.trihydro.library.helpers.CdotGisConnector;
import com.trihydro.library.model.Milepost;

import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClientException;

@CrossOrigin
@RestController
@RequestMapping("cdot-upstream-path")
public class CdotUpstreamPathController extends BaseController {
    private final CdotGisConnector cdotGisService;
    private final ObjectMapper objectMapper;

    private final Logger logger = LoggerFactory.getLogger(CdotUpstreamPathController.class);

    public CdotUpstreamPathController(CdotGisConnector cdotGisService, ObjectMapper objectMapper) {
        this.cdotGisService = cdotGisService;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves a buffer path of mileposts for a given route and desired distance.
     * <p>
     * This method takes a list of path mileposts, a route ID, and a desired distance in miles.
     * It fetches all mileposts for the specified route and determines the direction of the path.
     * Then, it traverses the mileposts to create a buffer path that meets the desired distance.
     * <p>
     * Note: The current implementation pulls back the entire route linestring in one request.
     * There is a possibility to optimize this by hitting the geo REST service multiple times
     * to fetch smaller segments of the route. However, this method will proceed with the current
     * implementation unless performance issues are observed.
     *
     * @param pathMileposts          the list of mileposts defining the path
     * @param routeId                the ID of the route
     * @param desiredDistanceInMiles the desired distance for the buffer path in miles
     * @return a ResponseEntity containing the buffer path of mileposts or a bad request status if an error occurs
     * @throws JsonProcessingException if there is an error processing the JSON response from the geo service
     */
    @PostMapping(value = "/get-buffer-for-path/{routeId}/{desiredDistanceInMiles:.+}")
    public ResponseEntity<List<Milepost>> getBufferForPath(@RequestBody List<Milepost> pathMileposts,
                                                           @PathVariable String routeId, @PathVariable
                                                           double desiredDistanceInMiles) throws
            JsonProcessingException {
        try {
            List<Milepost> allMileposts = getMilepostsForRoute(routeId);
            if (allMileposts == null || allMileposts.isEmpty()) {
                logger.warn("No mileposts found for route");
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }

            PathDirection direction = getPathDirection(pathMileposts, allMileposts);
            if (direction == null) {
                logger.warn("Invalid path direction");
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }

            Milepost firstMilepostInPath = pathMileposts.get(0);
            int startIndex = getIndexOfMilepost(allMileposts, firstMilepostInPath);
            TraverseContext traverseContext =
                    new TraverseContext(allMileposts, startIndex, desiredDistanceInMiles, direction);

            if (direction == PathDirection.ASCENDING) {
                traverseContext.setTraverseStrategy(new DescendingTraverseStrategy());
            } else {
                traverseContext.setTraverseStrategy(new AscendingTraverseStrategy());
            }
            traverseContext.performTraversal();

            List<Milepost> buffer = traverseContext.getBuffer();
            if (buffer.size() < 2) {
                // at least 2 mileposts are needed to create a valid buffer path
                logger.warn("Buffer path has less than 2 mileposts");
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }
            double distanceInMiles = traverseContext.getDistanceInMiles();
            if (distanceInMiles < desiredDistanceInMiles) {
                logger.warn("Buffer path has less distance than desired distance");
                return ResponseEntity.badRequest().body(new ArrayList<>());
            }

            if (logger.isDebugEnabled()) {
                String geojsonString = convertMilepostsToGeojsonString(buffer);
                logger.debug("Geojson string for buffer: {}", geojsonString);
            }

            return ResponseEntity.ok(buffer);
        } catch (NotEnoughMilepostsException e) {
            logger.warn("Not enough mileposts in path", e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        } catch (MilepostNotFoundException e) {
            logger.warn("Milepost not found in route", e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        } catch (RestClientException e) {
            logger.error("Error getting mileposts for route", e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    /**
     * Retrieves all mileposts for a given route from the CDOT GIS service.
     *
     * <p>This method uses `CdotGisService.getRouteById()` to retrieve the route information
     * in JSON format. The JSON response contains all the latitude and longitude points
     * in the route. This information is then extracted into the `Milepost` model and
     * returned as a list of mileposts.</p>
     *
     * @param routeId the ID of the route to retrieve mileposts for
     * @return a list of `Milepost` objects representing the mileposts in the route
     * @throws JsonProcessingException if there is an error processing the JSON response
     * @throws RestClientException     if an error occurs while making the request
     */
    public List<Milepost> getMilepostsForRoute(String routeId) throws JsonProcessingException,
            RestClientException {
        ResponseEntity<String> response = cdotGisService.getRouteById(routeId);
        return getMilepostsFromResponse(response, routeId);
    }

    /**
     * Determines if the route is supported.
     *
     * <p>This method uses `CdotGisService.getRouteById()` to retrieve the route information
     * in JSON format. If the JSON format does not contain the route, it is not supported.</p>
     *
     * @param routeId the ID of the route to retrieve mileposts for
     * @return boolean indicating whether the route is supported or not
     * @throws JsonProcessingException if there is an error processing the JSON response
     * @throws RestClientException     if an error occurs while making the request
     */
    @RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/get-route-supported/{routeId}")
    public ResponseEntity<Boolean> isRouteSupported(@PathVariable String routeId) throws JsonProcessingException,
            RestClientException {

        ResponseEntity<String> response = cdotGisService.getRouteById(routeId);
        Route routeDetails =
                objectMapper.readValue(response.getBody(), Route.class);

        // If a routeDetails has no routes, the route does not exist
        return ResponseEntity.ok(routeDetails.hasRoutes());
    }

    /**
     * Retrieves all mileposts between the start and end points
     *
     * <p>This method uses `CdotGisService.RouteBetweenMeasures()` to retrieve the route between the start and end
     * in JSON format. The JSON response contains all the latitude and longitude points in the path. This information
     * is then extracted into the `Milepost` model and returned as a list of mileposts.</p>
     *
     * @param wydotTim The WydotTim object containing the data for the TIM.
     * @return a list of `Milepost` objects representing the mileposts from the start to the end point of a Tim
     * @throws JsonProcessingException if there is an error processing the JSON response
     * @throws RestClientException     if an error occurs while making the request
     */
    @RequestMapping(method = RequestMethod.POST, produces = "application/json", value = "/get-milepost-start-end")
    public ResponseEntity<List<Milepost>> getMilepostsByStartEndPoint(@RequestBody WydotTim wydotTim) throws JsonProcessingException,
            RestClientException {
        BigDecimal startLat = wydotTim.getStartPoint().getLatitude();
        BigDecimal startLong = wydotTim.getStartPoint().getLongitude();
        BigDecimal endLat = wydotTim.getEndPoint().getLatitude();
        BigDecimal endLong = wydotTim.getEndPoint().getLongitude();
        String routeId = wydotTim.getRoute().replace('-', '_');

        ResponseEntity<String> startMeasureDetailsJson = cdotGisService.getMeasureAtPoint(startLong, startLat);
        Measure startMeasureDetails =
                objectMapper.readValue(startMeasureDetailsJson.getBody(), Measure.class);
        String startRoute = startMeasureDetails.getFeatures().get(0).getAttributes().getRoute();
        double startMeasure = startMeasureDetails.getFeatures().get(0).getAttributes().getMeasure();

        ResponseEntity<String> endMeasureDetailsJson = cdotGisService.getMeasureAtPoint(endLong, endLat);
        Measure endMeasureDetails =
                objectMapper.readValue(endMeasureDetailsJson.getBody(), Measure.class);
        String endRoute = endMeasureDetails.getFeatures().get(0).getAttributes().getRoute();
        double endMeasure = endMeasureDetails.getFeatures().get(0).getAttributes().getMeasure();

        if (!startRoute.equals(endRoute) || !startRoute.equals(routeId)) {
            logger.warn("Unable to find route. Generated route does not match.");
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }

        if (startMeasure == endMeasure) {
            endMeasure = getBufferedMeasure(routeId, startMeasureDetails, 1.0);
        }

        ResponseEntity<String> response = cdotGisService.getRouteBetweenMeasures(startRoute, startMeasure, endMeasure);
        return ResponseEntity.ok(getMilepostsFromResponse(response, routeId));
    }

    /**
     * Retrieves all mileposts between the buffer point and a buffer
     *
     * <p>This method uses `CdotGisService.GetRouteDetails()` to retrieve the route for the buffer point
     * in JSON format. This is then used to find the endpoint that is the buffered amount away from the buffer point.
     * It then uses the 'CdotGisService.RouteBetweenMeasures()' to get a route between the two measures. The JSON
     * response contains all the latitude and longitude points in the path. This information is then extracted into the
     * `Milepost` model and returned as a list of mileposts.</p>
     *
     * @param milepostBuffer The milepost buffer containing the data for the buffer and the starting point
     * @return a list of `Milepost` objects representing the mileposts from the start to the end point of a buffer
     * @throws JsonProcessingException if there is an error processing the JSON response
     * @throws RestClientException     if an error occurs while making the request
     */
    @RequestMapping(method = RequestMethod.POST, produces = "application/json", value = "/get-milepost-single-point")
    public ResponseEntity<List<Milepost>> getMilepostsByPointWithBuffer(
            @RequestBody MilepostBuffer milepostBuffer) throws JsonProcessingException, RestClientException {

        // check startPoint
        if (milepostBuffer.getPoint() == null || milepostBuffer.getPoint().getLatitude() == null
                || milepostBuffer.getPoint().getLongitude() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
        }

        // check direction, route
        if (milepostBuffer.getDirection() == null || milepostBuffer.getCommonName() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
        }

        var milepost = milepostBuffer.getPoint();
        ResponseEntity<String> measureDetailsJson = cdotGisService.getMeasureAtPoint(milepost.getLongitude(), milepost.getLatitude());
        Measure measureDetails =
                objectMapper.readValue(measureDetailsJson.getBody(), Measure.class);

        String milepostRoute = measureDetails.getFeatures().get(0).getAttributes().getRoute();
        if (!milepostRoute.equals(milepostBuffer.getCommonName())) {
            logger.warn("Unable to find measure on route");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
        }

        double milepostMeasure = measureDetails.getFeatures().get(0).getAttributes().getMeasure();
        double bufferMilepost = getBufferedMeasure(milepostRoute, measureDetails, milepostBuffer.getBufferMiles());

        ResponseEntity<String> response = cdotGisService.getRouteBetweenMeasures(milepostRoute, milepostMeasure, bufferMilepost);
        return ResponseEntity.ok(getMilepostsFromResponse(response, milepostRoute));
    }

    public PathDirection getPathDirection(List<Milepost> pathMileposts, List<Milepost> allMileposts)
            throws NotEnoughMilepostsException, MilepostNotFoundException {
        if (pathMileposts.size() < 2) {
            throw new NotEnoughMilepostsException("Path has less than 2 mileposts");
        }
        Milepost firstMilepostInPath = pathMileposts.get(0);
        Milepost secondMilepostInPath = pathMileposts.get(1);
        int firstMilepostInPathIndex = getIndexOfMilepost(allMileposts, firstMilepostInPath);
        if (firstMilepostInPathIndex == -1) {
            throw new MilepostNotFoundException("First milepost not found in route");
        }
        int secondMilepostInPathIndex = getIndexOfMilepost(allMileposts, secondMilepostInPath);
        if (secondMilepostInPathIndex == -1) {
            throw new MilepostNotFoundException("Second milepost not found in route");
        }
        if (firstMilepostInPathIndex < secondMilepostInPathIndex) {
            return PathDirection.ASCENDING;
        } else {
            return PathDirection.DESCENDING;
        }
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

    private int getIndexOfMilepost(List<Milepost> mileposts, Milepost milepost) {
        if (milepost.getLatitude() == null || milepost.getLongitude() == null) {
            logger.warn("Milepost has null latitude or longitude");
            return -1;
        }
        BigDecimal latitude = milepost.getLatitude().setScale(14, RoundingMode.HALF_UP);
        BigDecimal longitude = milepost.getLongitude().setScale(14, RoundingMode.HALF_UP);
        // Roughly a 1-mile starting buffer
        BigDecimal latDifference = new BigDecimal(0.015);
        BigDecimal lonDifference = new BigDecimal(0.015);
        int closestIndex = -1;
        for (int i = 0; i < mileposts.size(); i++) {
            Milepost currentMilepost = mileposts.get(i);
            BigDecimal currentLatitude = currentMilepost.getLatitude().setScale(14, RoundingMode.HALF_UP);
            BigDecimal currentLongitude = currentMilepost.getLongitude().setScale(14,
                    RoundingMode.HALF_UP);

            // If the current milepost is closer, update the closest index
            if (latDifference.compareTo(latitude.subtract(currentLatitude).abs()) > 0
                    && lonDifference.compareTo(longitude.subtract(currentLongitude).abs()) > 0) {
                latDifference = latitude.subtract(currentLatitude).abs();
                lonDifference = longitude.subtract(currentLongitude).abs();
                closestIndex = i;
            }
        }
        return closestIndex;
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

    private String convertMilepostsToGeojsonString(List<Milepost> mileposts) {
        StringBuilder geojsonStringBuilder = new StringBuilder();
        geojsonStringBuilder.append(
                "{ \"type\": \"FeatureCollection\", \"features\": [{ \"type\": \"Feature\", \"geometry\": { \"type\": \"LineString\", \"coordinates\": [");
        for (int i = 0; i < mileposts.size(); i++) {
            Milepost milepost = mileposts.get(i);
            geojsonStringBuilder.append("[");
            geojsonStringBuilder.append(milepost.getLongitude().toString());
            geojsonStringBuilder.append(", ");
            geojsonStringBuilder.append(milepost.getLatitude().toString());
            geojsonStringBuilder.append("]");
            if (i < mileposts.size() - 1) {
                geojsonStringBuilder.append(", ");
            }
        }
        geojsonStringBuilder.append("] }, \"properties\": { \"commonName\": \"");
        geojsonStringBuilder.append(mileposts.get(0).getCommonName());
        geojsonStringBuilder.append("\" } }] }");
        return geojsonStringBuilder.toString();
    }

    // define path direction enum (ASCENDING, DESCENDING)
    public enum PathDirection {
        ASCENDING,
        DESCENDING
    }

    public static class NotEnoughMilepostsException extends Exception {
        public NotEnoughMilepostsException(String message) {
            super(message);
        }
    }

    public static class MilepostNotFoundException extends Exception {
        public MilepostNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Context class for traversing mileposts to get a buffered path
     */
    public static class TraverseContext {
        private final List<Milepost> allMileposts;
        private final int startIndex;
        private final double desiredDistanceInMiles;
        private final PathDirection direction;

        private TraverseStrategy traverseStrategy;

        private List<Milepost> buffer;
        private double distanceInMiles;

        public TraverseContext(List<Milepost> allMileposts, int startIndex,
                               double desiredDistanceInMiles, PathDirection direction) {
            this.allMileposts = allMileposts;
            this.startIndex = startIndex;
            this.desiredDistanceInMiles = desiredDistanceInMiles;
            this.direction = direction;
            this.buffer = new ArrayList<>();
        }

        public void performTraversal() {
            traverseStrategy.traverse(this);
        }

        public List<Milepost> getAllMileposts() {
            return allMileposts;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public double getDesiredDistanceInMiles() {
            return desiredDistanceInMiles;
        }

        public PathDirection getDirection() {
            return direction;
        }

        public void setTraverseStrategy(TraverseStrategy traverseStrategy) {
            this.traverseStrategy = traverseStrategy;
        }

        public List<Milepost> getBuffer() {
            return buffer;
        }

        public void setBuffer(List<Milepost> buffer) {
            this.buffer = buffer;
        }

        public double getDistanceInMiles() {
            return distanceInMiles;
        }

        public void setDistanceInMiles(double distanceInMiles) {
            this.distanceInMiles = distanceInMiles;
        }
    }

    /**
     * Interface for traverse strategy to get a buffered path
     */
    public interface TraverseStrategy {
        void traverse(TraverseContext context);
    }

    /**
     * Traverse strategy to get a buffered path by traversing the mileposts in ascending
     * or descending direction from a starting milepost.
     */
    public abstract static class AbstractTraverseStrategy implements TraverseStrategy {

        protected abstract int getNextIndex(int currentIndex);

        /**
         * Traverses the mileposts to create a buffer path.
         * <p>
         * This method iterates through the mileposts starting from the given start index
         * and adds them to the buffer until the desired distance in miles is reached.
         * <p>
         * Note: The current implementation recalculates the total distance of the buffer
         * after adding each milepost, which can be inefficient. If performance issues are
         * observed, consider optimizing this calculation.
         *
         * @param context the context containing the mileposts, start index, desired distance, and direction
         */
        @Override
        public void traverse(TraverseContext context) {
            List<Milepost> buffer = new ArrayList<>();
            List<Milepost> allMileposts = context.getAllMileposts();
            int startIndex = context.getStartIndex();
            double desiredDistanceInMiles = context.getDesiredDistanceInMiles();
            double distanceInMiles = 0;

            buffer.add(allMileposts.get(startIndex));
            for (int i = getNextIndex(startIndex); i >= 0 && i < allMileposts.size(); i = getNextIndex(i)) {
                distanceInMiles = DistanceCalculator.calculateDistanceInMiles(buffer);
                if (distanceInMiles >= desiredDistanceInMiles) {
                    break;
                }
                buffer.add(allMileposts.get(i));
            }
            context.setBuffer(buffer);
            context.setDistanceInMiles(distanceInMiles);
        }
    }

    /**
     * Traverse strategy to get a buffered path by traversing the mileposts in an ascending
     * direction from a starting milepost.
     */
    public static class AscendingTraverseStrategy extends AbstractTraverseStrategy {

        @Override
        protected int getNextIndex(int currentIndex) {
            return currentIndex + 1;
        }
    }

    /**
     * Traverse strategy to get a buffered path by traversing the mileposts in a descending
     * direction from a starting milepost.
     */
    public static class DescendingTraverseStrategy extends AbstractTraverseStrategy {

        @Override
        protected int getNextIndex(int currentIndex) {
            return currentIndex - 1;
        }
    }

    /**
     * Helper class to calculate the distance between two points and total distance of a buffer path
     */
    public static class DistanceCalculator {
        final static int R = 6371000; // Radius of the earth in meters

        public static double calculateDistanceInMiles(List<Milepost> buffer) {
            double distanceInMiles = 0;
            for (int i = 0; i < buffer.size() - 1; i++) {
                Milepost mp1 = buffer.get(i);
                Milepost mp2 = buffer.get(i + 1);
                double distanceInMeters = mp1.angularDistanceTo(mp2) * R;
                distanceInMiles += distanceInMeters / 1609.34;
            }
            return distanceInMiles;
        }
    }
}