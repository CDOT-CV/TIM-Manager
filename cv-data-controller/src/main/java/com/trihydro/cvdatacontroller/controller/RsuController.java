package com.trihydro.cvdatacontroller.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.model.WydotRsu;
import com.trihydro.library.model.WydotRsuTim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import springfox.documentation.annotations.ApiIgnore;

@CrossOrigin
@RestController
@Slf4j
@ApiIgnore
public class RsuController extends BaseController {

    @RequestMapping(value = "/rsus", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<List<WydotRsu>> selectAllRsus() {
        ArrayList<WydotRsu> rsus = new ArrayList<WydotRsu>();

        // select all RSUs from rsus table
        String query = "SELECT rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "ipv4_address, primary_route, milepost FROM rsus ORDER BY milepost ASC";

        try (Connection connection = dbInteractions.getConnectionPool();
             PreparedStatement statement = connection.prepareStatement(query)) {

            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    WydotRsu rsu = new WydotRsu();
                    rsu.setRsuId(rs.getInt("RSU_ID"));
                    rsu.setRsuTarget(rs.getString("IPV4_ADDRESS"));
                    rsu.setLatitude(rs.getBigDecimal("LATITUDE"));
                    rsu.setLongitude(rs.getBigDecimal("LONGITUDE"));
                    rsu.setRoute(rs.getString("PRIMARY_ROUTE"));
                    rsu.setMilepost(rs.getDouble("MILEPOST"));
                    rsus.add(rsu);
                }
            }

        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
        }

        return ResponseEntity.ok(rsus);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/rsus-for-tim/{timId}")
    public ResponseEntity<List<WydotRsuTim>> getFullRsusTimIsOn(@PathVariable Long timId) {
        List<WydotRsuTim> rsus = new ArrayList<WydotRsuTim>();

        String query = "SELECT rsus.rsu_id, rsu_credentials.username AS update_username, "
                + "rsu_credentials.password AS update_password, ST_X(ST_AsText(rsus.geography)) AS longitude, "
                + "ST_Y(ST_AsText(rsus.geography)) AS latitude, rsus.ipv4_address, tim_rsu.rsu_index "
                + "FROM rsus "
                + "INNER JOIN rsu_credentials ON rsu_credentials.credential_id = rsus.credential_id "
                + "INNER JOIN tim_rsu ON tim_rsu.rsu_id = rsus.rsu_id "
                + "WHERE tim_rsu.tim_id = ?";

        try(Connection connection = dbInteractions.getConnectionPool();
            PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setLong(1, timId);

            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    WydotRsuTim rsu = new WydotRsuTim();
                    rsu.setRsuTarget(rs.getString("IPV4_ADDRESS"));
                    rsu.setLatitude(rs.getBigDecimal("LATITUDE"));
                    rsu.setLongitude(rs.getBigDecimal("LONGITUDE"));
                    rsu.setIndex(rs.getInt("RSU_INDEX"));
                    rsu.setRsuUsername(rs.getString("UPDATE_USERNAME"));
                    rsu.setRsuPassword(rs.getString("UPDATE_PASSWORD"));
                    // only add unique values in
                    if (!rsus.contains(rsu)) {
                        rsus.add(rsu);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
        }
        return ResponseEntity.ok(rsus);
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/rsus-by-geometry/{geometry}")
    public ResponseEntity<ArrayList<WydotRsu>> selectRsusByGeometry(@PathVariable String geometry) {
        ArrayList<WydotRsu> rsus = new ArrayList<>();

        String query = "SELECT rsus.rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "primary_route, milepost, ipv4_address, sc.username, sc.password "
                + "FROM rsus "
                + "JOIN snmp_credentials AS sc ON rsus.snmp_credential_id = sc.snmp_credential_id "
                + "JOIN rsu_options AS ro ON rsus.rsu_id = ro.rsu_id "
                + "WHERE ro.tim_deposit = true "
                + "AND ST_Intersects(ST_Buffer(ST_GeomFromText(?), 1), geography)";

        try(Connection connection = dbInteractions.getConnectionPool();
            PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, geometry);

            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    WydotRsu rsu = new WydotRsu();
                    rsu.setRsuId(rs.getInt("RSU_ID"));
                    rsu.setRsuTarget(rs.getString("IPV4_ADDRESS"));
                    rsu.setLatitude(rs.getBigDecimal("LATITUDE"));
                    rsu.setLongitude(rs.getBigDecimal("LONGITUDE"));
                    rsu.setRoute(rs.getString("PRIMARY_ROUTE"));
                    rsu.setMilepost(rs.getDouble("MILEPOST"));
                    rsu.setRsuUsername(rs.getString("USERNAME"));
                    rsu.setRsuPassword(rs.getString("PASSWORD"));
                    rsus.add(rsu);
                }
            }
        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
        }
        return ResponseEntity.ok(rsus);
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/rsus-by-route/{route}")
    public ResponseEntity<ArrayList<WydotRsu>> selectRsusByRoute(@PathVariable String route) {
        ArrayList<WydotRsu> rsus = new ArrayList<WydotRsu>();

        // select all RSUs from RSU table
        String query = "SELECT rsu_id, ST_X(ST_AsText(geography)) AS longitude, ST_Y(ST_AsText(geography)) AS latitude, "
                + "ipv4_address, primary_route, milepost FROM rsus WHERE primary_route LIKE ?";

        try(Connection connection = dbInteractions.getConnectionPool();
            PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, "%" + route + "%");

            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    WydotRsu rsu = new WydotRsu();
                    rsu.setRsuId(rs.getInt("RSU_ID"));
                    rsu.setRsuTarget(rs.getString("IPV4_ADDRESS"));
                    rsu.setLatitude(rs.getBigDecimal("LATITUDE"));
                    rsu.setLongitude(rs.getBigDecimal("LONGITUDE"));
                    rsu.setRoute(rs.getString("PRIMARY_ROUTE"));
                    rsu.setMilepost(rs.getDouble("MILEPOST"));
                    rsus.add(rsu);
                }
            }
        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
        }
        return ResponseEntity.ok(rsus);
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/active-rsu-tim-indexes/{rsuId}")
    public ResponseEntity<List<Integer>> getActiveRsuTimIndexes(@PathVariable Integer rsuId) {
        List<Integer> indexes = new ArrayList<Integer>();

        // select all RSUs from RSU table
        var sql = "SELECT rsu_index FROM active_tim "
                + "INNER JOIN tim_rsu ON active_tim.tim_id = tim_rsu.tim_id "
                + "WHERE sat_record_id IS NULL AND rsu_id = ?";

        try(Connection connection = dbInteractions.getConnectionPool();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, rsuId);

            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    indexes.add(rs.getInt("RSU_INDEX"));
                }
            }

        } catch (SQLException e) {
            log.error("Exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(indexes);
        }
        return ResponseEntity.ok(indexes);
    }
}