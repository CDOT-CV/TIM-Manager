package com.trihydro.cvdatacontroller.services;

import com.trihydro.library.helpers.DbInteractions;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name="config.milepostProvider", havingValue="neo4j")
public class MilepostDbImpl implements MilepostService {
    private final MilepostDbService milepostDbService;
    protected final DbInteractions dbInteractions;

    @Override
    public List<Milepost> getMilepostsByStartEndPoint(WydotTim wydotTim) {

        // check startPoint
        if (wydotTim.getStartPoint() == null || wydotTim.getStartPoint().getLatitude() == null
                || wydotTim.getStartPoint().getLongitude() == null) {
            return new ArrayList<>();
        }

        // check endpoint
        if (wydotTim.getEndPoint() == null || wydotTim.getEndPoint().getLatitude() == null
                || wydotTim.getEndPoint().getLongitude() == null) {
            return new ArrayList<>();
        }

        // check direction, route
        if (wydotTim.getDirection() == null || wydotTim.getRoute() == null) {
            return new ArrayList<>();
        }

        Collection<com.trihydro.cvdatacontroller.model.Milepost> data = milepostDbService.getPathWithBuffer(
                wydotTim.getRoute(), wydotTim.getStartPoint().getLatitude(), wydotTim.getStartPoint().getLongitude(),
                wydotTim.getEndPoint().getLatitude(), wydotTim.getEndPoint().getLongitude(), wydotTim.getDirection());
        return getMilepostsFromResponse(data);
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

        Collection<com.trihydro.cvdatacontroller.model.Milepost> data = milepostDbService.getPathWithSpecifiedBuffer(
                milepostBuffer.getCommonName(), milepostBuffer.getPoint().getLatitude(),
                milepostBuffer.getPoint().getLongitude(), milepostBuffer.getDirection(),
                milepostBuffer.getBufferMiles());
        return getMilepostsFromResponse(data);
    }

    @Override
    public List<String> getRoutes() {
            Connection connection = null;
            ResultSet rs = null;
            PreparedStatement preparedStatement = null;
            try {
                List<String> routes = new ArrayList<>();
                connection = dbInteractions.getConnectionPool();

                // build SQL query
                String statementStr = "select distinct common_name from MILEPOST_VW_NEW";
                preparedStatement = connection.prepareStatement(statementStr);
                rs = preparedStatement.executeQuery();

                while (rs.next()) {
                    routes.add(rs.getString("COMMON_NAME"));
                }
                return routes;
            } catch (SQLException e) {
                log.error("Exception", e);
                return new ArrayList<>();
            } finally {
                try {
                    // close prepared statement
                    if (preparedStatement != null)
                        preparedStatement.close();
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
    }

    private List<Milepost> getMilepostsFromResponse(Collection<com.trihydro.cvdatacontroller.model.Milepost> response) {
        List<Milepost> mileposts = new ArrayList<>();
        for (var milepost : response) {
            Milepost newMilepost = new Milepost();
            newMilepost.setCommonName(milepost.getCommonName());
            newMilepost.setMilepost(milepost.getMilepost());
            newMilepost.setDirection(milepost.getDirection());
            newMilepost.setLatitude(new BigDecimal(milepost.getLatitude().toString()).setScale(14, RoundingMode.HALF_UP));
            newMilepost.setLongitude(new BigDecimal(milepost.getLongitude().toString()).setScale(14, RoundingMode.HALF_UP));
            mileposts.add(newMilepost);
        }
        return mileposts;
    }
}
