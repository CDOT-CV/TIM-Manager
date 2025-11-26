package com.trihydro.library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class TimUpdateModelTest {

    /**
     * Tests the `toString` method of the `TimUpdateModel` class.
     * Verifies that the method produces a correct string representation of the object
     * by focusing on different combinations of field values.
     */

    @Test
    public void testToString_SomeFieldsPopulated() {
        TimUpdateModel model = new TimUpdateModel();
        model.setActiveTimId(1L);
        model.setPacketId("12345");
        model.setStartPoint(new Coordinate(new BigDecimal("37.7749"), new BigDecimal("-122.4194")));
        model.setDirection("D");
        model.setRoute("Route1");

        String expected = "TimUpdateModel(super=ActiveTim(activeTimId=1, timId=null, timType=null, timTypeId=null, direction=D, startTimestamp=null, startDateTime=null, endDateTime=null, expirationDateTime=null, route=Route1, clientId=null, satRecordId=null, pk=null, rsuTarget=null, rsuIndex=null, itisCodes=null, startPoint=37.774900,-122.419400, endPoint=null, projectKey=null), msgCnt=0, urlB=null, startDate_Timestamp=null, endDate_Timestamp=null, packetId=12345, timTypeName=null, timTypeDescription=null, regionId=null, regionDescription=null, laneWidth=null, anchorLat=null, anchorLong=null, regionDirection=null, directionality=null, closedPath=null, pathId=null, dataFrameId=0, frameType=null, durationTime=0, doNotUse2=0, doNotUse1=0, doNotUse4=0, doNotUse3=0, dfContent=null, url=null)";
        assertEquals(expected, model.toString());
    }

    @Test
    public void testToString_NullStartPoint() {
        TimUpdateModel model = new TimUpdateModel();
        model.setActiveTimId(2L);
        model.setPacketId("54321");
        model.setStartPoint(null);
        model.setDirection("I");
        model.setRoute("Route2");

        String expected = "TimUpdateModel(super=ActiveTim(activeTimId=2, timId=null, timType=null, timTypeId=null, direction=I, startTimestamp=null, startDateTime=null, endDateTime=null, expirationDateTime=null, route=Route2, clientId=null, satRecordId=null, pk=null, rsuTarget=null, rsuIndex=null, itisCodes=null, startPoint=null, endPoint=null, projectKey=null), msgCnt=0, urlB=null, startDate_Timestamp=null, endDate_Timestamp=null, packetId=54321, timTypeName=null, timTypeDescription=null, regionId=null, regionDescription=null, laneWidth=null, anchorLat=null, anchorLong=null, regionDirection=null, directionality=null, closedPath=null, pathId=null, dataFrameId=0, frameType=null, durationTime=0, doNotUse2=0, doNotUse1=0, doNotUse4=0, doNotUse3=0, dfContent=null, url=null)";
        assertEquals(expected, model.toString());
    }

    @Test
    public void testToString_EmptyPacketId() {
        TimUpdateModel model = new TimUpdateModel();
        model.setActiveTimId(3L);
        model.setPacketId("");
        model.setStartPoint(new Coordinate(new BigDecimal("34.0522"), new BigDecimal("-118.2437")));
        model.setDirection("B");
        model.setRoute("");

        String expected = "TimUpdateModel(super=ActiveTim(activeTimId=3, timId=null, timType=null, timTypeId=null, direction=B, startTimestamp=null, startDateTime=null, endDateTime=null, expirationDateTime=null, route=, clientId=null, satRecordId=null, pk=null, rsuTarget=null, rsuIndex=null, itisCodes=null, startPoint=34.052200,-118.243700, endPoint=null, projectKey=null), msgCnt=0, urlB=null, startDate_Timestamp=null, endDate_Timestamp=null, packetId=, timTypeName=null, timTypeDescription=null, regionId=null, regionDescription=null, laneWidth=null, anchorLat=null, anchorLong=null, regionDirection=null, directionality=null, closedPath=null, pathId=null, dataFrameId=0, frameType=null, durationTime=0, doNotUse2=0, doNotUse1=0, doNotUse4=0, doNotUse3=0, dfContent=null, url=null)";
        assertEquals(expected, model.toString());
    }
}