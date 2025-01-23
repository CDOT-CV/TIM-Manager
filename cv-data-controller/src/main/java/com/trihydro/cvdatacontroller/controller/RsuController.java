package com.trihydro.cvdatacontroller.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.model.WydotRsu;
import com.trihydro.library.model.WydotRsuTim;

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
@ApiIgnore
public class RsuController extends BaseController {

	@RequestMapping(value = "/rsus", method = RequestMethod.GET, headers = "Accept=application/json")
	public ResponseEntity<List<WydotRsu>> SelectAllRsus() {
		ArrayList<WydotRsu> rsus = new ArrayList<WydotRsu>();
		Connection connection = null;
		ResultSet rs = null;
		Statement statement = null;

		try {
			connection = dbInteractions.getConnectionPool();
			statement = connection.createStatement();

			// select all RSUs from rsus table
			rs = statement.executeQuery("select rsu_id, ST_X(ST_AsText(geography)) as longitude, ST_Y(ST_AsText(geography)) as latitude, ipv4_address, primary_route, milepost from rsus order by milepost asc");

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

		} catch (SQLException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
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
				e.printStackTrace();
			}
		}

		return ResponseEntity.ok(rsus);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/rsus-for-tim/{timId}")
	public ResponseEntity<List<WydotRsuTim>> GetFullRsusTimIsOn(@PathVariable Long timId) {
		List<WydotRsuTim> rsus = new ArrayList<WydotRsuTim>();
		Connection connection = null;
		ResultSet rs = null;
		Statement statement = null;

		try {
			connection = dbInteractions.getConnectionPool();
			statement = connection.createStatement();

			// select all RSUs that are labeled as 'Existing' in the WYDOT view
			rs = statement.executeQuery(
					"select rsus.rsu_id, rsu_credentials.username as update_username, " +
					"rsu_credentials.password as update_password, ST_X(ST_AsText(rsus.geography)) " + 
					"as longitude, ST_Y(ST_AsText(rsus.geography)) as latitude, rsus.ipv4_address, " + 
					"tim_rsu.rsu_index from rsus inner join rsu_credentials on " + 
					"rsu_credentials.credential_id = rsus.credential_id inner join tim_rsu on " + 
					"tim_rsu.rsu_id = rsus.rsu_id where tim_rsu.tim_id = " + timId);

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
		} catch (SQLException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
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
				e.printStackTrace();
			}
		}
		return ResponseEntity.ok(rsus);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/rsus-by-geometry/{geometry}")
	public ResponseEntity<ArrayList<WydotRsu>> SelectRsusByGeometry(@PathVariable String geometry) {
		ArrayList<WydotRsu> rsus = new ArrayList<WydotRsu>();
		Connection connection = null;
		ResultSet rs = null;
		Statement statement = null;

		try {
			connection = dbInteractions.getConnectionPool();
			statement = connection.createStatement();

			// select all RSUs from RSU table
			rs = statement.executeQuery(
					"SELECT rsu_id, ST_X(ST_AsText(geography)) as longitude, " +
					"ST_Y(ST_AsText(geography)) as latitude, primary_route, milepost, ipv4_address, sc.username, sc.password " +
					"FROM rsus " + 
					"JOIN snmp_credentials AS sc ON rsus.snmp_credential_id = sc.snmp_credential_id " +
					"WHERE rsu_id NOT IN (" +
					"SELECT rsu_id " +
					"FROM rsu_organization AS ro " +
					"JOIN organizations AS o ON ro.organization_id = o.organization_id " +
					"WHERE o.name = 'Region 1') " +
					"AND ST_Intersects(" +
					"ST_Buffer(ST_GeomFromText('" + geometry + "'), 1), geography)"
			);

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
		} catch (SQLException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
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
				e.printStackTrace();
			}
		}
		return ResponseEntity.ok(rsus);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/rsus-by-route/{route}")
	public ResponseEntity<ArrayList<WydotRsu>> SelectRsusByRoute(@PathVariable String route) {
		ArrayList<WydotRsu> rsus = new ArrayList<WydotRsu>();
		Connection connection = null;
		ResultSet rs = null;
		Statement statement = null;

		try {
			connection = dbInteractions.getConnectionPool();
			statement = connection.createStatement();

			// select all RSUs from RSU table
			rs = statement.executeQuery(
					"select rsu_id, ST_X(ST_AsText(geography)) as longitude, ST_Y(ST_AsText(geography)) as latitude, ipv4_address, primary_route, milepost from rsus where primary_route like %'%" + route + "%'");

			while (rs.next()) {
				WydotRsu rsu = new WydotRsu();
				rsu.setRsuId(rs.getInt("RSU_ID"));
				rsu.setRsuTarget(rs.getString("IPV4_ADDRESS"));
				rsu.setLatitude(rs.getBigDecimal("LATITUDE"));
				rsu.setLongitude(rs.getBigDecimal("LONGITUDE"));
				rsu.setRoute(rs.getString("ROUTE"));
				rsu.setMilepost(rs.getDouble("MILEPOST"));
				rsus.add(rsu);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rsus);
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
				e.printStackTrace();
			}
		}
		return ResponseEntity.ok(rsus);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/active-rsu-tim-indexes/{rsuId}")
	public ResponseEntity<List<Integer>> GetActiveRsuTimIndexes(@PathVariable Integer rsuId) {
		List<Integer> indexes = new ArrayList<Integer>();

		Connection connection = null;
		ResultSet rs = null;
		PreparedStatement statement = null;

		try {
			// select all RSUs from RSU table
			var sql = "select rsu_index from active_tim inner join tim_rsu on active_tim.tim_id = tim_rsu.tim_id"
					+ " where sat_record_id is null and rsu_id = ?";

			connection = dbInteractions.getConnectionPool();
			statement = connection.prepareStatement(sql);
			statement.setLong(1, rsuId);
			
			rs = statement.executeQuery();

			while (rs.next()) {
				indexes.add(rs.getInt("RSU_INDEX"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(indexes);
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
				e.printStackTrace();
			}
		}
		return ResponseEntity.ok(indexes);
	}
}