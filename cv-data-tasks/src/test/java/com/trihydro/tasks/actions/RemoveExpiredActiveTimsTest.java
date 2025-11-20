package com.trihydro.tasks.actions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.tasks.config.DataTasksConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class RemoveExpiredActiveTimsTest {

    @Mock
    private DataTasksConfiguration mockConfig;
    @Mock
    private RestTemplate mockRestTemplate;
    @Mock
    ActiveTimService mockActiveTimService;
    @Mock
    RestTemplateProvider mockRestTemplateProvider;

    @InjectMocks
    public RemoveExpiredActiveTims uut;

    @Test
    public void run_ShouldCallRestTemplateExchangeTwice_WhenTwoExpiredActiveTimsExist() {
        // Arrange
        when(mockRestTemplateProvider.GetRestTemplate()).thenReturn(mockRestTemplate);

        List<ActiveTim> expiredTims = new ArrayList<ActiveTim>();
        expiredTims.add(new ActiveTim());
        expiredTims.add(new ActiveTim());
        when(mockActiveTimService.getExpiredActiveTims(500)).thenReturn(expiredTims).thenReturn(
            new ArrayList<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = new ResponseEntity<>("success", headers, HttpStatus.OK);
        when(mockRestTemplate.exchange(contains("/delete-tim/"), any(HttpMethod.class),
                Mockito.<HttpEntity<String>>any(), Mockito.<Class<String>>any())).thenReturn(response);

        // Act
        uut.run();

        // Assert
        verify(mockRestTemplate, Mockito.times(2)).exchange(any(String.class), any(HttpMethod.class),
                Mockito.<HttpEntity<String>>any(), Mockito.<Class<String>>any());
    }

    @Test
    public void run_ShouldCallRestTemplateExchangeTwice_WhenTwoExpiredActiveTimsExist_AndFirstRequestReturnsBadRequest() {
        // Arrange
        when(mockRestTemplateProvider.GetRestTemplate()).thenReturn(mockRestTemplate);

        List<ActiveTim> expiredTims = new ArrayList<ActiveTim>();
        expiredTims.add(new ActiveTim());
        expiredTims.add(new ActiveTim());
        when(mockActiveTimService.getExpiredActiveTims(500)).thenReturn(expiredTims).thenReturn(new ArrayList<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> secondResponse = new ResponseEntity<>("success", headers, HttpStatus.OK);

        when(mockRestTemplate.exchange(contains("/delete-tim/"), any(HttpMethod.class),
                Mockito.<HttpEntity<String>>any(), Mockito.<Class<String>>any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .thenReturn(secondResponse);

        // Act
        uut.run();

        // Assert
        verify(mockRestTemplate, Mockito.times(2)).exchange(contains("/delete-tim/"), any(HttpMethod.class),
                Mockito.<HttpEntity<String>>any(), Mockito.<Class<String>>any());
    }
}