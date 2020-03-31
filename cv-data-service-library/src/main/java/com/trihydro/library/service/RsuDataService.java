package com.trihydro.library.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.trihydro.library.model.RsuDataServiceProps;
import com.trihydro.library.model.RsuIndexInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class RsuDataService {
    private RsuDataServiceProps config;

    @Autowired
    public void InjectDependencies(RsuDataServiceProps config) {
        this.config = config;
    }

    /**
     * Gets the RSU's populated indexes and deliveryStart times.
     * 
     * @param ipv4Address Address of RSU
     * @return null if unable to establish connection with RSU
     */
    public List<RsuIndexInfo> getRsuDeliveryStartTimes(String ipv4Address) {
        String url = String.format("%s/rsu/%s/tims/delivery-start", config.getRsuDataServiceUrl(), ipv4Address);
        ResponseEntity<RsuIndexInfo[]> response = null;

        try {
            response = RestTemplateProvider.GetRestTemplate().getForEntity(url, RsuIndexInfo[].class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                return null;
            }

            throw ex;
        }

        List<RsuIndexInfo> result = new ArrayList<>();
        Collections.addAll(result, response.getBody());

        return result;
    }
}