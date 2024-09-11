package com.trihydro.library.helpers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.dot.its.jpo.ode.model.OdeBsmMetadata;
import us.dot.its.jpo.ode.model.OdeBsmMetadata.BsmSource;
import us.dot.its.jpo.ode.model.OdeBsmPayload;
import us.dot.its.jpo.ode.model.OdeDriverAlertPayload;
import us.dot.its.jpo.ode.model.OdeLogMetadata;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.model.OdeLogMetadata.SecurityResultCode;
import us.dot.its.jpo.ode.model.OdeLogMsgMetadataLocation;
import us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy;
import us.dot.its.jpo.ode.model.OdeTimPayload;
import us.dot.its.jpo.ode.model.ReceivedMessageDetails;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.j2735.J2735SupplementalVehicleExtensions;
import us.dot.its.jpo.ode.plugin.j2735.J2735TransmissionState;
import us.dot.its.jpo.ode.plugin.j2735.J2735VehicleSafetyExtensions;
import us.dot.its.jpo.ode.plugin.j2735.OdePosition3D;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region.Circle;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.DistanceUnits.DistanceUnitsEnum;

/**
 * Unit tests for JSON to Java Object Converters.
 */
public class JsonToJavaConverterTest {

    private JsonToJavaConverter jsonToJava;

    @BeforeEach
    public void setup() {
        jsonToJava = new JsonToJavaConverter();
    }

    @Test
    public void TestConvertTimMetadataJsonToJava() throws IOException {

        // create test objects
        ReceivedMessageDetails receivedMessageDetails = new ReceivedMessageDetails();
        OdeLogMsgMetadataLocation locationData = new OdeLogMsgMetadataLocation();
        SerialId serialId;

        OdeLogMetadata odeTimMetadata = new OdeLogMetadata();
        odeTimMetadata.setRecordGeneratedBy(GeneratedBy.OBU);

        locationData.setElevation("1515");
        locationData.setHeading("0.0000");
        locationData.setLatitude("40.4739533");
        locationData.setLongitude("-104.9689995");
        locationData.setSpeed("0.14");

        receivedMessageDetails.setLocationData(locationData);
        receivedMessageDetails.setRxSource(RxSource.SAT);

        odeTimMetadata.setReceivedMessageDetails(receivedMessageDetails);
        odeTimMetadata.setSchemaVersion(3);
        odeTimMetadata.setSecurityResultCode(SecurityResultCode.unknown);
        odeTimMetadata.setPayloadType("us.dot.its.jpo.ode.model.OdeTimPayload");

        serialId = new SerialId("f212c298-4021-412a-b7c6-1fdb64a6a227", 1, 4, 2, 0);
        odeTimMetadata.setSerialId(serialId);

        odeTimMetadata.setSanitized(false);
        odeTimMetadata.setRecordGeneratedAt("2017-09-05T20:23:39.194Z[UTC]");

        odeTimMetadata.setRecordType(RecordType.rxMsg);
        odeTimMetadata.setLogFileName("rxMsg_TIM.bin");

        odeTimMetadata.setOdeReceivedAt("2017-11-09T13:33:34.039Z[UTC]");

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/rxMsg_TIM_OdeOutput.json")));

        OdeLogMetadata odeTimMetadataTest = jsonToJava.convertTimMetadataJsonToJava(value);

        Assertions.assertNotNull(odeTimMetadataTest);
        Assertions.assertEquals(odeTimMetadata, odeTimMetadataTest);
        Assertions.assertEquals(odeTimMetadata.getSecurityResultCode(), odeTimMetadataTest.getSecurityResultCode());
    }

    @Test
    public void TestConvertTimMetadataNullException() throws IOException {
        OdeLogMetadata odeTimMetadataTest = jsonToJava.convertTimMetadataJsonToJava("");
        Assertions.assertNull(odeTimMetadataTest);
    }

    @Test
    public void TestConvertTimPayloadJsonToJava_Path() throws IOException, URISyntaxException {

        // create test objects
        OdeTravelerInformationMessage tim = new OdeTravelerInformationMessage();

        OdeTimPayload odeTimPayload = new OdeTimPayload();

        OdeTravelerInformationMessage.DataFrame[] dataFrames = new OdeTravelerInformationMessage.DataFrame[1];
        OdeTravelerInformationMessage.DataFrame dataFrame = new OdeTravelerInformationMessage.DataFrame();
        OdeTravelerInformationMessage.DataFrame.Region[] regions = new OdeTravelerInformationMessage.DataFrame.Region[1];
        OdeTravelerInformationMessage.DataFrame.Region region = new OdeTravelerInformationMessage.DataFrame.Region();
        OdeTravelerInformationMessage.DataFrame.Region.Path path = new OdeTravelerInformationMessage.DataFrame.Region.Path();

        tim.setMsgCnt(0);
        tim.setPacketID("EC9C236B0000000000");
        tim.setTimeStamp("2017-10-11T21:32");

        OdePosition3D anchorPosition = new OdePosition3D();
        anchorPosition.setLatitude((BigDecimal.valueOf(263056840)).multiply(new BigDecimal(".0000001")));
        anchorPosition.setLongitude((BigDecimal.valueOf(-801481510)).multiply(new BigDecimal(".0000001")));
        // anchorPosition.setElevation(new BigDecimal(20));

        region.setAnchorPosition(anchorPosition);

        OdeTravelerInformationMessage.NodeXY nodeXY0 = new OdeTravelerInformationMessage.NodeXY();
        nodeXY0.setNodeLat((new BigDecimal("405744807")).multiply(new BigDecimal(".0000001")));
        nodeXY0.setNodeLong((new BigDecimal("-1050524251")).multiply(new BigDecimal(".0000001")));
        nodeXY0.setDelta("node-LatLon");

        OdeTravelerInformationMessage.NodeXY[] nodeXYArr = new OdeTravelerInformationMessage.NodeXY[2];
        nodeXYArr[0] = nodeXY0;

        OdeTravelerInformationMessage.NodeXY nodeXY1 = new OdeTravelerInformationMessage.NodeXY();
        nodeXY1.setNodeLat((new BigDecimal("405735393")).multiply(new BigDecimal(".0000001")));
        nodeXY1.setNodeLong((new BigDecimal("-1050500237")).multiply(new BigDecimal(".0000001")));
        nodeXY1.setDelta("node-LatLon");
        nodeXYArr[1] = nodeXY1;

        path.setNodes(nodeXYArr);
        region.setPath(path);
        regions[0] = region;
        dataFrame.setRegions(regions);
        dataFrames[0] = dataFrame;
        tim.setDataframes(dataFrames);

        odeTimPayload.setData(tim);

        String value = new String(
                Files.readAllBytes(Paths.get(getClass().getResource("/rxMsg_TIM_OdeOutput.json").toURI())));
        OdeTimPayload odeTimPayloadTest = jsonToJava.convertTimPayloadJsonToJava(value);
        System.out.println("PACKETID: " + getTim(odeTimPayload).getPacketID());
        for (int i = 0; i < 2; i++) {
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getNodeLat(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getNodeLat());
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getNodeLong(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath().getNodes()[i]
                            .getNodeLong());
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getDelta(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getDelta());
        }

        Assertions.assertEquals(getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getAnchorPosition(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getAnchorPosition());
        Assertions.assertEquals(getTim(odeTimPayload).getMsgCnt(), getTim(odeTimPayloadTest).getMsgCnt());

        Assertions.assertEquals(getTim(odeTimPayload).getPacketID(), getTim(odeTimPayloadTest).getPacketID());
        Assertions.assertEquals(getTim(odeTimPayload).getUrlB(), getTim(odeTimPayloadTest).getUrlB());

        // verify number of regions = 1
        Assertions.assertEquals(1, getTim(odeTimPayloadTest).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTimPayloadJsonToJava_Path_MultipleRegions() throws IOException, URISyntaxException {

        // create test objects
        OdeTravelerInformationMessage tim = new OdeTravelerInformationMessage();

        OdeTimPayload odeTimPayload = new OdeTimPayload();

        OdeTravelerInformationMessage.DataFrame[] dataFrames = new OdeTravelerInformationMessage.DataFrame[1];
        OdeTravelerInformationMessage.DataFrame dataFrame = new OdeTravelerInformationMessage.DataFrame();
        OdeTravelerInformationMessage.DataFrame.Region[] regions = new OdeTravelerInformationMessage.DataFrame.Region[1];
        OdeTravelerInformationMessage.DataFrame.Region region = new OdeTravelerInformationMessage.DataFrame.Region();
        OdeTravelerInformationMessage.DataFrame.Region.Path path = new OdeTravelerInformationMessage.DataFrame.Region.Path();

        tim.setMsgCnt(0);
        tim.setPacketID("EC9C236B0000000000");
        tim.setTimeStamp("2017-10-11T21:32");

        OdePosition3D anchorPosition = new OdePosition3D();
        anchorPosition.setLatitude((BigDecimal.valueOf(263056840)).multiply(new BigDecimal(".0000001")));
        anchorPosition.setLongitude((BigDecimal.valueOf(-801481510)).multiply(new BigDecimal(".0000001")));
        // anchorPosition.setElevation(new BigDecimal(20));

        region.setAnchorPosition(anchorPosition);

        OdeTravelerInformationMessage.NodeXY nodeXY0 = new OdeTravelerInformationMessage.NodeXY();
        nodeXY0.setNodeLat((new BigDecimal("405744807")).multiply(new BigDecimal(".0000001")));
        nodeXY0.setNodeLong((new BigDecimal("-1050524251")).multiply(new BigDecimal(".0000001")));
        nodeXY0.setDelta("node-LatLon");

        OdeTravelerInformationMessage.NodeXY[] nodeXYArr = new OdeTravelerInformationMessage.NodeXY[2];
        nodeXYArr[0] = nodeXY0;

        OdeTravelerInformationMessage.NodeXY nodeXY1 = new OdeTravelerInformationMessage.NodeXY();
        nodeXY1.setNodeLat((new BigDecimal("405735393")).multiply(new BigDecimal(".0000001")));
        nodeXY1.setNodeLong((new BigDecimal("-1050500237")).multiply(new BigDecimal(".0000001")));
        nodeXY1.setDelta("node-LatLon");
        nodeXYArr[1] = nodeXY1;

        path.setNodes(nodeXYArr);
        region.setPath(path);
        regions[0] = region;
        dataFrame.setRegions(regions);
        dataFrames[0] = dataFrame;
        tim.setDataframes(dataFrames);

        odeTimPayload.setData(tim);

        String value = new String(
                Files.readAllBytes(Paths.get(getClass().getResource("/rxMsg_TIM_OdeOutput_MultipleRegions.json").toURI())));
        OdeTimPayload odeTimPayloadTest = jsonToJava.convertTimPayloadJsonToJava(value);
        System.out.println("PACKETID: " + getTim(odeTimPayload).getPacketID());
        for (int i = 0; i < 2; i++) {
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getNodeLat(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getNodeLat());
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getNodeLong(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath().getNodes()[i]
                            .getNodeLong());
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getDelta(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath().getNodes()[i].getDelta());
        }

        Assertions.assertEquals(getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getAnchorPosition(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getAnchorPosition());
        Assertions.assertEquals(getTim(odeTimPayload).getMsgCnt(), getTim(odeTimPayloadTest).getMsgCnt());

        Assertions.assertEquals(getTim(odeTimPayload).getPacketID(), getTim(odeTimPayloadTest).getPacketID());
        Assertions.assertEquals(getTim(odeTimPayload).getUrlB(), getTim(odeTimPayloadTest).getUrlB());

        // verify number of regions = 2
        Assertions.assertEquals(2, getTim(odeTimPayloadTest).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTimPayloadJsonToJava_SpeedLimit() throws IOException, URISyntaxException {
        // Arrange
        String value = new String(
                Files.readAllBytes(Paths.get(getClass().getResource("/rxMsg_TIM_SpeedLimit.json").toURI())));

        // Act
        OdeTimPayload odeTimPayloadTest = jsonToJava.convertTimPayloadJsonToJava(value);

        // Assert
        Assertions.assertNotNull(odeTimPayloadTest);
        Assertions.assertTrue(getTim(odeTimPayloadTest).getDataframes()[0].getItems().length > 0);
        Assertions.assertEquals("speedLimit", getTim(odeTimPayloadTest).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "13609", "268", "12554", "8720" },
                getTim(odeTimPayloadTest).getDataframes()[0].getItems());
    }

    @Test
    public void TestConvertTimPayloadJsonToJava_Geometry() throws IOException, URISyntaxException {

        // create test objects
        OdeTravelerInformationMessage tim = new OdeTravelerInformationMessage();

        OdeTimPayload odeTimPayload = new OdeTimPayload();

        OdeTravelerInformationMessage.DataFrame[] dataFrames = new OdeTravelerInformationMessage.DataFrame[1];
        OdeTravelerInformationMessage.DataFrame dataFrame = new OdeTravelerInformationMessage.DataFrame();
        OdeTravelerInformationMessage.DataFrame.Region[] regions = new OdeTravelerInformationMessage.DataFrame.Region[1];
        OdeTravelerInformationMessage.DataFrame.Region region = new OdeTravelerInformationMessage.DataFrame.Region();
        OdeTravelerInformationMessage.DataFrame.Region.Geometry geometry = new OdeTravelerInformationMessage.DataFrame.Region.Geometry();

        tim.setMsgCnt(0);
        tim.setPacketID("EC9C236B0000000000");
        tim.setTimeStamp("2017-10-11T21:32");

        OdePosition3D anchorPosition = new OdePosition3D();
        anchorPosition.setLatitude((BigDecimal.valueOf(263056840)).multiply(new BigDecimal(".0000001")));
        anchorPosition.setLongitude((BigDecimal.valueOf(-801481510)).multiply(new BigDecimal(".0000001")));
        // anchorPosition.setElevation(new BigDecimal(20));

        region.setAnchorPosition(anchorPosition);

        geometry.setDirection("1010101010101010");
        geometry.setExtent(1);// this is an enum
        geometry.setLaneWidth(BigDecimal.valueOf(33));

        Circle circle = new Circle();
        circle.setRadius(15);
        circle.setUnits(DistanceUnitsEnum.mile);
        OdePosition3D position = new OdePosition3D(new BigDecimal("41.678473"), new BigDecimal("-108.782775"),
                new BigDecimal("917.1432"));
        circle.setCenter(position);
        geometry.setCircle(circle);

        region.setGeometry(geometry);

        regions[0] = region;
        dataFrame.setRegions(regions);
        dataFrames[0] = dataFrame;
        tim.setDataframes(dataFrames);

        odeTimPayload.setData(tim);

        String value = new String(
                Files.readAllBytes(Paths.get(getClass().getResource("/rxMsg_TIM_OdeOutput_Geometry.json").toURI())));
        OdeTimPayload odeTimPayloadTest = jsonToJava.convertTimPayloadJsonToJava(value);
        System.out.println("PACKETID: " + getTim(odeTimPayload).getPacketID());

        // test geometry properties
        // direction
        Assertions.assertEquals(getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getDirection(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getDirection());
        // extent
        Assertions.assertEquals(getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getExtent(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getExtent());
        // laneWidth
        Assertions.assertEquals(getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getLaneWidth(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getLaneWidth());
        // circle/radius
        Assertions.assertEquals(
                getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getRadius(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getRadius());
        // circle/units
        Assertions.assertEquals(
                getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getUnits(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getUnits());
        // circle/position/latitude
        Assertions.assertEquals(
                getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getCenter()
                        .getLatitude(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getCenter()
                        .getLatitude());
        // circle/position/longitude
        Assertions.assertEquals(
                getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getCenter()
                        .getLongitude(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getCenter()
                        .getLongitude());
        // circle/position/elevation
        Assertions.assertEquals(
                getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getCenter()
                        .getElevation(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getGeometry().getCircle().getCenter()
                        .getElevation());

        Assertions.assertEquals(getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getAnchorPosition(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getAnchorPosition());
        Assertions.assertEquals(getTim(odeTimPayload).getMsgCnt(), getTim(odeTimPayloadTest).getMsgCnt());

        Assertions.assertEquals(getTim(odeTimPayload).getPacketID(), getTim(odeTimPayloadTest).getPacketID());
        Assertions.assertEquals(getTim(odeTimPayload).getUrlB(), getTim(odeTimPayloadTest).getUrlB());
    }

    @Test
    public void convertBroadcastTimPayloadJsonToJava() throws IOException {

        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current relative path is: " + s);

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/broadcastTim_OdeOutput.json")));
        // String value = new
        // String(Files.readAllBytes(Paths.get("broadcastTim_OdeOutput.json")));
        OdeTravelerInformationMessage timTest = jsonToJava.convertBroadcastTimPayloadJsonToJava(value);

        Assertions.assertEquals(1, timTest.getMsgCnt());
        Assertions.assertEquals("2018-03-15T21:18:46.719-07:00", timTest.getTimeStamp());
        Assertions.assertEquals("17e610000000000000", timTest.getPacketID());
        Assertions.assertEquals("null", timTest.getUrlB());
        Assertions.assertEquals("null", timTest.getUrlB());
    }

    @Test
    public void TestConvertTimPayloadNullException() throws IOException {
        OdeTimPayload odeTimPayload = jsonToJava.convertTimPayloadJsonToJava("");
        Assertions.assertNull(odeTimPayload);
    }

    @Test
    public void TestConvertBsmMetadataJsonToJava() throws IOException {

        // create test objects
        SerialId serialId;

        OdeBsmMetadata odeBsmMetadata = new OdeBsmMetadata();
        odeBsmMetadata.setRecordGeneratedBy(GeneratedBy.OBU);

        odeBsmMetadata.setSchemaVersion(3);
        odeBsmMetadata.setSecurityResultCode(SecurityResultCode.unknown);
        odeBsmMetadata.setPayloadType("us.dot.its.jpo.ode.model.OdeBsmPayload");

        serialId = new SerialId("c8babc6e-ec8a-4232-a151-1dcd27c323ef", 1, 4070, 2, 0);
        odeBsmMetadata.setSerialId(serialId);

        odeBsmMetadata.setSanitized(false);
        odeBsmMetadata.setRecordGeneratedAt("2017-09-08T14:51:19.294Z[UTC]");

        odeBsmMetadata.setRecordType(RecordType.bsmLogDuringEvent);
        odeBsmMetadata.setLogFileName("bsmLogDuringEvent_OdeOutput.json");
        odeBsmMetadata.setBsmSource(BsmSource.RV);

        odeBsmMetadata.setOdeReceivedAt("2017-11-22T18:37:29.31Z[UTC]");

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/bsmLogDuringEvent_OdeOutput.json")));

        OdeBsmMetadata odeBsmMetadataTest = jsonToJava.convertBsmMetadataJsonToJava(value);

        Assertions.assertNotNull(odeBsmMetadataTest);
        Assertions.assertEquals(odeBsmMetadata, odeBsmMetadataTest);
        Assertions.assertEquals(odeBsmMetadata.getSecurityResultCode(), odeBsmMetadataTest.getSecurityResultCode());
        Assertions.assertEquals(odeBsmMetadata.getBsmSource(), odeBsmMetadataTest.getBsmSource());
    }

    @Test
    public void TestConvertBsmMetadataNullException() throws IOException {
        OdeBsmMetadata odeBsmMetadataTest = jsonToJava.convertBsmMetadataJsonToJava("");
        Assertions.assertNull(odeBsmMetadataTest);
    }

    @Test
    public void TestConvertBsmPayloadJsonToJava() throws IOException {

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/bsmLogDuringEvent_OdeOutput.json")));

        OdeBsmPayload odeBsmPayloadTest = jsonToJava.convertBsmPayloadJsonToJava(value);

        Assertions.assertNotNull(odeBsmPayloadTest);
        Assertions.assertEquals(Integer.valueOf(11), odeBsmPayloadTest.getBsm().getCoreData().getMsgCnt());
        Assertions.assertEquals("738B0000", odeBsmPayloadTest.getBsm().getCoreData().getId());
        Assertions.assertEquals(Integer.valueOf(19400), odeBsmPayloadTest.getBsm().getCoreData().getSecMark());
        Assertions.assertEquals("40.4740003",
                odeBsmPayloadTest.getBsm().getCoreData().getPosition().getLatitude().toString());
        Assertions.assertEquals("-104.9691846",
                odeBsmPayloadTest.getBsm().getCoreData().getPosition().getLongitude().toString());
        Assertions.assertEquals(BigDecimal.valueOf(1489),
                odeBsmPayloadTest.getBsm().getCoreData().getPosition().getElevation());
        Assertions.assertEquals(BigDecimal.valueOf(0),
                odeBsmPayloadTest.getBsm().getCoreData().getAccelSet().getAccelYaw());
        Assertions.assertEquals("12.7",
                odeBsmPayloadTest.getBsm().getCoreData().getAccuracy().getSemiMajor().toString());
        Assertions.assertEquals("12.7",
                odeBsmPayloadTest.getBsm().getCoreData().getAccuracy().getSemiMinor().toString());
        Assertions.assertEquals(J2735TransmissionState.NEUTRAL,
                odeBsmPayloadTest.getBsm().getCoreData().getTransmission());
        Assertions.assertEquals("0.1", odeBsmPayloadTest.getBsm().getCoreData().getSpeed().toString());
        Assertions.assertEquals("19.9125", odeBsmPayloadTest.getBsm().getCoreData().getHeading().toString());
        Assertions.assertEquals(false,
                odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getWheelBrakes().get("leftFront"));
        Assertions.assertEquals(false,
                odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getWheelBrakes().get("rightFront"));
        Assertions.assertEquals(true,
                odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getWheelBrakes().get("unavailable"));
        Assertions.assertEquals(false,
                odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getWheelBrakes().get("leftRear"));
        Assertions.assertEquals(false,
                odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getWheelBrakes().get("rightRear"));
        Assertions.assertEquals("unavailable", odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getTraction());
        Assertions.assertEquals("unavailable", odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getAbs());
        Assertions.assertEquals("unavailable", odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getScs());
        Assertions.assertEquals("unavailable", odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getBrakeBoost());
        Assertions.assertEquals("unavailable", odeBsmPayloadTest.getBsm().getCoreData().getBrakes().getAuxBrakes());
        Assertions.assertEquals(2, odeBsmPayloadTest.getBsm().getPartII().size());
    }

    @Test
    public void TestConvertJ2735VehicleSafetyExtensionsJsonToJava() throws IOException {
        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/bsmLogDuringEvent_OdeOutput.json")));

        J2735VehicleSafetyExtensions vse = jsonToJava.convertJ2735VehicleSafetyExtensionsJsonToJava(value, 0);
        Assertions.assertEquals("-9.2", vse.getPathHistory().getCrumbData().get(0).getElevationOffset().toString());
        Assertions.assertEquals("0.0000322", vse.getPathHistory().getCrumbData().get(0).getLatOffset().toString());
        Assertions.assertEquals("0.0001445", vse.getPathHistory().getCrumbData().get(0).getLonOffset().toString());
        Assertions.assertEquals("33.38", vse.getPathHistory().getCrumbData().get(0).getTimeOffset().toString());

        Assertions.assertEquals("1", vse.getPathHistory().getCrumbData().get(1).getElevationOffset().toString());
        Assertions.assertEquals("-0.0000097", vse.getPathHistory().getCrumbData().get(1).getLatOffset().toString());
        Assertions.assertEquals("0.0000609", vse.getPathHistory().getCrumbData().get(1).getLonOffset().toString());
        Assertions.assertEquals("225.6", vse.getPathHistory().getCrumbData().get(1).getTimeOffset().toString());

        Assertions.assertEquals("50", vse.getPathPrediction().getConfidence().toString());
        Assertions.assertEquals("0", vse.getPathPrediction().getRadiusOfCurve().toString());
    }

    @Test
    public void TestJ2735SupplementalVehicleExtensionsJsonToJava() throws IOException {
        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/bsmLogDuringEvent_OdeOutput.json")));

        J2735SupplementalVehicleExtensions suve = jsonToJava.convertJ2735SupplementalVehicleExtensionsJsonToJava(value,
                1);
        Assertions.assertEquals("unknownFuel", suve.getClassDetails().getFuelType().toString());
        Assertions.assertEquals("none", suve.getClassDetails().getHpmsType().toString());
        Assertions.assertEquals(Integer.valueOf(0), suve.getClassDetails().getKeyType());
        Assertions.assertEquals("basicVehicle", suve.getClassDetails().getRole().toString());
    }

    @Test
    public void TestGetPart2Node() throws IOException {
        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/bsmLogDuringEvent_OdeOutput.json")));
        String testNode = "{\"classDetails\":{\"fuelType\":\"unknownFuel\",\"hpmsType\":\"none\",\"keyType\":0,\"regional\":[],\"role\":\"basicVehicle\"},\"weatherProbe\":{},\"regional\":[]}";
        JsonNode part2Node = jsonToJava.getPart2Node(value, 1);
        Assertions.assertEquals(testNode, part2Node.toString());
    }

    @Test
    public void TestConvertDriverAlertMetadataJsonToJava() throws IOException {

        // create test objects
        ReceivedMessageDetails receivedMessageDetails = new ReceivedMessageDetails();
        OdeLogMsgMetadataLocation locationData = new OdeLogMsgMetadataLocation();
        SerialId serialId;

        OdeLogMetadata odeDriverAlertMetadata = new OdeLogMetadata();
        odeDriverAlertMetadata.setRecordGeneratedBy(GeneratedBy.OBU);

        locationData.setElevation("1486.0");
        locationData.setHeading("331.9000");
        locationData.setLatitude("40.4739771");
        locationData.setLongitude("-104.9691666");
        locationData.setSpeed("1.04");

        receivedMessageDetails.setLocationData(locationData);
        odeDriverAlertMetadata.setReceivedMessageDetails(receivedMessageDetails);

        odeDriverAlertMetadata.setSchemaVersion(3);
        odeDriverAlertMetadata.setSecurityResultCode(SecurityResultCode.unknown);
        odeDriverAlertMetadata.setPayloadType("us.dot.its.jpo.ode.model.OdeDriverAlertPayload");

        serialId = new SerialId("408f1086-d028-4919-afc7-50ec097ddba9", 1, 12, 2, 0);
        odeDriverAlertMetadata.setSerialId(serialId);

        odeDriverAlertMetadata.setSanitized(false);
        odeDriverAlertMetadata.setRecordGeneratedAt("2017-10-04T21:03:56.614Z[UTC]");

        odeDriverAlertMetadata.setRecordType(RecordType.driverAlert);
        odeDriverAlertMetadata.setRecordGeneratedBy(GeneratedBy.OBU);
        odeDriverAlertMetadata.setLogFileName("driverAlert_OdeOutput.csv");

        odeDriverAlertMetadata.setOdeReceivedAt("2017-11-30T21:37:24.266Z[UTC]");

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/driverAlert_OdeOutput.json")));
        OdeLogMetadata odeDriverAlertMetadataTest = jsonToJava.convertDriverAlertMetadataJsonToJava(value);

        Assertions.assertNotNull(odeDriverAlertMetadataTest);
        Assertions.assertEquals(odeDriverAlertMetadata, odeDriverAlertMetadata);
        Assertions.assertEquals(odeDriverAlertMetadata.getSecurityResultCode(),
                odeDriverAlertMetadata.getSecurityResultCode());
    }

    @Test
    public void TestConvertDriverAlertPayloadJsonToJava() throws IOException {

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/driverAlert_OdeOutput.json")));
        OdeDriverAlertPayload odeDriverAlertPayloadTest = jsonToJava.convertDriverAlertPayloadJsonToJava(value);

        Assertions.assertNotNull(odeDriverAlertPayloadTest);
        Assertions.assertEquals("ICW", odeDriverAlertPayloadTest.getAlert());
    }

    @Test
    public void TestConvertTmcTimTopicJsonToJava_HandlesVslContentType() throws IOException {
        // Arrange
        String tim_vsl_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_vsl.json")));

        // Act
        var tim_vsl = jsonToJava.convertTmcTimTopicJsonToJava(tim_vsl_json);

        // Assert
        Assertions.assertNotNull(tim_vsl);
        Assertions.assertEquals("speedLimit", getTim(tim_vsl).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "268", "12604", "8720" },
                getTim(tim_vsl).getDataframes()[0].getItems());
        
        // verify number of regions = 1
        Assertions.assertEquals(1, getTim(tim_vsl).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTmcTimTopicJsonToJava_HandlesVslContentType_MultipleRegions() throws IOException {
        // Arrange
        String tim_vsl_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_vsl_MultipleRegions.json")));

        // Act
        var tim_vsl = jsonToJava.convertTmcTimTopicJsonToJava(tim_vsl_json);

        // Assert
        Assertions.assertNotNull(tim_vsl);
        Assertions.assertEquals("speedLimit", getTim(tim_vsl).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "268", "12604", "8720" },
                getTim(tim_vsl).getDataframes()[0].getItems());
        
        // verify number of regions = 2
        Assertions.assertEquals(2, getTim(tim_vsl).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTmcTimTopicJsonToJava_HandlesParkingContentType() throws IOException {
        // Arrange
        String tim_parking_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_parking.json")));

        // Act
        var tim_parking = jsonToJava.convertTmcTimTopicJsonToJava(tim_parking_json);

        // Assert
        Assertions.assertNotNull(tim_parking);
        Assertions.assertEquals("exitService", getTim(tim_parking).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "4104", "11794", "345" },
                getTim(tim_parking).getDataframes()[0].getItems());
        
        // verify number of regions = 1
        Assertions.assertEquals(1, getTim(tim_parking).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTmcTimTopicJsonToJava_HandlesParkingContentType_MultipleRegions() throws IOException {
        // Arrange
        String tim_parking_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_parking_MultipleRegions.json")));

        // Act
        var tim_parking = jsonToJava.convertTmcTimTopicJsonToJava(tim_parking_json);

        // Assert
        Assertions.assertNotNull(tim_parking);
        Assertions.assertEquals("exitService", getTim(tim_parking).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "4104", "11794", "345" },
                getTim(tim_parking).getDataframes()[0].getItems());
        
        // verify number of regions = 2
        Assertions.assertEquals(2, getTim(tim_parking).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTmcTimTopicJsonToJava_HandlesConstructionContentType() throws IOException {
        // Arrange
        String tim_construction_json = new String(
                Files.readAllBytes(Paths.get("src/test/resources/tim_construction.json")));

        // Act
        var tim_construction = jsonToJava.convertTmcTimTopicJsonToJava(tim_construction_json);

        // Assert
        Assertions.assertNotNull(tim_construction);
        Assertions.assertEquals("workZone", getTim(tim_construction).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "1537", "12554", "8728" },
                getTim(tim_construction).getDataframes()[0].getItems());
        
        // verify number of regions = 1
        Assertions.assertEquals(1, getTim(tim_construction).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTmcTimTopicJsonToJava_HandlesConstructionContentType_MultipleRegions() throws IOException {
        // Arrange
        String tim_construction_json = new String(
                Files.readAllBytes(Paths.get("src/test/resources/tim_construction_MultipleRegions.json")));

        // Act
        var tim_construction = jsonToJava.convertTmcTimTopicJsonToJava(tim_construction_json);

        // Assert
        Assertions.assertNotNull(tim_construction);
        Assertions.assertEquals("workZone", getTim(tim_construction).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "1537", "12554", "8728" },
                getTim(tim_construction).getDataframes()[0].getItems());
        
        // verify number of regions = 2
        Assertions.assertEquals(2, getTim(tim_construction).getDataframes()[0].getRegions().length);
    }


    /**
     * Helper method to get an OdeTravelerInformationMessage object given an OdeTimPayload.
     */
    private OdeTravelerInformationMessage getTim(OdeTimPayload odeTimPayload) {
        return (OdeTravelerInformationMessage) odeTimPayload.getData();
    }
}
