package com.trihydro.library.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DataFrameService extends CvDataServiceLibrary {
	private final Logger logger = LoggerFactory.getLogger(DataFrameService.class);

	/**
	 * Calls out to cv-data-controller REST service to fetch ITIS codes associated
	 * with a given DataFrame id
	 * 
	 * @param dataFrameId the DataFrame id to fetch ITIS codes for
	 * @return String array of all ITIS codes associated with dataFrameId
	 */
	public String[] getItisCodesForDataFrameId(Integer dataFrameId) {
		String url = String.format("%s/data-frame/itis-for-data-frame/%d", config.getCvRestService(), dataFrameId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(null, headers);
		logger.debug("Getting ITIS codes for dataFrameId: {} from URL: {}", dataFrameId, url);
		String[] itisCodes = new String[0];
		try {
			ResponseEntity<String[]> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.GET, entity, String[].class);
			if (response.getBody() != null) {
				itisCodes = response.getBody();
			}
		} catch (Exception e) {
			logger.error("Error getting ITIS codes for dataFrameId: {} from URL: {}", dataFrameId, url, e);
		}
		logger.debug("ITIS codes for dataFrameId: {} are: {}", dataFrameId, itisCodes);
		return itisCodes;
	}

}
