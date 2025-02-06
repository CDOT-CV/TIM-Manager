package com.trihydro.library.service;

import java.util.List;

import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.MilepostBuffer;
import com.trihydro.library.model.WydotTim;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class MilepostService extends CvDataServiceLibrary {

	public List<Milepost> getMilepostsByStartEndPointDirection(WydotTim wydotTim) {
		String url = String.format("%s/get-milepost-start-end", config.getCvRestService());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<WydotTim> entity = new HttpEntity<WydotTim>(wydotTim, headers);
		ParameterizedTypeReference<List<Milepost>> responseType = new ParameterizedTypeReference<List<Milepost>>() {
		};
		ResponseEntity<List<Milepost>> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.POST,
				entity, responseType);
		return response.getBody();
	}

	public List<Milepost> getMilepostsByPointWithBuffer(MilepostBuffer milepostBuffer) {
		String url = String.format("%s/get-milepost-single-point", config.getCvRestService());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<MilepostBuffer> entity = new HttpEntity<MilepostBuffer>(milepostBuffer, headers);
		ParameterizedTypeReference<List<Milepost>> responseType = new ParameterizedTypeReference<List<Milepost>>() {
		};
		ResponseEntity<List<Milepost>> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.POST,
				entity, responseType);
		return response.getBody();
	}

	/**
	 * Retrieves a buffer of mileposts for a given path from the CDOT upstream service.
	 *
	 * @param routeId the ID of the route for which the buffer is to be retrieved
	 * @param desiredDistanceInMiles the desired distance in miles for the buffer
	 * @param pathMileposts the list of mileposts representing the path
	 * @return a list of mileposts representing the buffer for the given path
	 */
	public List<Milepost> getBufferForPath(String routeId, double desiredDistanceInMiles, List<Milepost> pathMileposts) {
		String url = String.format("%s/cdot-upstream-path/get-buffer-for-path/%s/%s", config.getCvRestService(), routeId, desiredDistanceInMiles);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<List<Milepost>> entity = new HttpEntity<>(pathMileposts, headers);
		ParameterizedTypeReference<List<Milepost>> responseType = new ParameterizedTypeReference<List<Milepost>>() {
		};
		ResponseEntity<List<Milepost>> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.POST,
				entity, responseType);
		return response.getBody();
	}
}
