package com.trihydro.library.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.trihydro.library.model.AdvisorySituationDataDeposit;
import com.trihydro.library.model.SDXDecodeRequest;
import com.trihydro.library.model.SDXDecodeResponse;
import com.trihydro.library.model.SDXQuery;
import com.trihydro.library.model.SdwProps;
import com.trihydro.library.model.SemiDialogID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SdwService {
    public final Gson gson = new Gson();
    private final SdwProps configProperties;
    private final RestTemplateProvider restTemplateProvider;

    /**
     * Fetches messages deposited into the SDX, by the ODE User (identified by
     * apikey).
     * 
     * @param type Type of message to retrieve
     */
    public List<AdvisorySituationDataDeposit> getMsgsForOdeUser(SemiDialogID type) throws RestClientException, SdwServiceException {
        log.info("Fetching SDX messages for dialog type: {}", type);

        if (type == null) {
            throw new SdwServiceException("Null dialog type passed to getMsgsForOdeUser");
        }

        List<AdvisorySituationDataDeposit> results = null;

        String url = String.format("%s/api/deposited-by-me/%d", configProperties.getSdwRestUrl(), type.getValue());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("apikey", configProperties.getSdwApiKey());

        HttpEntity<SDXQuery> entity = new HttpEntity<>(null, headers);
        try {
            ResponseEntity<AdvisorySituationDataDeposit[]> response = restTemplateProvider.GetRestTemplate()
                    .exchange(url, HttpMethod.GET, entity, AdvisorySituationDataDeposit[].class);

            results = Arrays.asList(response.getBody());
            log.info("Successfully retrieved {} SDX messages", results.size());
        } catch (RestClientException ex) {
            log.error("GET messages failed - {}. DialogType={}, URL={}", ex.getMessage(), type, url);
            throw new SdwServiceException("SDX GET messages request failed: " + ex.getMessage(), ex);
        }

        return results;
    }

    public List<Integer> getItisCodesFromAdvisoryMessage(String advisoryMessage) throws SdwServiceException {
        if (advisoryMessage == null) {
            log.error("Null advisory message provided");
            throw new SdwServiceException("Null advisory message provided");
        }
        log.trace("Processing advisory message for ITIS codes, message length: {}", advisoryMessage.length());
        int idx = advisoryMessage.indexOf("001F");
        if (idx < 0) {
            log.error("Invalid message format - missing MessageFrame marker");
            throw new SdwServiceException("Invalid message format - missing MessageFrame marker");
        }

        List<Integer> results = new ArrayList<>();
        SDXDecodeResponse decodeResponse = null;

        // Build request
        String url = String.format("%s/api/decode", configProperties.getSdwRestUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("apikey", configProperties.getSdwApiKey());

        SDXDecodeRequest request = new SDXDecodeRequest();
        request.setEncodeType("hex");
        request.setMessageType("MessageFrame");
        request.setEncodedMsg(advisoryMessage.substring(idx));

        // Execute request
        try {
            log.trace("Sending decode request to SDX API");
            HttpEntity<SDXDecodeRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<SDXDecodeResponse> response = restTemplateProvider.GetRestTemplate().exchange(url,
                    HttpMethod.POST, entity, SDXDecodeResponse.class);

            decodeResponse = response.getBody();
            if (decodeResponse == null || decodeResponse.getDecodedMessage() == null) {
                log.warn("Received null decode response from SDX");
                throw new SdwServiceException("SDX decode response is null");
            }

        } catch (RestClientException ex) {
            log.error("POST decode failed - {}. Message length={}, URL={}", ex.getMessage(), advisoryMessage.length(), url);
            throw new SdwServiceException("SDX decode request failed: " + ex.getMessage(), ex);
        }

        // Process request (convert decodedMessage into an array of ITIS codes)
        Pattern p = Pattern.compile("(<itis>)([0-9]*)(</itis>)");
        Matcher m = p.matcher(decodeResponse.getDecodedMessage());

        while (m.find()) {
            String itisCode = m.group(2);

            try {
                results.add(Integer.parseInt(itisCode));
            } catch (NumberFormatException ex) {
                log.error("Failed to parse ITIS code: {}", m.group(2), ex);
            }
        }

        log.trace("Successfully extracted {} ITIS codes from message", results.size());
        return results;
    }

    /**
     * Returns a pseudo-random 4 byte hex value representing recordId. This 4 byte
     * limitation comes from asn1_codec SEMI_v2.3.0_070616.asn found at
     * https://github.com/usdot-jpo-ode/asn1_codec/blob/master/asn1c_combined/SEMI_v2.3.0_070616.asn
     *
     */
    public String getNewRecordId() {
        String hexChars = "ABCDEF1234567890";
        StringBuilder hexStrB = new StringBuilder();
        Random rnd = new Random();
        while (hexStrB.length() < 8) { // length of the random string.
            int index = (int) (rnd.nextFloat() * hexChars.length());
            hexStrB.append(hexChars.charAt(index));
        }
        return hexStrB.toString();
    }

    public HashMap<Integer, Boolean> deleteSdxDataBySatRecordId(List<String> satRecordIds) {
        log.trace("deleteSdxDataBySatRecordId called with satRecordIds: {}", satRecordIds);

        log.info("Attempting to delete {} SDX record(s)",
                satRecordIds != null ? satRecordIds.size() : 0);

        HashMap<Integer, Boolean> results = null;
        if (satRecordIds == null || satRecordIds.isEmpty() || configProperties.getSdwApiKey() == null) {
            if (configProperties.getSdwApiKey() == null) {
                log.error("Attempting to delete satellite records failed due to null apiKey");
            } else {
                log.error("Attempting to delete satellite records failed due to no satRecordIds passed in");
            }
            log.trace("Returning null results");
            return results;
        }

        List<Integer> satRecordInts = satRecordIds.stream().map(x -> Integer.parseUnsignedInt(x, 16))
                .collect(Collectors.toList());

        String url = getBaseUrlString("api/delete-multiple-by-recordid");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("apikey", configProperties.getSdwApiKey());
        HttpEntity<List<Integer>> entity = new HttpEntity<List<Integer>>(satRecordInts, headers);
        ParameterizedTypeReference<HashMap<Integer, Boolean>> responseType = new ParameterizedTypeReference<HashMap<Integer, Boolean>>() {
        };
        ResponseEntity<HashMap<Integer, Boolean>> response;
        try {
            log.debug("Sending request to delete satellite records with IDs: {}", satRecordIds);
            response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.DELETE, entity, responseType);
        } catch (HttpClientErrorException ex) {
            log.error("DELETE records failed - {}. recordIds={}", ex.getMessage(), satRecordIds);
            response = new ResponseEntity<>(ex.getStatusCode());
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Delete operation failed with status: {}", response.getStatusCode());
        }
        return response.getBody();
    }

    public HashMap<Integer, Boolean> deleteSdxDataByRecordIdIntegers(List<Integer> satRecordInts) {
        HashMap<Integer, Boolean> results = null;
        if (satRecordInts == null || satRecordInts.isEmpty() || configProperties.getSdwApiKey() == null) {
            if (configProperties.getSdwApiKey() == null) {
                log.info("Attempting to delete satellite records failed due to null apiKey");
            } else {
                log.info("Attempting to delete satellite records failed due to no satRecordIds passed in");
            }
            return results;
        }

        String url = getBaseUrlString("api/delete-multiple-by-recordid");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("apikey", configProperties.getSdwApiKey());
        HttpEntity<List<Integer>> entity = new HttpEntity<List<Integer>>(satRecordInts, headers);
        ParameterizedTypeReference<HashMap<Integer, Boolean>> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<HashMap<Integer, Boolean>> response;
        try {
            response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.DELETE, entity, responseType);
        } catch (HttpClientErrorException ex) {
            log.error("DELETE records failed - {}. recordIds={}", ex.getMessage(), satRecordInts);

            response = new ResponseEntity<>(ex.getStatusCode());
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Delete operation failed with status: {}", response.getStatusCode());
        }
        return response.getBody();
    }

    private String getBaseUrlString(String end) {
        String baseUrl = configProperties.getSdwRestUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        baseUrl += end;
        return baseUrl;
    }

    public static class SdwServiceException extends Exception {
        public SdwServiceException(String message) {
            super(message);
        }

        public SdwServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}