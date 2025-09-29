package com.trihydro.library.service;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.model.CVRestServiceProps;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class MilepostServiceTest extends BaseServiceTest {

    @Mock
    private ResponseEntity<List<Milepost>> mockRespMilepostList;

    @Mock
    private CVRestServiceProps mockConfig;

    @InjectMocks
    private MilepostService uut;

    private String baseUrl = "baseUrl";

    @BeforeEach
    public void setupSubTest() {
        doReturn(baseUrl).when(mockConfig).getCvRestService();
    }

    @Test
    public void getMilepostsByStartEndPoint() {
        // Arrange
        WydotTim wydotTim = new WydotTim();
        List<Milepost> mileposts = new ArrayList<>();
        Milepost milepost = new Milepost();
        milepost.setDirection("B");
        milepost.setCommonName("route");
        mileposts.add(milepost);
        doReturn(mileposts).when(mockRespMilepostList).getBody();
        HttpEntity<WydotTim> entity = getEntity(wydotTim, WydotTim.class);
        String url = String.format("%s/get-milepost-start-end", baseUrl);
        ParameterizedTypeReference<List<Milepost>> responseType = new ParameterizedTypeReference<List<Milepost>>() {
        };
        when(mockRestTemplate.exchange(url, HttpMethod.POST, entity, responseType)).thenReturn(mockRespMilepostList);

        // Act
        List<Milepost> data = uut.getMilepostsByStartEndPointDirection(wydotTim);

        // Assert
        verify(mockRestTemplate).exchange(url, HttpMethod.POST, entity, responseType);
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(milepost, data.get(0));
    }

    @Test
    public void getMilepostsByPointWithBuffer() {
        // Arrange
        MilepostBuffer mpb = new MilepostBuffer();
        List<Milepost> mileposts = new ArrayList<>();
        Milepost milepost = new Milepost();
        milepost.setDirection("B");
        milepost.setCommonName("route");
        mileposts.add(milepost);
        doReturn(mileposts).when(mockRespMilepostList).getBody();
        HttpEntity<MilepostBuffer> entity = getEntity(mpb, MilepostBuffer.class);
        String url = String.format("%s/get-milepost-single-point", baseUrl);
        ParameterizedTypeReference<List<Milepost>> responseType = new ParameterizedTypeReference<List<Milepost>>() {
        };
        when(mockRestTemplate.exchange(url, HttpMethod.POST, entity, responseType)).thenReturn(mockRespMilepostList);

        // Act
        List<Milepost> data = uut.getMilepostsByPointWithBuffer(mpb);

        // Assert
        verify(mockRestTemplate).exchange(url, HttpMethod.POST, entity, responseType);
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(milepost, data.get(0));
    }

    @Test
    public void getBufferForPath_Success() {
        // prepare
        String routeId = "routeId";
        double desiredDistanceInMiles = 10.0;
        Milepost milepost1 = new Milepost();
        Milepost milepost2 = new Milepost();
        List<Milepost> pathMileposts = new ArrayList<>();
        pathMileposts.add(milepost1);
        pathMileposts.add(milepost2);
        Milepost bufferMilepost1 = new Milepost();
        Milepost bufferMilepost2 = new Milepost();
        List<Milepost> bufferMileposts = new ArrayList<>();
        bufferMileposts.add(bufferMilepost1);
        bufferMileposts.add(bufferMilepost2);
        when(mockRespMilepostList.getBody()).thenReturn(bufferMileposts);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<Milepost>> entity = new HttpEntity<>(pathMileposts, headers);
        String url = String.format("%s/cdot-upstream-path/get-buffer-for-path/%s/%s", baseUrl, routeId, desiredDistanceInMiles);
        ParameterizedTypeReference<List<Milepost>> responseType = new ParameterizedTypeReference<List<Milepost>>() {
        };
        when(mockRestTemplate.exchange(url, HttpMethod.POST, entity, responseType)).thenReturn(mockRespMilepostList);

        // execute
        List<Milepost> data = uut.getBufferForPath(routeId, desiredDistanceInMiles, pathMileposts);

        // verify
        verify(mockRestTemplate).exchange(url, HttpMethod.POST, entity, responseType);
        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals(bufferMilepost1, data.get(0));
        Assertions.assertEquals(bufferMilepost2, data.get(1));
    }

    @Test
    public void getBufferForPath_Failure_ResponseBodyNull() {
        // prepare
        String routeId = "routeId";
        double desiredDistanceInMiles = 10.0;
        Milepost milepost1 = new Milepost();
        Milepost milepost2 = new Milepost();
        List<Milepost> pathMileposts = new ArrayList<>();
        pathMileposts.add(milepost1);
        pathMileposts.add(milepost2);
        when(mockRespMilepostList.getBody()).thenReturn(null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<Milepost>> entity = new HttpEntity<>(pathMileposts, headers);
        String url = String.format("%s/cdot-upstream-path/get-buffer-for-path/%s/%s", baseUrl, routeId, desiredDistanceInMiles);
        ParameterizedTypeReference<List<Milepost>> responseType = new ParameterizedTypeReference<List<Milepost>>() {
        };
        when(mockRestTemplate.exchange(url, HttpMethod.POST, entity, responseType)).thenReturn(mockRespMilepostList);

        // execute
        List<Milepost> data = uut.getBufferForPath(routeId, desiredDistanceInMiles, pathMileposts);

        // verify
        verify(mockRestTemplate).exchange(url, HttpMethod.POST, entity, responseType);
        Assertions.assertNull(data);
    }
}
