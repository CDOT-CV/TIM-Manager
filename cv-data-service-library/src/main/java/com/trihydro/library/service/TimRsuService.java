package com.trihydro.library.service;

import java.util.Arrays;
import java.util.List;

import com.trihydro.library.model.TimRsu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TimRsuService extends CvDataServiceLibrary {

	public List<TimRsu> getTimRsusByTimId(Long timId) {
		log.trace("Getting TimRsus for timId: {}", timId);
		// tim-id
		String url = String.format("%s/tim-rsu/tim-id/%d", config.getCvRestService(), timId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(null, headers);
		ResponseEntity<TimRsu[]> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.GET, entity,
				TimRsu[].class);
		return Arrays.asList(response.getBody());
	}

	public TimRsu getTimRsu(Long timId, Integer rsuId) {
		String url = String.format("%s/tim-rsu/tim-rsu/%d/%d", config.getCvRestService(), timId, rsuId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(null, headers);
		ResponseEntity<TimRsu> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.GET, entity,
				TimRsu.class);
		return response.getBody();
	}
}
