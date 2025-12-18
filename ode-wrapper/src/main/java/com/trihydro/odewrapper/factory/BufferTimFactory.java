package com.trihydro.odewrapper.factory;

import com.trihydro.library.model.Coordinate;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;
import com.trihydro.library.service.MilepostService;
import com.trihydro.odewrapper.helpers.SetItisCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface BufferTimFactory {
    default List<WydotTim> makeIncreasingTims(WydotTim wydotTim, List<Integer> bufferTimITISCodes, MilepostService milepostService) {
        return makeOneWayTims("I", wydotTim, bufferTimITISCodes, milepostService);
    }

    default List<Coordinate> milepostToGeometry(List<Milepost> mileposts) {
        var timGeometry = new ArrayList<Coordinate>();
        for (Milepost milepost : mileposts) {
            timGeometry.add(new Coordinate(milepost.getLatitude(), milepost.getLongitude()));
        }
        return timGeometry;
    }

    default List<WydotTim> makeDecreasingTims(WydotTim wydotTim, List<Integer> bufferTimITISCodes, MilepostService milepostService) {
        return makeOneWayTims("d", wydotTim, bufferTimITISCodes, milepostService);
    }

    private List<WydotTim> makeOneWayTims(String direction, WydotTim wydotTim, List<Integer> bufferTimITISCodes, MilepostService milepostService) {
        var timOneWay = wydotTim.copy();
        timOneWay.setDirection(direction);

        var timsFromItisCodes = buildTimsFromItisCodes(timOneWay, bufferTimITISCodes, false);
        var timsToSend = new ArrayList<>(timsFromItisCodes);

        var bufferTims = makeBufferTims(wydotTim, bufferTimITISCodes, milepostService);
        timsToSend.addAll(bufferTims);
        return timsToSend;
    }

    default List<WydotTim> makeBufferTims(WydotTim wydotTim, List<Integer> bufferTimITISCodes, MilepostService milepostService) {
        if (wydotTim.getRoute() != null) {
            List<Milepost> mileposts = wydotTim.toMileposts();
            List<Milepost> bufferMps;
            if (mileposts.isEmpty()) {
                if (wydotTim.getEndPoint() != null) {
                    mileposts = milepostService.getMilepostsByStartEndPointDirection(wydotTim);
                    // If the route of the TIM is supported, create a buffer based on mileposts associated with that route
                    bufferMps = milepostService.getBufferForPath(wydotTim.getRoute().replace('-', '_'), 1.0, mileposts);
                } else {
                    // point incident, buffered
                    MilepostBuffer mpb = new MilepostBuffer();
                    mpb.setBufferMiles(1.0);
                    mpb.setCommonName(wydotTim.getRoute());
                    mpb.setDirection(wydotTim.getDirection());
                    mpb.setPoint(wydotTim.getStartPoint());
                    bufferMps = milepostService.getMilepostsByPointWithBuffer(mpb);
                }
            } else {
                bufferMps = mileposts;
            }
            wydotTim.setGeometry(milepostToGeometry(bufferMps));
            wydotTim.setClientId(wydotTim.getClientId() + "%BUFF");
        }

        return buildTimsFromItisCodes(wydotTim, bufferTimITISCodes,true);
    }

    default List<WydotTim> buildTimsFromItisCodes(WydotTim tim, List<Integer> bufferTimITISCodes, boolean isBuffer) {
        var timsToSend = new ArrayList<WydotTim>();
        for (String itisCodeEntry : tim.getItisCodes()) {
            // CTW Update requires that individual TIMs are created for each ITIS ordering
            List<String> itisCodes = Arrays.asList(itisCodeEntry.split(" "));

            // only generate appropriate TIMs for geometry list (buffer, workZone)
            String lastItisCode = itisCodes.get(itisCodes.size() - 1);
            boolean isBufferTim = bufferTimITISCodes.contains(Integer.valueOf(lastItisCode));
            if (isBuffer && !isBufferTim) {
                continue;
            } else if (!isBuffer && isBufferTim) {
                continue;
            }

            WydotTim timToSend = tim.copy();
            timToSend.setItisCodes(itisCodes);
            var itisCodeAbb = SetItisCodes.getItisCodeAbbreviation(itisCodeEntry);
            String clientIdWithItis = tim.getClientId() + '-' + tim.getDirection() + '-' + itisCodeAbb;
            timToSend.setClientId(clientIdWithItis);
            timsToSend.add(timToSend);
        }

        return timsToSend;
    }

}
