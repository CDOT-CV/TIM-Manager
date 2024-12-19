package com.trihydro.cvdatacontroller.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.helpers.CdotGisConnector;
import com.trihydro.library.model.Milepost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClientException;

@CrossOrigin
@RestController
@RequestMapping("cdot-upstream-path")
public class CdotUpstreamPathController extends BaseController {
  private final CdotGisConnector cdotGisService;

  private final Logger logger = LoggerFactory.getLogger(CdotUpstreamPathController.class);

  @Autowired
  public CdotUpstreamPathController(CdotGisConnector cdotGisService) {
    this.cdotGisService = cdotGisService;
  }

  /**
   * Retrieves a buffer path of mileposts for a given route and desired distance.
   *
   * This method takes a list of path mileposts, a route ID, and a desired distance in miles.
   * It fetches all mileposts for the specified route and determines the direction of the path.
   * Then, it traverses the mileposts to create a buffer path that meets the desired distance.
   *
   * Note: The current implementation pulls back the entire route linestring in one request.
   * There is a possibility to optimize this by hitting the geo REST service multiple times
   * to fetch smaller segments of the route. However, this method will proceed with the current
   * implementation unless performance issues are observed.
   *
   * @param pathMileposts the list of mileposts defining the path
   * @param routeId the ID of the route
   * @param desiredDistanceInMiles the desired distance for the buffer path in miles
   * @return a ResponseEntity containing the buffer path of mileposts or a bad request status if an error occurs
   * @throws JsonProcessingException if there is an error processing the JSON response from the geo service
   */
  @PostMapping(value = "/get-buffer-for-path/{routeId}/{desiredDistanceInMiles:.+}")
  public ResponseEntity<List<Milepost>> getBufferForPath(@RequestBody List<Milepost> pathMileposts,
                                                         @PathVariable String routeId, @PathVariable
                                                         double desiredDistanceInMiles) throws
      JsonProcessingException {
    logger.info("Getting buffer for path with desired distance: {} miles", desiredDistanceInMiles);
    List<Milepost> allMileposts;
    try {
      allMileposts = getMilepostsForRoute(routeId);
    } catch (RestClientException e) {
      logger.error("Error getting mileposts for route", e);
      return ResponseEntity.badRequest().body(null);
    }
    if (allMileposts == null || allMileposts.isEmpty()) {
      logger.warn("No mileposts found for route");
      return ResponseEntity.badRequest().body(null);
    }
    PathDirection direction;
    try {
      direction = getPathDirection(pathMileposts, allMileposts);
    } catch (NotEnoughMilepostsException e) {
      logger.warn("Not enough mileposts in path", e);
      return ResponseEntity.badRequest().body(null);
    } catch (MilepostNotFoundException e) {
      logger.warn("Milepost not found in route", e);
      return ResponseEntity.badRequest().body(null);
    }
    if (direction == null) {
      logger.warn("Invalid path direction");
      return ResponseEntity.badRequest().body(null);
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
      return ResponseEntity.badRequest().body(null);
    }
    double distanceInMiles = traverseContext.getDistanceInMiles();
    if (distanceInMiles < desiredDistanceInMiles) {
      logger.warn("Buffer path has less distance than desired distance");
      return ResponseEntity.badRequest().body(null);
    }
    logger.info("Distance of buffer path: {} miles", distanceInMiles);
    if (logger.isDebugEnabled()) {
      String geojsonString = convertMilepostsToGeojsonString(buffer);
      logger.debug("Geojson string for buffer: {}", geojsonString);
    }
    return ResponseEntity.ok(buffer);
  }

  public List<Milepost> getMilepostsForRoute(String routeId) throws JsonProcessingException,
      RestClientException {
    ResponseEntity<String> response = cdotGisService.getRouteById(routeId);
    String routeJsonString = response.getBody();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(routeJsonString);
    JsonNode pathNode = rootNode.path("features").get(0).path("geometry").path("paths").get(0);
    List<Milepost> mileposts = new ArrayList<>();
    for (JsonNode node : pathNode) {
      Milepost milepost = new Milepost();
      milepost.setCommonName(routeId);
      BigDecimal latitude = new BigDecimal(node.get(1).asText()).setScale(14, RoundingMode.HALF_UP);
      BigDecimal longitude =
          new BigDecimal(node.get(0).asText()).setScale(14, RoundingMode.HALF_UP);
      milepost.setLatitude(latitude);
      milepost.setLongitude(longitude);
      mileposts.add(milepost);
    }
    return mileposts;
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

  private int getIndexOfMilepost(List<Milepost> mileposts, Milepost milepost) {
    if (milepost.getLatitude() == null || milepost.getLongitude() == null) {
      logger.warn("Milepost has null latitude or longitude");
      return -1;
    }
    BigDecimal latitude = milepost.getLatitude().setScale(14, RoundingMode.HALF_UP);
    BigDecimal longitude = milepost.getLongitude().setScale(14, RoundingMode.HALF_UP);
    for (int i = 0; i < mileposts.size(); i++) {
      Milepost currentMilepost = mileposts.get(i);
      BigDecimal currentLatitude = currentMilepost.getLatitude().setScale(14, RoundingMode.HALF_UP);
      BigDecimal currentLongitude = currentMilepost.getLongitude().setScale(14,
          RoundingMode.HALF_UP);
      if (latitude.equals(currentLatitude) && longitude.equals(currentLongitude)) {
        return i;
      }
    }
    return -1;
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
   * Context class for traversing mileposts to get buffer path
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
   * Interface for traverse strategy to get buffer path
   */
  public interface TraverseStrategy {
    void traverse(TraverseContext context);
  }

  /**
   * Traverse strategy to get buffer path by traversing the mileposts in ascending
   * or descending direction from a starting milepost.
   */
  public abstract static class AbstractTraverseStrategy implements TraverseStrategy {

    protected abstract int getNextIndex(int currentIndex);

    /**
     * Traverses the mileposts to create a buffer path.
     *
     * This method iterates through the mileposts starting from the given start index
     * and adds them to the buffer until the desired distance in miles is reached.
     *
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
   * Traverse strategy to get buffer path by traversing the mileposts in ascending
   * direction from a starting milepost.
   */
  public static class AscendingTraverseStrategy extends AbstractTraverseStrategy {

    @Override
    protected int getNextIndex(int currentIndex) {
      return currentIndex + 1;
    }
  }

  /**
   * Traverse strategy to get buffer path by traversing the mileposts in descending
   * direction from a starting milepost.
   */
  public static class DescendingTraverseStrategy extends AbstractTraverseStrategy {

    @Override
    protected int getNextIndex(int currentIndex) {
      return currentIndex - 1;
    }
  }

  /**
   * Helper class to calculate distance between two points and total distance of a buffer path
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