package com.trihydro.library.helpers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.dot.its.jpo.ode.model.OdeLogMetadata;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.model.OdeLogMetadata.SecurityResultCode;
import us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy;
import us.dot.its.jpo.ode.model.OdeTimPayload;
import us.dot.its.jpo.ode.model.ReceivedMessageDetails;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.j2735.OdePosition3D;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region.Circle;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.DistanceUnits.DistanceUnitsEnum;

/**
 * Unit tests for JSON to Java Object Converters.
 */
@Slf4j
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
        SerialId serialId;

        OdeLogMetadata odeTimMetadata = new OdeLogMetadata();
        odeTimMetadata.setRecordGeneratedBy(GeneratedBy.RSU);

        receivedMessageDetails.setRxSource(RxSource.NA);

        odeTimMetadata.setReceivedMessageDetails(receivedMessageDetails);
        odeTimMetadata.setSchemaVersion(9);
        odeTimMetadata.setSecurityResultCode(SecurityResultCode.success);
        odeTimMetadata.setPayloadType("us.dot.its.jpo.ode.model.OdeMessageFramePayload");

        serialId = new SerialId("6f204bcf-5db1-4b46-be8a-35149a6b2240", 1, 0, 0, 0);
        odeTimMetadata.setSerialId(serialId);

        odeTimMetadata.setSanitized(false);
        odeTimMetadata.setOdePacketID("17e610000000000000");
        odeTimMetadata.setOdeTimStartDateTime("2026-01-22T21:10:48.642Z");
        odeTimMetadata.setRecordGeneratedAt("2026-01-22T21:10:48.642Z");
        odeTimMetadata.setAsn1("001F80A97014B901EC9C236B00000000000F775D9B0301EA73E452D1539716C99E9D555100003F0A59B080010007F8AA9979F4D3BB3A0A9266C000000854E3B2C47291F21E85EEF057980028422C1FFE0001FFFC00017FFF80000FFFFF000009FFFFF8000005FFFFFF00000007FF80007FFF00005FFFE00003FFFFC000027FFFFE0000017FFFFFC000000C523E43D138ECB11C6200C00FB0473DFB72A0E997C74007D408E5C376CD4F775D9B00");

        odeTimMetadata.setRecordType(RecordType.timMsg);
        odeTimMetadata.setLogFileName("rxMsg_TIM.bin");

        odeTimMetadata.setOdeReceivedAt("2026-01-22T21:10:48.642Z");

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
    public void TestConvertTimPayloadJsonToJava_Geometry() throws IOException, URISyntaxException {

        // create test objects
        OdeTravelerInformationMessage tim = new OdeTravelerInformationMessage();

        OdeTimPayload odeTimPayload = new OdeTimPayload();

        OdeTravelerInformationMessage.DataFrame[] dataFrames = new OdeTravelerInformationMessage.DataFrame[1];
        OdeTravelerInformationMessage.DataFrame dataFrame = new OdeTravelerInformationMessage.DataFrame();
        OdeTravelerInformationMessage.DataFrame.Region[] regions = new OdeTravelerInformationMessage.DataFrame.Region[1];
        OdeTravelerInformationMessage.DataFrame.Region region = new OdeTravelerInformationMessage.DataFrame.Region();
        OdeTravelerInformationMessage.DataFrame.Region.Geometry geometry = new OdeTravelerInformationMessage.DataFrame.Region.Geometry();

        tim.setMsgCnt(1);
        tim.setPacketID("8D442FF4020C6B1A01");
        tim.setTimeStamp("2017-10-11T21:32");

        OdePosition3D anchorPosition = new OdePosition3D();
        anchorPosition.setLatitude((BigDecimal.valueOf(411535930)).multiply(new BigDecimal(".0000001")));
        anchorPosition.setLongitude((BigDecimal.valueOf(-1046557850)).multiply(new BigDecimal(".0000001")));

        region.setAnchorPosition(anchorPosition);

        geometry.setDirection("F0F0");

        Circle circle = new Circle();
        circle.setRadius(50);
        circle.setUnits(DistanceUnitsEnum.meter);
        OdePosition3D position = new OdePosition3D(new BigDecimal("411535930"), new BigDecimal("-1046557850"),
                new BigDecimal("18240"));
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
        OdeTimPayload odeTimPayloadTest = jsonToJava.convertTimTopicJsonToJava(value);
        log.info("PACKETID: {}", getTim(odeTimPayload).getPacketID());

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
        log.info("Current relative path is: {}", s);

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
        OdeTimPayload odeTimPayload = jsonToJava.convertTimTopicJsonToJava("");
        Assertions.assertNull(odeTimPayload);
    }

    @Test
    public void TestConvertTimTopicJsonToJava_HandlesVslContentType() throws IOException {
        // Arrange
        String tim_vsl_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_vsl.json")));

        // Act
        var tim_vsl = jsonToJava.convertTimTopicJsonToJava(tim_vsl_json);

        // Assert
        Assertions.assertNotNull(tim_vsl);
        Assertions.assertEquals("advisory", getTim(tim_vsl).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "268", "12604", "8720" },
                getTim(tim_vsl).getDataframes()[0].getItems());

        // verify number of regions = 1
        Assertions.assertEquals(1, getTim(tim_vsl).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTimTopicJsonToJava_HandlesVslContentType_MultipleRegions() throws IOException {
        // Arrange
        String tim_vsl_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_vsl_MultipleRegions.json")));

        // Act
        var tim_vsl = jsonToJava.convertTimTopicJsonToJava(tim_vsl_json);

        // Assert
        Assertions.assertNotNull(tim_vsl);
        Assertions.assertEquals("advisory", getTim(tim_vsl).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "268", "12604", "8720" },
                getTim(tim_vsl).getDataframes()[0].getItems());

        // verify number of regions = 2
        Assertions.assertEquals(2, getTim(tim_vsl).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTimTopicJsonToJava_HandlesParkingContentType() throws IOException {
        // Arrange
        String tim_parking_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_parking.json")));

        // Act
        var tim_parking = jsonToJava.convertTimTopicJsonToJava(tim_parking_json);

        // Assert
        Assertions.assertNotNull(tim_parking);
        Assertions.assertEquals("advisory", getTim(tim_parking).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "4104", "11794", "345" },
                getTim(tim_parking).getDataframes()[0].getItems());

        // verify number of regions = 1
        Assertions.assertEquals(1, getTim(tim_parking).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTimTopicJsonToJava_HandlesParkingContentType_MultipleRegions() throws IOException {
        // Arrange
        String tim_parking_json = new String(Files.readAllBytes(Paths.get("src/test/resources/tim_parking_MultipleRegions.json")));

        // Act
        var tim_parking = jsonToJava.convertTimTopicJsonToJava(tim_parking_json);

        // Assert
        Assertions.assertNotNull(tim_parking);
        Assertions.assertEquals("advisory", getTim(tim_parking).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "4104", "11794", "345" },
                getTim(tim_parking).getDataframes()[0].getItems());

        // verify number of regions = 2
        Assertions.assertEquals(2, getTim(tim_parking).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTimTopicJsonToJava_HandlesConstructionContentType() throws IOException {
        // Arrange
        String tim_construction_json = new String(
                Files.readAllBytes(Paths.get("src/test/resources/tim_construction.json")));

        // Act
        var tim_construction = jsonToJava.convertTimTopicJsonToJava(tim_construction_json);

        // Assert
        Assertions.assertNotNull(tim_construction);
        Assertions.assertEquals("advisory", getTim(tim_construction).getDataframes()[0].getContent());
        Assertions.assertArrayEquals(new String[] { "1537", "12554", "8728" },
                getTim(tim_construction).getDataframes()[0].getItems());

        // verify number of regions = 1
        Assertions.assertEquals(1, getTim(tim_construction).getDataframes()[0].getRegions().length);
    }

    @Test
    public void TestConvertTimTopicJsonToJava_HandlesConstructionContentType_MultipleRegions() throws IOException {
        // Arrange
        String tim_construction_json = new String(
                Files.readAllBytes(Paths.get("src/test/resources/tim_construction_MultipleRegions.json")));

        // Act
        var tim_construction = jsonToJava.convertTimTopicJsonToJava(tim_construction_json);

        // Assert
        Assertions.assertNotNull(tim_construction);
        Assertions.assertEquals("advisory", getTim(tim_construction).getDataframes()[0].getContent());
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