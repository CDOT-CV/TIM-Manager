package com.trihydro.library.service;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.trihydro.library.model.WydotTravelerInputData;
import java.math.BigDecimal;

import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.OdeProps;
import com.trihydro.library.model.TimQuery;
import com.trihydro.library.model.WydotRsu;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class OdeServiceTest {

    @Mock
    Utility mockUtility;
    @Mock
    OdeProps mockOdeProps;

    @Mock
    RestTemplateProvider mockRestTemplateProvider;

    @InjectMocks
    OdeService uut;

    String baseUrl = "http://localhost:8080";
    RestTemplate restTemplate = new RestTemplate();

    @Mock
    MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();

    @BeforeEach
    void setupSubTest() {
        mockRestTemplateProvider = Mockito.mock(RestTemplateProvider.class);
        mockOdeProps = Mockito.mock(OdeProps.class);
        mockUtility = Mockito.mock(Utility.class);
        uut = new OdeService();
        uut.InjectDependencies(mockUtility, mockRestTemplateProvider, mockOdeProps);
        lenient().when(mockRestTemplateProvider.GetRestTemplate()).thenReturn(restTemplate);
        when(mockOdeProps.getOdeUrl()).thenReturn(baseUrl);
        lenient().when(mockRestTemplateProvider.GetRestTemplate_NoErrors()).thenReturn(restTemplate);
    }

    @Test
    void updateTimOnSdw_Success() {
        // prepare
        WydotTravelerInputData timToSend = new WydotTravelerInputData();
        String url = baseUrl + "/tim";
        String jsonString = "{\"status\":\"success\"}";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        String exceptionMessage = uut.updateTimOnSdw(timToSend);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate_NoErrors();
        mockServer.verify();
        Assertions.assertEquals("", exceptionMessage);
    }

    @Test
    void updateTimOnSdw_Failure_ServerError() {
        // prepare
        WydotTravelerInputData timToSend = new WydotTravelerInputData();
        String url = baseUrl + "/tim";
        mockServer.expect(requestTo(url)).andRespond(withServerError());

        // execute
        String exceptionMessage = uut.updateTimOnSdw(timToSend);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate_NoErrors();
        mockServer.verify();
        Assertions.assertEquals("An exception occurred while updating TIM on SDX", exceptionMessage);
    }

    @Test
    void submitTimQuery_wydotRsu_NoIndicies() {
        // prepare
        WydotRsu rsu = new WydotRsu();
        rsu.setRsuId(-1);
        rsu.setRsuTarget("10.10.10.10");
        rsu.setLatitude(new BigDecimal(41.0000));
        rsu.setLongitude(new BigDecimal(-104.000000));
        rsu.setRoute("I 80");
        rsu.setMilepost(10d);
        when(mockOdeProps.getOdeUrl()).thenReturn(baseUrl);
        String url = baseUrl + "/tim/query";
        String jsonString = "{\"indicies_set\":\"[]\"}";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        TimQuery timQuery = uut.submitTimQuery(rsu, 1);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertNotNull(timQuery);
        Assertions.assertEquals(0, timQuery.getIndicies_set().size());
    }

    @Test
    void submitTimQuery_wydotRsu_SomeIndicies() {
        // prepare
        WydotRsu rsu = new WydotRsu();
        rsu.setRsuId(-1);
        rsu.setRsuTarget("10.10.10.10");
        rsu.setLatitude(new BigDecimal(41.0000));
        rsu.setLongitude(new BigDecimal(-104.000000));
        rsu.setRoute("I 80");
        rsu.setMilepost(10d);
        when(mockOdeProps.getOdeUrl()).thenReturn(baseUrl);
        String url = baseUrl + "/tim/query";
        String jsonString = "{\"indicies_set\":\"[1, 2, 3]\"}";
        mockServer.expect(requestTo(url)).andRespond(
            withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        TimQuery timQuery = uut.submitTimQuery(rsu, 1);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertNotNull(timQuery);
        Assertions.assertEquals(3, timQuery.getIndicies_set().size());
    }

    @Test
    void submitTimQuery_wydotRsu_ArrayInsteadOfObject() {
        // prepare
        WydotRsu rsu = new WydotRsu();
        rsu.setRsuId(-1);
        rsu.setRsuTarget("10.10.10.10");
        rsu.setLatitude(new BigDecimal(41.0000));
        rsu.setLongitude(new BigDecimal(-104.000000));
        rsu.setRoute("I 80");
        rsu.setMilepost(10d);
        when(mockOdeProps.getOdeUrl()).thenReturn(baseUrl);
        String url = baseUrl + "/tim/query";
        String jsonString = "[1, 2, 3]";
        mockServer.expect(requestTo(url)).andRespond(
            withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        TimQuery timQuery = uut.submitTimQuery(rsu, 1);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertNotNull(timQuery);
        Assertions.assertEquals(3, timQuery.getIndicies_set().size());
    }

    @Test
    void submitTimQuery_wydotRsu_EmptyString() {
        // prepare
        WydotRsu rsu = new WydotRsu();
        rsu.setRsuId(-1);
        rsu.setRsuTarget("10.10.10.10");
        rsu.setLatitude(new BigDecimal(41.0000));
        rsu.setLongitude(new BigDecimal(-104.000000));
        rsu.setRoute("I 80");
        rsu.setMilepost(10d);
        when(mockOdeProps.getOdeUrl()).thenReturn(baseUrl);
        String url = baseUrl + "/tim/query";
        String jsonString = "";
        mockServer.expect(requestTo(url)).andRespond(
            withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        Assertions.assertThrows(NullPointerException.class, () -> {
            uut.submitTimQuery(rsu, 1);
        });
    }
}
