package com.trihydro.cvdatacontroller.controller;

import com.trihydro.cvdatacontroller.services.MilepostService;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.SetMilepostCacheRequest;
import com.trihydro.library.model.WydotTim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@Slf4j
@ApiIgnore
public class MilepostController extends BaseController {

    private MilepostService milepostService;
    private final HashMap<String, List<Milepost>> milepostCache = new HashMap<>();

    @Autowired
    public void InjectDependencies(MilepostService _milepostService) {
        this.milepostService = _milepostService;
    }

    @RequestMapping(value = "/routes", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getRoutes() {

        List<String> routes = milepostService.getRoutes();

        if (routes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(routes);
    }

    /**
     * Fetch mileposts from the view by their common name. Used for TimCreator tool
     *
     * @param commonName
     * @param mod
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/mileposts-common-name/{commonName}/{mod}")
    public ResponseEntity<List<Milepost>> getMilepostsCommonName(@PathVariable String commonName,
                                                                 @PathVariable Boolean mod) {
        List<Milepost> mileposts = new ArrayList<Milepost>();
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;

        try {

            // build statement SQL query
            connection = dbInteractions.getConnectionPool();
            statement = connection.createStatement();

            // build statement SQL query
            String sqlString = "select * from MILEPOST_VW_NEW where COMMON_NAME = '" + commonName + "'";

            if (mod)
                sqlString += " and MOD(milepost, 1) = 0";

            rs = statement.executeQuery(sqlString);

            // convert result to milepost objects
            while (rs.next()) {
                Milepost milepost = new Milepost();
                milepost.setCommonName(rs.getString("COMMON_NAME"));
                milepost.setMilepost(rs.getDouble("MILEPOST"));
                milepost.setDirection(rs.getString("DIRECTION"));
                milepost.setLatitude(rs.getBigDecimal("LATITUDE"));
                milepost.setLongitude(rs.getBigDecimal("LONGITUDE"));
                mileposts.add(milepost);
            }
        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mileposts);
        } finally {
            try {
                // close prepared statement
                if (statement != null)
                    statement.close();
                // return connection back to pool
                if (connection != null)
                    connection.close();
                // close result set
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                log.error("Exception", e);
            }
        }
        return ResponseEntity.ok(mileposts);
    }

    // the milepost_vw
    @RequestMapping(method = RequestMethod.GET, value = "/get-milepost-range/{direction}/{fromMilepost}/{toMilepost}/{commonName}")
    public ResponseEntity<List<Milepost>> getMilepostRange(@PathVariable String direction,
                                                           @PathVariable String commonName, @PathVariable Double fromMilepost, @PathVariable Double toMilepost) {
        List<Milepost> mileposts = new ArrayList<Milepost>();
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;

        try {

            connection = dbInteractions.getConnectionPool();
            statement = connection.createStatement();

            // build SQL query
            String statementStr = "select * from MILEPOST_VW_NEW where direction = '" + translateDirection(direction)
                    + "' and milepost between " + Math.min(fromMilepost, toMilepost) + " and "
                    + Math.max(fromMilepost, toMilepost) + " and common_name = '" + commonName + "'";

            if (fromMilepost < toMilepost)
                rs = statement.executeQuery(statementStr + " order by milepost asc");
            else
                rs = statement.executeQuery(statementStr + " order by milepost desc");

            // convert result to milepost objects
            while (rs.next()) {
                Milepost milepost = new Milepost();
                milepost.setCommonName(rs.getString("COMMON_NAME"));
                milepost.setMilepost(rs.getDouble("MILEPOST"));
                milepost.setDirection(rs.getString("DIRECTION"));
                milepost.setLatitude(rs.getBigDecimal("LATITUDE"));
                milepost.setLongitude(rs.getBigDecimal("LONGITUDE"));
                mileposts.add(milepost);
            }

            if (mileposts.size() == 0) {
                log.info("Unable to find mileposts with query: {}", statementStr);
            }
        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mileposts);
        } finally {
            try {
                // close prepared statement
                if (statement != null)
                    statement.close();
                // return connection back to pool
                if (connection != null)
                    connection.close();
                // close result set
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                log.error("Exception", e);
            }
        }
        return ResponseEntity.ok(mileposts);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/get-milepost-range-no-direction/{fromMilepost}/{toMilepost}/{commonName}")
    public ResponseEntity<List<Milepost>> getMilepostRangeNoDirection(@PathVariable String commonName,
                                                                      @PathVariable Double fromMilepost, @PathVariable Double toMilepost) {
        List<Milepost> mileposts = new ArrayList<Milepost>();
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;

        try {

            connection = dbInteractions.getConnectionPool();
            statement = connection.createStatement();

            // build SQL query
            String statementStr = "select * from MILEPOST_VW_NEW where milepost between "
                    + Math.min(fromMilepost, toMilepost) + " and " + Math.max(fromMilepost, toMilepost)
                    + " and common_name = '" + commonName + "'";

            if (fromMilepost < toMilepost)
                rs = statement.executeQuery(statementStr + " order by milepost asc");
            else
                rs = statement.executeQuery(statementStr + " order by milepost desc");

            // convert result to milepost objects
            while (rs.next()) {
                Milepost milepost = new Milepost();
                milepost.setCommonName(rs.getString("COMMON_NAME"));
                milepost.setMilepost(rs.getDouble("MILEPOST"));
                milepost.setLatitude(rs.getBigDecimal("LATITUDE"));
                milepost.setLongitude(rs.getBigDecimal("LONGITUDE"));
                mileposts.add(milepost);
            }
        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mileposts);
        } finally {
            try {
                // close prepared statement
                if (statement != null)
                    statement.close();
                // return connection back to pool
                if (connection != null)
                    connection.close();
                // close result set
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                log.error("Exception", e);
            }
        }
        return ResponseEntity.ok(mileposts);
    }

    private String translateDirection(String direction) {
        switch (direction.toLowerCase()) {
            case "northbound":
            case "eastbound":
            case "eastward":
                return "I";

            case "southbound":
            case "westbound":
            case "westward":
                return "D";

            case "both":
                return "B";

            default:
                return direction.toUpperCase();
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json", value = "get-milepost-start-end")
    public ResponseEntity<List<Milepost>> getMilepostsByStartEndPoint(@RequestBody WydotTim wydotTim) throws Exception {

        List<Milepost> mileposts = milepostService.getMilepostsByStartEndPoint(wydotTim);

        if (mileposts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mileposts);
        }

        return ResponseEntity.ok(mileposts);
    }

    @RequestMapping(method = RequestMethod.POST, value="/set-milepost-cache")
    public ResponseEntity<String> setMilepostCache(@RequestBody SetMilepostCacheRequest milepostCacheBody) {

        if (milepostCacheBody.getMileposts().isEmpty() || milepostCacheBody.getTimID() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Request: please provide a valid milepost list and timID");
        }
        if (milepostCache.containsKey(milepostCacheBody.getTimID())) {
            log.info("Updating milepost cache for timID: {}", milepostCacheBody.getTimID());
        } else {
            log.info("Setting milepost cache for timID: {}", milepostCacheBody.getTimID());
        }
        milepostCache.put(milepostCacheBody.getTimID(), milepostCacheBody.getMileposts());
        return ResponseEntity.ok("Milepost cache set successfully for timID: " + milepostCacheBody.getTimID());
    }

    @RequestMapping(method = RequestMethod.GET, value="/get-milepost-cache/{timID}")
    public ResponseEntity<List<Milepost>> getMilepostCacheByTimID(@PathVariable String timID) {
        List<Milepost> mileposts = new ArrayList<>();

        if (milepostCache.containsKey(timID)) {
            mileposts = milepostCache.get(timID);
            log.info("Found {} mileposts in cache for timID: {}", mileposts.size(), timID);
            return ResponseEntity.ok(milepostCache.get(timID));
        }

        return ResponseEntity.ok(mileposts);
    }

    @RequestMapping(method = RequestMethod.DELETE, value="/delete-milepost-cache/{timID}")
    public ResponseEntity<String> deleteMilepostCache(@PathVariable String timID) {
        log.info("Deleting milepost cache for timID: {}", timID);

        if (milepostCache.containsKey(timID)) {
            milepostCache.remove(timID);
            return ResponseEntity.ok("Milepost cache deleted successfully for timID: " + timID);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Milepost cache not found for timID: " + timID);
    }

    @RequestMapping(method = RequestMethod.GET, value="/clear-milepost-cache")
    public ResponseEntity<String> clearMilepostCache() {
        log.info("Clearing milepost cache");
        List<String> clientIDs = new ArrayList<>(milepostCache.keySet());
        List<String> activeTimClientIds = getActiveTimClientIds();
        // remove all active TIM IDs from the list of milepost cache TIM IDs
        clientIDs.removeAll(activeTimClientIds);
        for (String clientID : clientIDs) {
            milepostCache.remove(clientID);
        }
        return ResponseEntity.ok("Milepost cache cleared successfully");
    }

    private List<String> getActiveTimClientIds() {
        List<String> activeTimIds = new ArrayList<>();
        String sql = "SELECT client_id FROM active_tim WHERE marked_for_deletion = False";
        try (Connection connection = dbInteractions.getConnectionPool();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String timId = rs.getString("CLIENT_ID");
                activeTimIds.add(timId);
            }
        } catch (SQLException e) {
            log.error("Error retrieving active TIM IDs ", e); // Improved logging
        }
        return activeTimIds;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json", value = "/get-milepost-single-point")
    public ResponseEntity<List<Milepost>> getMilepostsByPointWithBuffer(
            @RequestBody MilepostBuffer milepostBuffer) throws Exception {

        List<Milepost> mileposts = milepostService.getMilepostsByPointWithBuffer(milepostBuffer);

        if (mileposts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mileposts);
        }

        return ResponseEntity.ok(mileposts);
    }

    /**
     * Needed for TIM Creator
     *
     * @return
     */
    @RequestMapping(value = "/mileposts-test", method = RequestMethod.GET, headers = "Accept=application/json")
    public List<Milepost> getMilepostsTest() {

        List<Milepost> mileposts = new ArrayList<Milepost>();
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;

        try {
            connection = dbInteractions.getConnectionPool();
            statement = connection.createStatement();
            rs = statement.executeQuery("select * from MILEPOST_TEST order by milepost asc");

            // convert result to milepost objects
            while (rs.next()) {
                Milepost milepost = new Milepost();
                // milepost.setMilepostId(rs.getInt("milepost_id"));
                milepost.setCommonName(rs.getString("route"));
                milepost.setMilepost(rs.getDouble("milepost"));
                milepost.setDirection(rs.getString("direction"));
                milepost.setLatitude(rs.getBigDecimal("latitude"));
                milepost.setLongitude(rs.getBigDecimal("longitude"));
                mileposts.add(milepost);
            }
        } catch (SQLException e) {
            log.error("Exception", e);
        } finally {
            try {
                // close prepared statement
                if (statement != null)
                    statement.close();
                // return connection back to pool
                if (connection != null)
                    connection.close();
                // close result set
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                log.error("Exception", e);
            }
        }
        return mileposts;
    }

    /**
     * Needed for TIM Creator
     *
     * @param direction
     * @param route
     * @param start
     * @param end
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/get-milepost-test-range/{direction}/{start}/{end}/{route}")
    public List<Milepost> getMilepostTestRange(@PathVariable String direction, @PathVariable String route,
                                               @PathVariable Double start, @PathVariable Double end) {
        List<Milepost> mileposts = new ArrayList<Milepost>();
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;

        try {

            connection = dbInteractions.getConnectionPool();
            statement = connection.createStatement();

            // build SQL query
            String statementStr = "select * from MILEPOST_TEST where direction = '" + direction
                    + "' and milepost between " + Math.min(start, end) + " and " + Math.max(start, end)
                    + " and route like '%" + route + "%'";

            if (start < end)
                rs = statement.executeQuery(statementStr + "order by milepost asc");
            else
                rs = statement.executeQuery(statementStr + "order by milepost desc");

            // convert result to milepost objects
            while (rs.next()) {
                Milepost milepost = new Milepost();
                milepost.setCommonName(rs.getString("route"));
                milepost.setMilepost(rs.getDouble("milepost"));
                milepost.setDirection(rs.getString("direction"));
                milepost.setLatitude(rs.getBigDecimal("latitude"));
                milepost.setLongitude(rs.getBigDecimal("longitude"));
                mileposts.add(milepost);
            }
        } catch (SQLException e) {
            log.error("Exception", e);
        } finally {
            try {
                // close prepared statement
                if (statement != null)
                    statement.close();
                // return connection back to pool
                if (connection != null)
                    connection.close();
                // close result set
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                log.error("Exception", e);
            }
        }
        return mileposts;
    }
}
