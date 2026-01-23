package com.trihydro.loggerkafkaconsumer.app.dataConverters;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.trihydro.library.helpers.JsonToJavaConverter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import us.dot.its.jpo.ode.model.OdeData;
import us.dot.its.jpo.ode.model.OdeLogMetadata;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.model.OdeLogMetadata.SecurityResultCode;
import us.dot.its.jpo.ode.model.OdeLogMsgMetadataLocation;
import us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy;
import us.dot.its.jpo.ode.model.OdeTimPayload;
import us.dot.its.jpo.ode.model.ReceivedMessageDetails;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.j2735.OdePosition3D;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;

@ExtendWith(MockitoExtension.class)
public class TimDataConverterTest {

    @Spy
    private JsonToJavaConverter jsonToJava = new JsonToJavaConverter();

    @InjectMocks
    private TimDataConverter uut;

    @Test
    public void processTimJson() throws IOException {

        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/rxMsg_TIM_OdeOutput.json")));

        // Arrange
        ReceivedMessageDetails receivedMessageDetails = new ReceivedMessageDetails();
        SerialId serialId;

        OdeLogMetadata odeTimMetadata = new OdeLogMetadata();
        odeTimMetadata.setRecordGeneratedBy(GeneratedBy.RSU);

        receivedMessageDetails.setRxSource(RxSource.SAT);

        odeTimMetadata.setReceivedMessageDetails(receivedMessageDetails);
        odeTimMetadata.setSchemaVersion(9);
        odeTimMetadata.setSecurityResultCode(SecurityResultCode.success);
        odeTimMetadata.setPayloadType("us.dot.its.jpo.ode.model.OdeTimPayload");

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

        // create test objects
        OdeTravelerInformationMessage tim = new OdeTravelerInformationMessage();

        OdeTimPayload odeTimPayload = new OdeTimPayload();

        OdeTravelerInformationMessage.DataFrame[] dataFrames = new OdeTravelerInformationMessage.DataFrame[1];
        OdeTravelerInformationMessage.DataFrame dataFrame = new OdeTravelerInformationMessage.DataFrame();
        OdeTravelerInformationMessage.DataFrame.Region[] regions = new OdeTravelerInformationMessage.DataFrame.Region[1];
        OdeTravelerInformationMessage.DataFrame.Region region = new OdeTravelerInformationMessage.DataFrame.Region();
        OdeTravelerInformationMessage.DataFrame.Region.Path path = new OdeTravelerInformationMessage.DataFrame.Region.Path();

        tim.setMsgCnt(1);
        tim.setPacketID("EC9C236B0000000000");
        tim.setTimeStamp("2026-01-22T21:10:48.642Z");

        OdePosition3D anchorPosition = new OdePosition3D();
        anchorPosition.setLatitude((new BigDecimal("412500807")).multiply(new BigDecimal(".0000001")));
        anchorPosition.setLongitude((new BigDecimal("-1110093847")).multiply(new BigDecimal(".0000001")));

        region.setAnchorPosition(anchorPosition);

        OdeTravelerInformationMessage.NodeXY nodeXY0 = new OdeTravelerInformationMessage.NodeXY();
        nodeXY0.setNodeLat((new BigDecimal("-2048")).multiply(new BigDecimal(".0000001")));
        nodeXY0.setNodeLong((new BigDecimal("2047")).multiply(new BigDecimal(".0000001")));
        nodeXY0.setDelta("node-LL1");

        OdeTravelerInformationMessage.NodeXY[] nodeXYArr = new OdeTravelerInformationMessage.NodeXY[2];
        nodeXYArr[0] = nodeXY0;

        OdeTravelerInformationMessage.NodeXY nodeXY1 = new OdeTravelerInformationMessage.NodeXY();
        nodeXY1.setNodeLat((new BigDecimal("-8192")).multiply(new BigDecimal(".0000001")));
        nodeXY1.setNodeLong((new BigDecimal("8191")).multiply(new BigDecimal(".0000001")));
        nodeXY1.setDelta("node-LL2");
        nodeXYArr[1] = nodeXY1;

        path.setNodes(nodeXYArr);
        region.setPath(path);
        regions[0] = region;
        dataFrame.setRegions(regions);
        dataFrames[0] = dataFrame;
        tim.setDataframes(dataFrames);

        odeTimPayload.setData(tim);

        // Act
        OdeData odeDataTest = uut.processTimJson(value);
        OdeLogMetadata odeTimMetadataTest = ((OdeLogMetadata) odeDataTest.getMetadata());
        OdeTimPayload odeTimPayloadTest = (OdeTimPayload) odeDataTest.getPayload();

        // Assert
        Assertions.assertNotNull(odeTimMetadataTest);
        Assertions.assertEquals(odeTimMetadata, odeTimMetadataTest);

        for (int i = 0; i < 2; i++) {
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath()
                            .getNodes()[i].getNodeLat(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath()
                            .getNodes()[i].getNodeLat());
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath()
                            .getNodes()[i].getNodeLong(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath()
                            .getNodes()[i].getNodeLong());
            Assertions.assertEquals(
                    getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getPath()
                            .getNodes()[i].getDelta(),
                    getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getPath()
                            .getNodes()[i].getDelta());
        }

        Assertions.assertEquals(getTim(odeTimPayload).getDataframes()[0].getRegions()[0].getAnchorPosition(),
                getTim(odeTimPayloadTest).getDataframes()[0].getRegions()[0].getAnchorPosition());
        Assertions.assertEquals(getTim(odeTimPayload).getMsgCnt(), getTim(odeTimPayloadTest).getMsgCnt());
        Assertions.assertEquals(getTim(odeTimPayload).getPacketID(), getTim(odeTimPayloadTest).getPacketID());
        Assertions.assertEquals(getTim(odeTimPayload).getUrlB(), getTim(odeTimPayloadTest).getUrlB());
    }

    @Test
    public void processTimJson_FAIL_Metadata() throws IOException {
        // Arrange
        String value = new String(Files
                .readAllBytes(Paths.get("src/test/resources/rxMsg_TIM_OdeOutput_NullMetadata.json")));

        // Act
        OdeData odeDataTest = uut.processTimJson(value);

        // Assert
        Assertions.assertNull(odeDataTest);
    }

    @Test
    public void processTimJson_FAIL_Payload() throws IOException {
        // Arrange
        String value = new String(Files
                .readAllBytes(Paths.get("src/test/resources/rxMsg_TIM_OdeOutput_NullPayload.json")));

        // Act
        OdeData odeDataTest = uut.processTimJson(value);

        // Assert
        Assertions.assertNull(odeDataTest);
    }

    @Test
    public void processTimJson_odeTimStartDateTime() throws IOException {
        // Arrange
        String value = new String(
                Files.readAllBytes(Paths.get("src/test/resources/TIM_odeTimStartDateTime.json")));

        // Act
        var data = uut.processTimJson(value);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertNotNull(data.getMetadata());
        Assertions.assertEquals("2026-01-15T20:26:17.989Z", data.getMetadata().getOdeTimStartDateTime());
    }

    @Test
    public void processTimJson_odeTimWithRsus() throws IOException {
        // Arrange
        String value = new String(Files.readAllBytes(Paths.get("src/test/resources/TIM_odeTim_Rsus.json")));

        // Act
        var data = uut.processTimJson(value);

        // Assert
        Assertions.assertNotNull(data);
        Assertions.assertNotNull(data.getMetadata());

    }

    /**
     * Helper method to get an OdeTravelerInformationMessage object given an OdeTimPayload.
     */
    private OdeTravelerInformationMessage getTim(OdeTimPayload odeTimPayload) {
        return (OdeTravelerInformationMessage) odeTimPayload.getData();
    }
}