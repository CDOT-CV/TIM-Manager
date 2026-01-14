package com.trihydro.cvdatacontroller.services;

import com.trihydro.cvdatacontroller.repositories.MilepostRepository;
import com.trihydro.library.helpers.DbInteractions;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name="config.milepostProvider", havingValue="neo4j")
public class MilepostDbImpl implements MilepostService {
    private final MilepostRepository milepostDbService;
    protected final DbInteractions dbInteractions;

    @Override
    public List<Milepost> getMilepostsByStartEndPoint(WydotTim wydotTim) {

        // check startPoint
        if (wydotTim.getStartPoint() == null || wydotTim.getStartPoint().getLatitude() == null
                || wydotTim.getStartPoint().getLongitude() == null) {
            return Collections.emptyList();
        }

        // check endpoint
        if (wydotTim.getEndPoint() == null || wydotTim.getEndPoint().getLatitude() == null
                || wydotTim.getEndPoint().getLongitude() == null) {
            return Collections.emptyList();
        }

        // check direction, route
        if (wydotTim.getDirection() == null || wydotTim.getRoute() == null) {
            return Collections.emptyList();
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
            return Collections.emptyList();
        }

        // check direction, route
        if (milepostBuffer.getDirection() == null || milepostBuffer.getCommonName() == null) {
            return Collections.emptyList();
        }

        Collection<com.trihydro.cvdatacontroller.model.Milepost> data = milepostDbService.getPathWithSpecifiedBuffer(
                milepostBuffer.getCommonName(), milepostBuffer.getPoint().getLatitude(),
                milepostBuffer.getPoint().getLongitude(), milepostBuffer.getDirection(),
                milepostBuffer.getBufferMiles());
        return getMilepostsFromResponse(data);
    }

    public List<String> getRoutes() {
        String statementStr = "select distinct common_name from MILEPOST_VW_NEW";
        try (Connection connection = dbInteractions.getConnectionPool();
             PreparedStatement preparedStatement = connection.prepareStatement(statementStr);
             ResultSet rs = preparedStatement.executeQuery()
        ) {
            List<String> routes = new ArrayList<>();

            while (rs.next()) {
                routes.add(rs.getString("COMMON_NAME"));
            }
            return routes;
        } catch (SQLException e) {
            log.error("Exception", e);
            return new ArrayList<>();
        }
    }

    private List<Milepost> getMilepostsFromResponse(Collection<com.trihydro.cvdatacontroller.model.Milepost> response) {
        List<Milepost> mileposts = new ArrayList<>();
        for (var milepost : response) {
            Milepost newMilepost = new Milepost();
            newMilepost.setCommonName(milepost.getCommonName());
            newMilepost.setMilepost(milepost.getMilepost());
            newMilepost.setDirection(milepost.getDirection());
            newMilepost.setLatitude(milepost.getLatitude());
            newMilepost.setLongitude(milepost.getLongitude());
            mileposts.add(newMilepost);
        }
        return mileposts;
    }
}
