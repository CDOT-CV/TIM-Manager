package com.trihydro.loggerkafkaconsumer.app.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.trihydro.library.helpers.SQLNullHandler;
import com.trihydro.library.tables.TimOracleTables;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;

@Component
public class NodeXYService extends BaseService {

    private TimOracleTables timOracleTables;
    private SQLNullHandler sqlNullHandler;

    @Autowired
    public void InjectDependencies(TimOracleTables _timOracleTables, SQLNullHandler _sqlNullHandler) {
        timOracleTables = _timOracleTables;
        sqlNullHandler = _sqlNullHandler;
    }

    public Long AddNodeXY(OdeTravelerInformationMessage.NodeXY nodeXY) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = dbInteractions.getConnectionPool();
            String insertQueryStatement = timOracleTables.buildInsertQueryStatement("node_xy",
                    timOracleTables.getNodeXYTable());
            preparedStatement = connection.prepareStatement(insertQueryStatement, new String[] { "node_xy_id" });
            int fieldNum = 1;

            for (String col : timOracleTables.getNodeXYTable()) {
                if (col.equals("DELTA"))
                    sqlNullHandler.setStringOrNull(preparedStatement, fieldNum, nodeXY.getDelta());
                else if (col.equals("NODE_LAT"))
                    sqlNullHandler.setBigDecimalOrNull(preparedStatement, fieldNum, nodeXY.getNodeLat());
                else if (col.equals("NODE_LONG"))
                    sqlNullHandler.setBigDecimalOrNull(preparedStatement, fieldNum, nodeXY.getNodeLong());
                else if (col.equals("X"))
                    sqlNullHandler.setBigDecimalOrNull(preparedStatement, fieldNum, nodeXY.getX());
                else if (col.equals("Y"))
                    sqlNullHandler.setBigDecimalOrNull(preparedStatement, fieldNum, nodeXY.getY());
                else if (col.equals("ATTRIBUTES_DWIDTH"))
                    if (nodeXY.getAttributes() != null)
                        sqlNullHandler.setBigDecimalOrNull(preparedStatement, fieldNum,
                                nodeXY.getAttributes().getdWidth());
                    else
                        preparedStatement.setString(fieldNum, null);
                else if (col.equals("ATTRIBUTES_DELEVATION"))
                    if (nodeXY.getAttributes() != null)
                        sqlNullHandler.setBigDecimalOrNull(preparedStatement, fieldNum,
                                nodeXY.getAttributes().getdElevation());
                    else
                        preparedStatement.setString(fieldNum, null);
                fieldNum++;
            }
            Long nodeXYId = dbInteractions.executeAndLog(preparedStatement, "nodexy");
            return nodeXYId;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                // close prepared statement
                if (preparedStatement != null)
                    preparedStatement.close();
                // return connection back to pool
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return Long.valueOf(0);
    }
}