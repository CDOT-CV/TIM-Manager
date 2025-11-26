package com.trihydro.library.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
public class RegionNameTrimmerTest {

    @Mock
    private Utility utility;

    @InjectMocks
    private RegionNameTrimmer uut;

    @Test
    public void testTrimRegionName_Empty_SUCCESS() {
        String regionName = "";
        String trimmedRegionName = uut.trimRegionNameIfTooLong(regionName);
        assertEquals(regionName, trimmedRegionName);
    }

    @Test
    public void testTrimRegionName_Null_SUCCESS() {
        String regionName = null;
        String trimmedRegionName = uut.trimRegionNameIfTooLong(regionName);
        assertEquals(regionName, trimmedRegionName);
    }

    @Test
    public void testTrimRegionName_LessThanMaxLength_SUCCESS() {
        String regionName = "I_Prairie Center Cir_RSU-10.145.1.100_RC_clientid";
        String trimmedRegionName = uut.trimRegionNameIfTooLong(regionName);
        assertEquals(regionName, trimmedRegionName);
    }

    @Test
    public void testTrimRegionName_EqualsMaxLength_SUCCESS() {
        String regionName = "I_Prairie Center Circle Drive_RSU-10.145.1.100_RC_alongclientid";
        String trimmedRegionName = uut.trimRegionNameIfTooLong(regionName);
        assertEquals(regionName, trimmedRegionName);
    }
}