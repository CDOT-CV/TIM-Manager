package com.trihydro.cvdatacontroller.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.helpers.CdotGisConnector;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.Milepost;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import springfox.documentation.annotations.ApiIgnore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@CrossOrigin
@RestController
@ApiIgnore
@RequestMapping("cdot-upstream-path")
public class CdotUpstreamPathController extends BaseController {
  private CdotGisConnector cdotGisService;
  private Utility utility;

  @Autowired
  public void InjectDependencies(CdotGisConnector cdotGisService, Utility utility) {
    this.cdotGisService = cdotGisService;
    this.utility = utility;
  }

  @RequestMapping(value = "/get-buffer-for-path/{routeId}/{desiredDistanceInMiles:.+}", method = RequestMethod.POST)
  public ResponseEntity<List<Milepost>> getBufferForPath(@RequestBody List<Milepost> pathMileposts, @PathVariable String routeId, @PathVariable double desiredDistanceInMiles) throws JsonMappingException, JsonProcessingException {
    utility.logWithDate("Number of mileposts in path in request body: " + pathMileposts.size(), this.getClass());
    for (Milepost milepost : pathMileposts) {
      utility.logWithDate("Milepost in path: " + milepost.getLatitude() + ", " + milepost.getLongitude(), this.getClass());
    }
    List<Milepost> allMileposts = getMilepostsForRoute(routeId);
    if (allMileposts == null || allMileposts.isEmpty()) {
      utility.logWithDate("No mileposts found for route", this.getClass());
      return ResponseEntity.badRequest().body(null);
    }
    utility.logWithDate("Number of mileposts in route: " + allMileposts.size(), this.getClass());
    PathDirection direction = getPathDirection(pathMileposts, allMileposts);
    if (direction == null) {
      utility.logWithDate("Invalid path direction", this.getClass());
      return ResponseEntity.badRequest().body(null);
    }
    Milepost firstMilepostInPath = pathMileposts.get(0);
    int startIndex = getIndexOfMilepost(allMileposts, firstMilepostInPath);

    List<Milepost> buffer = new ArrayList<Milepost>();
    double distanceInMiles = 0;
    if (direction == PathDirection.ASCENDING) {
      utility.logWithDate("Path direction is ascending. Start index: " + startIndex, this.getClass());
      buffer.add(allMileposts.get(startIndex));
      // add mileposts in descending order
      for (int i = startIndex - 1; i >= 0; i--) {
        distanceInMiles = DistanceCalculator.calculateDistanceInMiles(buffer);
        if (distanceInMiles >= desiredDistanceInMiles) {
          break;
        }
        buffer.add(allMileposts.get(i));
      }
    } else {
      utility.logWithDate("Path direction is descending. Start index: " + startIndex, this.getClass());
      // add mileposts in ascending order
      for (int i = startIndex + 1; i < allMileposts.size(); i++) {
        distanceInMiles = DistanceCalculator.calculateDistanceInMiles(buffer);
        if (distanceInMiles >= desiredDistanceInMiles) {
          break;
        }
        buffer.add(allMileposts.get(i));
      }
    }
    if (buffer.size() < 2) {
      // at least 2 mileposts are needed to create a valid buffer path
      utility.logWithDate("Buffer path has less than 2 mileposts", this.getClass());
      return ResponseEntity.badRequest().body(null);
    }
    if (distanceInMiles < desiredDistanceInMiles) {
      utility.logWithDate("Buffer path has less distance than desired distance", this.getClass());
      return ResponseEntity.badRequest().body(null);
    }
    utility.logWithDate("Distance of buffer path: " + distanceInMiles + " miles", this.getClass());
    String geojsonString = convertMilepostsToGeojsonString(buffer);
    utility.logWithDate("Geojson string for buffer: " + geojsonString, this.getClass());
    return ResponseEntity.ok(buffer);
  }

  @SuppressWarnings("deprecation")
  public List<Milepost> getMilepostsForRoute(String routeId) throws JsonMappingException, JsonProcessingException {
    ResponseEntity<String> response = cdotGisService.getRouteById(routeId);
    String routeJsonString = response.getBody();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(routeJsonString);
    JsonNode pathNode = rootNode.path("features").get(0).path("geometry").path("paths").get(0);
    List<Milepost> mileposts = new ArrayList<Milepost>();
    for (JsonNode node : pathNode) {
      Milepost milepost = new Milepost();
      milepost.setCommonName(routeId);
      BigDecimal latitude = new BigDecimal(node.get(1).asText()).setScale(14, BigDecimal.ROUND_HALF_UP);
      BigDecimal longitude = new BigDecimal(node.get(0).asText()).setScale(14, BigDecimal.ROUND_HALF_UP);
      milepost.setLatitude(latitude);
      milepost.setLongitude(longitude);
      mileposts.add(milepost);
    }
    return mileposts;
  }

  public PathDirection getPathDirection(List<Milepost> pathMileposts, List<Milepost> allMileposts) {
    if (pathMileposts.size() < 2) {
      utility.logWithDate("Path has less than 2 mileposts", this.getClass());
      return null;
    }
    Milepost firstMilepostInPath = pathMileposts.get(0);
    Milepost secondMilepostInPath = pathMileposts.get(1);
    int firstMilepostInPathIndex = getIndexOfMilepost(allMileposts, firstMilepostInPath);
    if (firstMilepostInPathIndex == -1) {
      utility.logWithDate("First milepost not found in route", this.getClass());
      return null;
    }
    int secondMilepostInPathIndex = getIndexOfMilepost(allMileposts, secondMilepostInPath);
    if (secondMilepostInPathIndex == -1) {
      utility.logWithDate("Second milepost not found in route", this.getClass());
      return null;
    }
    if (firstMilepostInPathIndex < secondMilepostInPathIndex) {
      return PathDirection.ASCENDING;
    } else {
      return PathDirection.DESCENDING;
    }
  }

  private int getIndexOfMilepost(List<Milepost> mileposts, Milepost milepost) {
    if (milepost.getLatitude() == null || milepost.getLongitude() == null) {
      utility.logWithDate("Milepost has null latitude or longitude", this.getClass());
      return -1;
    }
    BigDecimal latitude = milepost.getLatitude().setScale(14, BigDecimal.ROUND_HALF_UP);
    BigDecimal longitude = milepost.getLongitude().setScale(14, BigDecimal.ROUND_HALF_UP);
    for (int i = 0; i < mileposts.size(); i++) {
      Milepost currentMilepost = mileposts.get(i);
      BigDecimal currentLatitude = currentMilepost.getLatitude().setScale(14, BigDecimal.ROUND_HALF_UP);
      BigDecimal currentLongitude = currentMilepost.getLongitude().setScale(14, BigDecimal.ROUND_HALF_UP);
      if (latitude.equals(currentLatitude) && longitude.equals(currentLongitude)) {
        return i;
      }
    }
    return -1;
  }

  private String convertMilepostsToGeojsonString(List<Milepost> mileposts) {
    StringBuilder geojsonStringBuilder = new StringBuilder();
    geojsonStringBuilder.append("{ \"type\": \"FeatureCollection\", \"features\": [{ \"type\": \"Feature\", \"geometry\": { \"type\": \"LineString\", \"coordinates\": [");
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

  /**
   * Helper class to calculate distance between two points and total distance of a buffer path
   */
  public static class DistanceCalculator {
    public static double calculateDistanceInMiles(List<Milepost> buffer) {
      double distanceInMiles = 0;
      for (int i = 0; i < buffer.size() - 1; i++) {
        Milepost mp1 = buffer.get(i);
        Milepost mp2 = buffer.get(i + 1);
        double distanceInMeters = DistanceCalculator.calculateDistanceInMetersBetweenTwoPoints(mp1.getLatitude().doubleValue(),
            mp1.getLongitude().doubleValue(), mp2.getLatitude().doubleValue(), mp2.getLongitude().doubleValue());
        distanceInMiles += distanceInMeters / 1609.34;
      }
      return distanceInMiles;
    }

    public static double calculateDistanceInMetersBetweenTwoPoints(double lat1, double lon1, double lat2, double lon2) {
      final int R = 6371000; // Radius of the earth in meters
      double latDistance = Math.toRadians(lat2 - lat1);
      double lonDistance = Math.toRadians(lon2 - lon1);
      double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
          + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
          * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
      double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      return R * c; // Distance in meters
    }
  }

}