package com.trihydro.library.service;

import com.trihydro.library.model.AdvisorySituationDataDeposit;
import com.trihydro.library.model.SDXDecodeRequest;
import com.trihydro.library.model.SDXDecodeResponse;
import com.trihydro.library.model.SdwProps;
import com.trihydro.library.model.SemiDialogID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SdwServiceTest extends BaseServiceTest {

    @Mock
    SdwProps mockConfig;

    @Mock
    ResponseEntity<AdvisorySituationDataDeposit[]> mockAsddResponse;

    @Mock
    ResponseEntity<SDXDecodeResponse> mockDecodeResponse;

    @Mock
    ResponseEntity<AdvisorySituationDataDeposit> mockRespAdvisorySituationDataDeposit;

    @Mock
    ResponseEntity<HashMap<Integer, Boolean>> mockRespHashMap;

    @InjectMocks
    SdwService sdwService;

    String baseUrl = "http://localhost:12230";
    String apiKey = "apiKey";

    void setupConfig() {
        when(mockConfig.getSdwRestUrl()).thenReturn(baseUrl);
        setupApiKey();
    }

    void setupApiKey() {
        when(mockConfig.getSdwApiKey()).thenReturn(apiKey);
    }

    @Test
    void deleteSdxDataBySatRecordId_nullRecordIds() {
        setupApiKey();
        HashMap<Integer, Boolean> results = sdwService.deleteSdxDataBySatRecordId(null);
        Assertions.assertNull(results);
    }

    @Test
    void deleteSdxDataBySatRecordId_emptyRecordIds() {
        setupApiKey();
        HashMap<Integer, Boolean> results = sdwService.deleteSdxDataBySatRecordId(new ArrayList<String>());
        Assertions.assertNull(results);
    }

    @Test
    void deleteSdxDataBySatRecordId_nullApiKey() {
        List<String> satNames = new ArrayList<String>();
        satNames.add("A9184436");
        when(mockConfig.getSdwApiKey()).thenReturn(null);
        HashMap<Integer, Boolean> results = sdwService.deleteSdxDataBySatRecordId(satNames);
        Assertions.assertNull(results);
    }

    @Test
    void deleteSdxDataBySatRecordId_success() throws IOException, Exception {
        // Arrange
        setupConfig();
        List<String> satNames = new ArrayList<String>();
        satNames.add("A9184436");
        List<Integer> satIds = new ArrayList<Integer>();
        satIds.add(-1458027466);
        HashMap<Integer, Boolean> hMap = new HashMap<Integer, Boolean>();
        hMap.put(-1, true);

        String url = String.format("%s/api/delete-multiple-by-recordid", baseUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("apikey", apiKey);
        HttpEntity<List<Integer>> entity = new HttpEntity<List<Integer>>(satIds, headers);
        ParameterizedTypeReference<HashMap<Integer, Boolean>> responseType = new ParameterizedTypeReference<HashMap<Integer, Boolean>>() {
        };
        when(mockRestTemplate.exchange(url, HttpMethod.DELETE, entity, responseType)).thenReturn(mockRespHashMap);
        when(mockRespHashMap.getBody()).thenReturn(hMap);

        // Act
        HashMap<Integer, Boolean> results = sdwService.deleteSdxDataBySatRecordId(satNames);

        // Assert
        Assertions.assertNotNull(results);
        Assertions.assertTrue(results.containsKey(-1));
    }

    @Test
    void deleteSdxDataBySatRecordId_handlesHttpClientErrorException() {
        // Arrange
        setupConfig();
        List<String> satRecordIds = new ArrayList<String>();
        satRecordIds.add("A9184436");
        
        List<Integer> satRecordInts = satRecordIds.stream().map(x -> Integer.parseUnsignedInt(x, 16))
                .collect(Collectors.toList());

        String url = String.format("%s/api/delete-multiple-by-recordid", baseUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("apikey", apiKey);
        HttpEntity<List<Integer>> entity = new HttpEntity<List<Integer>>(satRecordInts, headers);
        ParameterizedTypeReference<HashMap<Integer, Boolean>> responseType = new ParameterizedTypeReference<HashMap<Integer, Boolean>>() {
        };
        when(mockRestTemplate.exchange(url, HttpMethod.DELETE, entity, responseType))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "something went wrong..."));

        // Act
        HashMap<Integer, Boolean> results = sdwService.deleteSdxDataBySatRecordId(satRecordIds);

        // Assert
        Assertions.assertNull(results);
    }

    @Test
    void deleteSdxDataByRecordIdIntegers_handlesHttpClientErrorException() {
        // Arrange
        setupConfig();
        List<Integer> recordIds = new ArrayList<Integer>();
        recordIds.add(1);
        recordIds.add(2);
        recordIds.add(3);

        String url = String.format("%s/api/delete-multiple-by-recordid", baseUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("apikey", apiKey);
        HttpEntity<List<Integer>> entity = new HttpEntity<List<Integer>>(recordIds, headers);
        ParameterizedTypeReference<HashMap<Integer, Boolean>> responseType = new ParameterizedTypeReference<HashMap<Integer, Boolean>>() {
        };
        when(mockRestTemplate.exchange(url, HttpMethod.DELETE, entity, responseType))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "something went wrong..."));

        // Act
        HashMap<Integer, Boolean> results = sdwService.deleteSdxDataByRecordIdIntegers(recordIds);

        // Assert
        Assertions.assertNull(results);
    }

    @Test
    void getItisCodesFromAdvisoryMessage_success() throws SdwService.SdwServiceException {
        // Arrange
        setupConfig();

        // Response from SDX
        SDXDecodeResponse response = new SDXDecodeResponse();
        String abbDecodedMsg = "<MessageFrame><advisory><SEQUENCE><item><itis>268</itis></item></SEQUENCE>";
        abbDecodedMsg += "<SEQUENCE><item><itis>12619</itis></item></SEQUENCE><SEQUENCE><item><itis>8720</itis></item></SEQUENCE></advisory></MessageFrame>";
        response.setDecodedMessage(abbDecodedMsg);
        response.setMessageType("MessageFrame");

        // Intercept request, returning our fake response
        String url = "http://localhost:12230/api/decode";
        when(mockRestTemplate.exchange(eq(url), eq(HttpMethod.POST), isA(HttpEntity.class),
                eq(SDXDecodeResponse.class))).thenReturn(mockDecodeResponse);
        when(mockDecodeResponse.getBody()).thenReturn(response);

        // Act
        List<Integer> result = sdwService.getItisCodesFromAdvisoryMessage("AAAAAAAAAAAAAAAAA001F");

        // Assert
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.contains(268));
        Assertions.assertTrue(result.contains(12619));
        Assertions.assertTrue(result.contains(8720));
    }

    @Test
    void getItisCodesFromAdvisoryMessage_realData() throws SdwService.SdwServiceException {
        // Arrange
        setupConfig();

        // request contains real, PER-encoded MessageFrame, and response contains the
        // corresponding response from the SDX's /api/decode endpoint
        SDXDecodeRequest request = importJsonObject("/sdxDecodeRequest.json", SDXDecodeRequest.class);
        SDXDecodeResponse response = importJsonObject("/sdxDecodeResponse.json", SDXDecodeResponse.class);

        String url = "http://localhost:12230/api/decode";
        when(mockRestTemplate.exchange(eq(url), eq(HttpMethod.POST), isA(HttpEntity.class),
                eq(SDXDecodeResponse.class))).thenReturn(mockDecodeResponse);
        when(mockDecodeResponse.getBody()).thenReturn(response);

        // Act
        List<Integer> result = sdwService.getItisCodesFromAdvisoryMessage(request.getEncodedMsg());

        // Assert
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(5895));
        Assertions.assertTrue(result.contains(5907));
    }

    @Test
    void getItisCodesFromAdvisoryMessage_nullAdvisoryMessage() {
        // Arrange

        // Act
        var exception = Assertions.assertThrows(SdwService.SdwServiceException.class, () -> {
            sdwService.getItisCodesFromAdvisoryMessage(null);
        });

        Assertions.assertEquals("Null advisory message provided", exception.getMessage());
    }

    @Test
    void getItisCodesFromAdvisoryMessage_NoMessageFrame() {
        // Arrange

        // Act
        var exception = Assertions.assertThrows(SdwService.SdwServiceException.class, () -> {
            sdwService.getItisCodesFromAdvisoryMessage("00000000");
        });

        Assertions.assertEquals("Invalid message format - missing MessageFrame marker", exception.getMessage());
    }

    @Test
    void getItisCodesFromAdvisoryMessage_handlesNumberFormatException() throws SdwService.SdwServiceException {
        // Arrange
        setupConfig();

        // Response from SDX
        SDXDecodeResponse response = new SDXDecodeResponse();
        String abbDecodedMsg = "<MessageFrame><advisory><SEQUENCE><item><itis>268</itis></item></SEQUENCE>";
        abbDecodedMsg += "<SEQUENCE><item><itis>NOTANUMBER</itis></item></SEQUENCE><SEQUENCE><item><itis>8720</itis></item></SEQUENCE></advisory></MessageFrame>";
        response.setDecodedMessage(abbDecodedMsg);
        response.setMessageType("MessageFrame");

        // Intercept request, returning our fake response
        String url = "http://localhost:12230/api/decode";
        when(mockRestTemplate.exchange(eq(url), eq(HttpMethod.POST), isA(HttpEntity.class),
                eq(SDXDecodeResponse.class))).thenReturn(mockDecodeResponse);
        when(mockDecodeResponse.getBody()).thenReturn(response);

        // Act
        List<Integer> result = sdwService.getItisCodesFromAdvisoryMessage("00000000000000001F");

        // Assert
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(268));
        Assertions.assertTrue(result.contains(8720));
    }

    @Test
    void getItisCodesFromAdvisoryMessage_handlesRestClientException() {
        // Arrange
        setupConfig();

        // Response from SDX
        SDXDecodeResponse response = new SDXDecodeResponse();
        String abbDecodedMsg = "<MessageFrame><advisory><SEQUENCE><item><itis>268</itis></item></SEQUENCE>";
        abbDecodedMsg += "<SEQUENCE><item><itis>NOTANUMBER</itis></item></SEQUENCE><SEQUENCE><item><itis>8720</itis></item></SEQUENCE></advisory></MessageFrame>";
        response.setDecodedMessage(abbDecodedMsg);
        response.setMessageType("MessageFrame");

        // Intercept request, returning our fake response
        String url = "http://localhost:12230/api/decode";
        when(mockRestTemplate.exchange(eq(url), eq(HttpMethod.POST), isA(HttpEntity.class),
                eq(SDXDecodeResponse.class))).thenThrow(new RestClientException("something went wrong..."));

        // Act & Assert
        Assertions.assertThrows(SdwService.SdwServiceException.class, () -> {
            sdwService.getItisCodesFromAdvisoryMessage("00000000000000001F");
        });
    }

    @Test
    void getMsgsForOdeUser_success() throws SdwService.SdwServiceException {
        // Arrange
        setupConfig();
        AdvisorySituationDataDeposit[] response = new AdvisorySituationDataDeposit[] {
                new AdvisorySituationDataDeposit() };

        String url = "http://localhost:12230/api/deposited-by-me/156";
        when(mockRestTemplate.exchange(eq(url), eq(HttpMethod.GET), isA(HttpEntity.class),
                eq(AdvisorySituationDataDeposit[].class))).thenReturn(mockAsddResponse);
        when(mockAsddResponse.getBody()).thenReturn(response);

        // Act
        List<AdvisorySituationDataDeposit> results = sdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);

        // Assert
        Assertions.assertEquals(1, results.size());
        verify(mockRestTemplate).exchange(eq(url), eq(HttpMethod.GET), isA(HttpEntity.class),
                eq(AdvisorySituationDataDeposit[].class));
    }

    @Test
    void getMsgsForOdeUser_handlesRestClientException() {
        // Arrange
        setupConfig();
        String url = "http://localhost:12230/api/deposited-by-me/156";
        when(mockRestTemplate.exchange(eq(url), eq(HttpMethod.GET), isA(HttpEntity.class),
                eq(AdvisorySituationDataDeposit[].class)))
                        .thenThrow(new RestClientException("something went wrong..."));

        // Act & Assert
        Assertions.assertThrows(SdwService.SdwServiceException.class, () -> {
            sdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);
        });
    }
}