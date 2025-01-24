package com.trihydro.rsudatacontroller.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.trihydro.library.helpers.DbInteractions;
import com.trihydro.library.helpers.Utility;
import com.trihydro.rsudatacontroller.config.BasicConfiguration;
import com.trihydro.rsudatacontroller.model.RsuTim;
import com.trihydro.rsudatacontroller.process.ProcessFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.plugin.SnmpProtocol;

@Component
public class RsuService {
    private static final String OID_FOUR_DOT_ONE_SRM_DELIVERY_START = "1.0.15628.4.1.4.1.7";
    private static final String OID_NTCIP_1218_DELIVERY_START = "1.3.6.1.4.1.1206.4.2.18.3.2.1.5";
    private static final DateTimeFormatter rsuDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ProcessFactory processFactory;
    private BasicConfiguration config;
    private Utility utility;
    protected DbInteractions dbInteractions;

    @Autowired
    public void InjectDependencies(ProcessFactory processFactory, BasicConfiguration config, DbInteractions _dbInteractions, Utility utility) {
        this.processFactory = processFactory;
        this.config = config;
        this.utility = utility;
        this.dbInteractions = _dbInteractions;
    }

    /**
     * Gets the deliveryStart times for each index of the provided RSU
     * 
     * @param rsuIpv4Address IPv4 address of the RSU
     * @return null if timeout occurred (unable to establish snmp session with RSU)
     * @throws Exception if unable to invoke command to perform SNMP communication
     */
    public List<RsuTim> getAllDeliveryStartTimes(String rsuIpv4Address) throws Exception {
        Process p;

        RSU rsu = getRSU(rsuIpv4Address);

        if (rsu.getSnmpProtocol() == SnmpProtocol.FOURDOT1) {
            p = processFactory.buildAndStartProcess("snmpwalk", "-v", "3", "-r",
                Integer.toString(config.getSnmpRetries()), "-t", Integer.toString(config.getSnmpTimeoutSeconds()), "-u",
                rsu.getRsuUsername(), "-l", config.getSnmpSecurityLevel(), "-a", config.getSnmpAuthProtocol(), "-A",
                rsu.getRsuPassword(), rsuIpv4Address, OID_FOUR_DOT_ONE_SRM_DELIVERY_START);
        } else {
            p = processFactory.buildAndStartProcess("snmpwalk", "-v", "3", "-r",
                Integer.toString(config.getSnmpRetries()), "-t", Integer.toString(config.getSnmpTimeoutSeconds()), "-u",
                rsu.getRsuUsername(), "-l", config.getSnmpSecurityLevel(), "-a", config.getSnmpAuthProtocol(), "-A",
                rsu.getRsuPassword(), rsuIpv4Address, OID_NTCIP_1218_DELIVERY_START);
        }

        String snmpWalkOutput = getProcessOutput(p);

        // If timeout occurred, return null
        if (snmpWalkOutput.matches("snmpwalk: Timeout")) {
            utility.logWithDate("SNMP Timeout occurred (RSU: " + rsuIpv4Address + ")");
            return null;
        }

        List<RsuTim> tims = new ArrayList<>();

        Pattern ip = Pattern.compile("\\.(\\d*) =");
        Pattern hp = Pattern.compile("Hex-STRING: ((?:[0-9|A-F]{2}\\s?)*)");
        Matcher im;
        Matcher hm;
        for (String line : snmpWalkOutput.split("\n")) {
            im = ip.matcher(line);
            hm = hp.matcher(line);

            if (im.find() && hm.find()) {
                RsuTim tim = new RsuTim();
                tim.setIndex(Integer.valueOf(im.group(1)));
                tim.setDeliveryStartTime(hexStringToDateTime(hm.group(1)));

                tims.add(tim);
            }
        }
        return tims;
    }

    private String getProcessOutput(Process p) throws IOException {
        String output = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            output += line + "\n";
        }
        if (output.length() > 0) {
            output = output.substring(0, output.length() - 1);
        }

        return output;
    }

    private String hexStringToDateTime(String hexString) {
        String[] octets = hexString.split(" ");
        if (octets.length != 6) {
            return null;
        }

        int year = Integer.parseInt(octets[0] + octets[1], 16);
        int month = Integer.parseInt(octets[2], 16);
        int day = Integer.parseInt(octets[3], 16);
        int hour = Integer.parseInt(octets[4], 16);
        int minute = Integer.parseInt(octets[5], 16);

        LocalDateTime date = LocalDateTime.of(year, month, day, hour, minute, 0);
        return date.format(rsuDateTimeFormatter);
    }

    private RSU getRSU(String rsuIpv4Address) throws Exception {
        // Need to grab firmware type, username, password.
        Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
        RSU rsu = null;
        

		try {
			connection = dbInteractions.getConnectionPool();

			statement = connection.createStatement();

			// The inner subqueries leave us with a list of tim_ids that aren't associated
			// with any valid itis codes. Select the active_tims with
			// those tim_ids
            String selectStatement = "select rc.username, rc.password, sp.nickname FROM rsus " +
                                "JOIN rsu_credentials AS rc " + //
                                "ON rsus.snmp_credential_id = rc.credential_id " + //
                                "JOIN snmp_protocols AS sp " + //
                                "ON rsus.snmp_protocol_id = sp.snmp_protocol_id " + //
                                "WHERE rsus.ipv4_address = '" + rsuIpv4Address + "'";

			rs = statement.executeQuery(selectStatement);

            // parse resultSet
            while (rs.next()) {
                String rsuUsername = rs.getString("username");
                String rsuPassword = rs.getString("password");
                SnmpProtocol snmpProtocol = rs.getString("nickname").equals("NTCIP 1218") ? SnmpProtocol.NTCIP1218 : SnmpProtocol.FOURDOT1;
                
                rsu = new RSU(rsuIpv4Address, rsuUsername, rsuPassword, config.getSnmpRetries(), config.getSnmpTimeoutSeconds(), snmpProtocol);
            }



		} catch (SQLException e) {
			e.printStackTrace();
			return null;
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
        
        return rsu;
    }
}