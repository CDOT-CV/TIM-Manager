package com.trihydro.odewrapper.factory;

import com.trihydro.library.model.Coordinate;
import com.trihydro.library.model.ItisCode;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.WydotTim;
import com.trihydro.library.service.MilepostService;
import com.trihydro.odewrapper.helpers.SetItisCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BufferTimFactoryTest implements BufferTimFactory {
    @Mock
    private MilepostService milepostService;
    @Mock
    private SetItisCodes setItisCodes;
    private WydotTim wydotTim;

    @BeforeEach
    void setUp() {
        milepostService = mock(MilepostService.class);

        wydotTim = new WydotTim();
        wydotTim.setStartPoint(new Coordinate());
        wydotTim.setEndPoint(new Coordinate(BigDecimal.valueOf(1), BigDecimal.valueOf(2)));
        var wydotTimItisCodes = new ArrayList<String>();
        wydotTimItisCodes.add("1309");
        wydotTim.setItisCodes(wydotTimItisCodes);
        wydotTim.setClientId("testclientid");
        wydotTim.setRoute("I-80");
        wydotTim.setDirection("I");

        List<ItisCode> itisCodes = new ArrayList<>();
        ItisCode ic = new ItisCode();
        ic.setCategoryId(-1);
        ic.setDescription("description");
        ic.setItisCode(-2);
        ic.setItisCodeId(-3);
        itisCodes.add(ic);

        lenient().doReturn(wydotTimItisCodes).when(setItisCodes).setItisCodes(any());
        lenient().doReturn(itisCodes).when(setItisCodes).getItisCodes();
    }

    private List<Milepost> getMileposts() {
        List<Milepost> mileposts = new ArrayList<>();

        var mp = new Milepost();
        mp = new Milepost();
        mp.setLatitude(BigDecimal.valueOf(200));
        mp.setLongitude(BigDecimal.valueOf(300));
        mileposts.add(mp);


        mp = new Milepost();
        mp.setLatitude(BigDecimal.valueOf(300));
        mp.setLongitude(BigDecimal.valueOf(300));
        mileposts.add(mp);

        mp = new Milepost();
        mp.setLatitude(BigDecimal.valueOf(400));
        mp.setLongitude(BigDecimal.valueOf(400));
        mileposts.add(mp);

        mp = new Milepost();
        mp.setLatitude(BigDecimal.valueOf(500));
        mp.setLongitude(BigDecimal.valueOf(500));
        mileposts.add(mp);
        return mileposts;
    }

    @Test
    void testMilepostToGeometry() {
        // Arrange
        List<Milepost> mileposts = getMileposts();

        // Act
        List<Coordinate> geometry = milepostToGeometry(mileposts);

        // Assert
        assertEquals(4, geometry.size());
        assertEquals(BigDecimal.valueOf(200), geometry.get(0).getLatitude());
        assertEquals(BigDecimal.valueOf(300), geometry.get(0).getLongitude());
    }

    @Test
    void testBuildTimsFromItisCodes_withNonBufferCode() {
        // Arrange
        List<Integer> bufferCodes = List.of(7364);

        // Act
        List<WydotTim> tims = buildTimsFromItisCodes(wydotTim, bufferCodes, false);

        // Assert
        assertEquals(1, tims.size());
        assertEquals("testclientid-I-1309", tims.get(0).getClientId());
    }

    @Test
    void testBuildTimsFromItisCodes_withBufferCode() {
        // Arrange
        var wydotTimItisCodes = new ArrayList<String>();
        wydotTimItisCodes.add("1309 7364");
        wydotTim.setItisCodes(wydotTimItisCodes);
        List<Integer> bufferCodes = List.of(7364);

        // Act
        List<WydotTim> tims = buildTimsFromItisCodes(wydotTim, bufferCodes, true);

        // Assert
        assertEquals(1, tims.size());
        assertEquals("testclientid-I-1309-7364", tims.get(0).getClientId());
    }


    @Test
    void testBuildTimsFromItisCodes_excludesBufferCodeWhenNotisBufferAndBufferTim() {
        // Arrange
        var wydotTimItisCodes = new ArrayList<String>();
        wydotTimItisCodes.add("1309 7364");
        wydotTim.setItisCodes(wydotTimItisCodes);
        List<Integer> bufferCodes = List.of(7364); // different from last code 7342

        // Act
        List<WydotTim> tims = buildTimsFromItisCodes(wydotTim, bufferCodes, false);

        // Assert
        assertTrue(tims.isEmpty());
    }

    @Test
    void testBuildTimsFromItisCodes_excludesBufferCodeWhenisBufferAndNotBufferTim() {
        // Arrange
        List<Integer> bufferCodes = List.of(7364); // different from last code 7342

        // Act
        List<WydotTim> tims = buildTimsFromItisCodes(wydotTim, bufferCodes, true);

        // Assert
        assertTrue(tims.isEmpty());
    }

    @Test
    void testMakeIncreasingTims_callsMakeOneWayWithI() {
        // Arrange
        List<Integer> bufferCodes = List.of(7364); // unrelated

        // Act
        List<WydotTim> result = makeIncreasingTims(wydotTim, bufferCodes, milepostService);

        // Assert
        for (WydotTim tim : result) {
            assertEquals("I", tim.getDirection());
        }
    }

    @Test
    void testMakeDecreasingTims_callsMakeOneWayWithD() {
        // Arrange
        when(milepostService.getBufferForPath(any(), eq(1.0), any()))
                .thenReturn(getMileposts());
        List<Integer> bufferCodes = List.of(7364); // unrelated

        // Act
        List<WydotTim> result = makeDecreasingTims(wydotTim, bufferCodes, milepostService);

        // Assert
        for (WydotTim tim : result) {
            assertEquals("d", tim.getDirection());
        }
    }

    @Test
    void testMakeBufferTims_createsBufferTimsWithUpdatedClientIdAndGeometry() {
        // Arrange
        when(milepostService.getBufferForPath(any(), eq(1.0), any()))
                .thenReturn(getMileposts());
        // Set a buffer ITIS code that matches the end of an ITIS entry
        List<Integer> bufferCodes = List.of(7364);
        wydotTim.setItisCodes(List.of("1309 7364"));  // Ends in 7364 -> matches buffer code

        // Act
        List<WydotTim> result = makeBufferTims(wydotTim, bufferCodes, milepostService);

        // Assert
        assertEquals(1, result.size());
        WydotTim tim = result.get(0);

        // Assert that clientId has been updated with "%BUFF"
        assertTrue(tim.getClientId().contains("%BUFF"));

        // Assert geometry was set correctly
        assertNotNull(wydotTim.getGeometry());
        assertEquals(4, wydotTim.getGeometry().size());
        assertEquals(BigDecimal.valueOf(200), wydotTim.getGeometry().get(0).getLatitude());
        assertEquals(BigDecimal.valueOf(300), wydotTim.getGeometry().get(0).getLongitude());

        // Assert direction and ITIS code structure
        assertEquals("I", tim.getDirection());
        assertEquals(List.of("1309", "7364"), tim.getItisCodes());
    }
}
