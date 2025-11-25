package com.trihydro.library.service;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.trihydro.library.model.CVRestServiceProps;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class DataFrameServiceTest {
    @Mock
    RestTemplateProvider mockRestTemplateProvider;

    @Mock
    CVRestServiceProps mockConfig;

    @InjectMocks
    DataFrameService uut;

    String baseUrl = "http://localhost:8080";
    RestTemplate restTemplate = new RestTemplate();

    @Mock
    MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();

    @BeforeEach
    void setupSubTest() {
        mockRestTemplateProvider = Mockito.mock(RestTemplateProvider.class);
        mockConfig = Mockito.mock(CVRestServiceProps.class);
        uut = new DataFrameService();
        uut.InjectDependencies(mockConfig, mockRestTemplateProvider);
        lenient().when(mockRestTemplateProvider.GetRestTemplate()).thenReturn(restTemplate);
        when(mockConfig.getCvRestService()).thenReturn(baseUrl);
    }

    /**
     * Test when the controller returns one or more ITIS codes.
     * Verifies that the response body contains the expected ITIS codes.
     */
    @Test
    void getItisCodesForDataFrameId_NonEmptyArray() {
        // prepare
        Integer dataFrameId = -1;
        String jsonString = "[\"ITIS1\", \"ITIS2\"]";
        mockServer.expect(requestTo(baseUrl + "/data-frame/itis-for-data-frame/-1"))
            .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        String[] data = uut.getItisCodesForDataFrameId(dataFrameId);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertEquals(2, data.length);
    }

    /**
     * Test when the controller returns an empty array.
     * Verifies that the response body is an empty array.
     */
    @Test
    void getItisCodesForDataFrameId_EmptyArray() {
        // prepare
        Integer dataFrameId = -1;
        String jsonString = "[]";
        mockServer.expect(requestTo(baseUrl + "/data-frame/itis-for-data-frame/-1"))
            .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        String[] data = uut.getItisCodesForDataFrameId(dataFrameId);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertEquals(0, data.length);
    }

    /**
     * Test when the controller returns an array with a null value.
     * Verifies that the response body contains the expected ITIS codes.
     */
    @Test
    void getItisCodesForDataFrameId_NullPresentInArray() {
        // prepare
        Integer dataFrameId = -1;
        String jsonString = "[null, \"ITIS2\"]";
        mockServer.expect(requestTo(baseUrl + "/data-frame/itis-for-data-frame/-1"))
            .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        String[] data = uut.getItisCodesForDataFrameId(dataFrameId);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertEquals(2, data.length);
        Assertions.assertEquals("ITIS2", data[1]);
    }

    /**
     * Test when the controller returns a JSON string representing a non-array object.
     * Verifies that the response body is an empty array.
     */
    @Test
    void getItisCodesForDataFrameId_ObjectInsteadOfArray() {
        // prepare
        Integer dataFrameId = -1;
        String jsonString = "{\"key\": \"value\"}";
        mockServer.expect(requestTo(baseUrl + "/data-frame/itis-for-data-frame/-1"))
            .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        String[] data = uut.getItisCodesForDataFrameId(dataFrameId);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertEquals(0, data.length);
    }

    /**
     * Test when the controller returns an empty JSON string.
     * Verifies that the response body is an empty array.
     */
    @Test
    void getItisCodesForDataFrameId_EmptyJsonString() {
        // prepare
        Integer dataFrameId = -1;
        String jsonString = "";
        mockServer.expect(requestTo(baseUrl + "/data-frame/itis-for-data-frame/-1"))
            .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        String[] data = uut.getItisCodesForDataFrameId(dataFrameId);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        mockServer.verify();
        Assertions.assertEquals(0, data.length);
    }

    /**
     * Test when there are issues with obtaining a server connection.
     * Verifies that the response body is an empty array.
     */
    @Test
    void getItisCodesForDataFrameId_ServerConnectionIssues() {
        // prepare
        Integer dataFrameId = -1;
        when(mockRestTemplateProvider.GetRestTemplate()).thenThrow(new RuntimeException("Server Connection Issues"));

        // execute
        String[] data = uut.getItisCodesForDataFrameId(dataFrameId);

        // verify
        verify(mockRestTemplateProvider).GetRestTemplate();
        Assertions.assertEquals(0, data.length);
    }
}