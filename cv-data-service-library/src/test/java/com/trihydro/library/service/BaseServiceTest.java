package com.trihydro.library.service;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RestTemplateProvider.class })
public class BaseServiceTest {
    @Mock
    protected RestTemplate mockRestTemplate;
    @Mock
    protected ResponseEntity<Long> mockResponseEntityLong;

    @Before
    public void setup() throws SQLException {
        PowerMockito.mockStatic(RestTemplateProvider.class);
        when(RestTemplateProvider.GetRestTemplate()).thenReturn(mockRestTemplate);
        when(mockResponseEntityLong.getBody()).thenReturn(1l);
    }

    protected HttpHeaders getDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @SuppressWarnings("unchecked")
    protected <T> HttpEntity<T> getEntity(Object body, Class<T> clazz) {
        HttpEntity<T> entity = new HttpEntity<T>((T) body, getDefaultHeaders());
        return entity;
    }

    protected <T> T importJsonObject(String fileName, Class<T> clazz) {
        InputStream is = BaseServiceTest.class.getResourceAsStream(fileName);
        InputStreamReader isr = new InputStreamReader(is);

        Gson gson = new Gson();
        T data = gson.fromJson(isr, clazz);

        try {
            isr.close();
        } catch (IOException ex) {

        }

        return data;
    }
}