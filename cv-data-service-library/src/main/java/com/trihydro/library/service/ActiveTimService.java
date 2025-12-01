package com.trihydro.library.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.trihydro.library.model.ActiveRsuTimQueryModel;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.TimUpdateModel;
import com.trihydro.library.model.WydotTim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ActiveTimService extends CvDataServiceLibrary {

	public Boolean updateActiveTim_SatRecordId(Long activeTimId, String satRecordId) {
		String url = String.format("%s/active-tim/update-sat-record-id/%d/%s", config.getCvRestService(), activeTimId,
				satRecordId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(null, headers);
		ResponseEntity<Boolean> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.PUT, entity,
				Boolean.class);
		return response.getBody();
	}

    public void addItisCodesToActiveTim(ActiveTim activeTim) {
        String url = String.format("%s/active-tim/itis-codes/%d", config.getCvRestService(), activeTim.getActiveTimId());
        ResponseEntity<Integer[]> response = restTemplateProvider.GetRestTemplate().getForEntity(url, Integer[].class);
        activeTim.setItisCodes(Arrays.asList(response.getBody()));
    }

    public boolean deleteActiveTim(Long activeTimId) {
        log.trace("Deleting Active TIM with ID: {}", activeTimId);
        String url = String.format("%s/active-tim/delete-id/%d", config.getCvRestService(), activeTimId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        ResponseEntity<Boolean> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.DELETE, entity, Boolean.class);
        return response.getBody();
    }

    public boolean deleteActiveTimsById(List<Long> activeTimIds) {
        log.trace("Deleting Active TIMs with IDs: {}", activeTimIds);
        String url = String.format("%s/active-tim/delete-ids", config.getCvRestService());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<Long>> entity = new HttpEntity<List<Long>>(activeTimIds, headers);
        ResponseEntity<Boolean> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.DELETE, entity, Boolean.class);
        return response.getBody();
    }

    // utility
    public List<Integer> getActiveTimIndicesByRsu(String rsuTarget) {
        String url = String.format("%s/active-tim/indices-rsu/%s", config.getCvRestService(), rsuTarget);
        ResponseEntity<Integer[]> response = restTemplateProvider.GetRestTemplate().getForEntity(url, Integer[].class);
        return Arrays.asList(response.getBody());
    }

    public List<ActiveTim> getActiveTimsById(List<Long> aTimIds) {
        String url = String.format("%s/active-tim/get-by-ids", config.getCvRestService());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<Long>> entity = new HttpEntity<List<Long>>(aTimIds, headers);
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.POST, entity, ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    public List<ActiveTim> getActiveTimsByWydotTim(List<? extends WydotTim> wydotTims, Long timTypeId) {
        String url = String.format("%s/active-tim/get-by-wydot-tim/%d", config.getCvRestService(), timTypeId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<? extends WydotTim>> entity = new HttpEntity<List<? extends WydotTim>>(wydotTims, headers);
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.POST, entity, ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    // get Active TIMs by client ID direction
    public List<ActiveTim> getActiveTimsByClientIdDirection(String clientId, Long timTypeId, String direction) {
        String url = String.format("%s/active-tim/client-id-direction/%s/%d", config.getCvRestService(), clientId, timTypeId);
        log.debug("Getting active TIMs for client ID: {} and direction: {} from URL: {}", clientId, direction, url);
		if (direction != null) {
			url += "/" + direction;
		}
		List<ActiveTim> activeTims = new ArrayList<>();
		try {
			ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().getForEntity(url,
					ActiveTim[].class);
			if (response != null && response.getBody() != null) {
				activeTims = Arrays.asList(response.getBody());
			}
		} catch (Exception e) {
			log.error("Error getting active TIMs for client ID: {} and direction: {} from URL: {}", clientId,
					direction, url, e);
		}
		log.debug("Number of active TIMs found: {}", activeTims.size());
		return activeTims;
	}

    // get buffers for RW TIMs
    public List<ActiveTim> getBufferTimsByClientId(String clientId) {
        String url = String.format("%s/active-tim/buffer-tims/%s", config.getCvRestService(), clientId);
        log.debug("Getting buffer TIMs for client ID: {} from URL: {}", clientId, url);
		List<ActiveTim> activeTims = new ArrayList<>();
		try {
			ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().getForEntity(url, ActiveTim[].class);
			if (response != null && response.getBody() != null) {
				activeTims = Arrays.asList(response.getBody());
			}
		} catch (Exception e) {
			log.error("Error getting buffer TIMs for client ID: {} from URL: {}", clientId, url, e);
		}
		return activeTims;
	}

    public List<ActiveTim> getExpiredActiveTims(int limit) {
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().getForEntity(config.getCvRestService() + "/active-tim/expired?limit=" + limit, ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    // for GETs
    public List<ActiveTim> getActivesTimByType(Long timTypeId) {
        String url = String.format("%s/active-tim/tim-type-id/%d", config.getCvRestService(), timTypeId);
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().getForEntity(url, ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    // get Active TIMs by client ID direction
    public ActiveTim getActiveRsuTim(ActiveRsuTimQueryModel artqm) {
        String url = String.format("%s/active-tim/active-rsu-tim", config.getCvRestService());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ActiveRsuTimQueryModel> entity = new HttpEntity<ActiveRsuTimQueryModel>(artqm, headers);
        ResponseEntity<ActiveTim> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.POST, entity, ActiveTim.class);
        return response.getBody();
    }

    /**
     * Fetch all ActiveTims for RSUs from cv-data-controller
     *
     * @return List of ActiveTims (including RSU address and index)
     */
    public List<ActiveTim> getActiveRsuTims() {
        return getActiveRsuTims(config.getCvRestService());
    }

    /**
     * Fetch all ActiveTims for RSUs from the specified endpoint
     *
     * @return List of ActiveTims (including RSU address and index)
     */
    public List<ActiveTim> getActiveRsuTims(String endpoint) {
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().getForEntity(endpoint + "/active-tim/active-rsu-tims", ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    /**
     * Calls out to the cv-data-controller REST function to fetch expiring TIMs
     *
     * @return List of TimUpdateModel representing all TIMs expiring within 1 day
     */
    public List<TimUpdateModel> getExpiringActiveTims() {
        ResponseEntity<TimUpdateModel[]> response = restTemplateProvider.GetRestTemplate().getForEntity(config.getCvRestService() + "/active-tim/expiring", TimUpdateModel[].class);
        return Arrays.asList(response.getBody());
    }

    public List<ActiveTim> getActiveTimsMissingItisCodes() {
        ResponseEntity<TimUpdateModel[]> response = restTemplateProvider.GetRestTemplate().getForEntity(config.getCvRestService() + "/active-tim/missing-itis", TimUpdateModel[].class);
        return Arrays.asList(response.getBody());
    }

    public List<ActiveTim> getActiveTimsNotSent() {
        ResponseEntity<TimUpdateModel[]> response = restTemplateProvider.GetRestTemplate().getForEntity(config.getCvRestService() + "/active-tim/not-sent", TimUpdateModel[].class);
        return Arrays.asList(response.getBody());
    }

    public List<ActiveTim> getActiveTimsForSDX() {
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().getForEntity(config.getCvRestService() + "/active-tim/all-sdx", ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    public List<ActiveTim> getActiveTimsWithItisCodes(boolean excludeVslAndParking) {
        ResponseEntity<ActiveTim[]> response =
            restTemplateProvider.GetRestTemplate().getForEntity(config.getCvRestService() + "/active-tim/all-with-itis?excludeVslAndParking=" + excludeVslAndParking, ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    public Boolean updateActiveTimExpiration(String packetID, String expDate) {
        String url = String.format("%s/active-tim/update-expiration/%s/%s", config.getCvRestService(), packetID, expDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        ResponseEntity<Boolean> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.PUT, entity, Boolean.class);
        return response.getBody();
    }

    public String getMinExpiration(String packetID, String expDate) {
        /// get-min-expiration/{packetID}/{startDate}/{expDate}
        String url = String.format("%s/active-tim/get-min-expiration/%s/%s", config.getCvRestService(), packetID, expDate);
        ResponseEntity<String> response = restTemplateProvider.GetRestTemplate().getForEntity(url, String.class);
        return response.getBody();
    }

    public TimUpdateModel getUpdateModelFromActiveTimId(Long activeTimId) {
        String url = String.format("%s/active-tim/update-model/%d", config.getCvRestService(), activeTimId);
        log.debug("Getting update model for active TIM ID: {} from URL: {}", activeTimId, url);
        TimUpdateModel timUpdateModel = null;
        try {
          ResponseEntity<TimUpdateModel> response = restTemplateProvider.GetRestTemplate().getForEntity(url, TimUpdateModel.class);
          timUpdateModel = response.getBody();
        } catch (Exception e) {
          log.error("Error getting update model for active TIM ID: {} from URL: {}", activeTimId, url, e);
        }
        log.debug("Update model found: {}", timUpdateModel);
        return timUpdateModel;
	}

    public boolean resetActiveTimsExpirationDate(List<Long> activeTimIds) {
        String url = String.format("%s/active-tim/reset-expiration-date", config.getCvRestService());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<Long>> entity = new HttpEntity<List<Long>>(activeTimIds, headers);
        ResponseEntity<Boolean> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.PUT, entity, Boolean.class);
        return response.getBody();
    }

	public void markForDeletion(Long activeTimId) {
		String url = String.format("%s/active-tim/mark-for-deletion/%d", config.getCvRestService(), activeTimId);
		log.debug("Marking active TIM for deletion: {} from URL: {}", activeTimId, url);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(null, headers);
		try {
			ResponseEntity<Boolean> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.PUT, entity,
					Boolean.class);
			if (response.getBody() == null || !response.getBody()) {
				log.error("Failed to mark active TIM for deletion: {}", activeTimId);
			}
		} catch (Exception e) {
			log.error("Error marking active TIM for deletion: {} from URL: {}", activeTimId, url, e);
		}
	}

    public List<ActiveTim> getAllRecords() {
        String url = String.format("%s/active-tim/all", config.getCvRestService());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.GET, entity, ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }

    public List<ActiveTim> getActivePlannedConditionTims() {
        String url = String.format("%s/active-tim/get-active-planned-condition-tims", config.getCvRestService());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<ActiveTim[]> response = restTemplateProvider.GetRestTemplate().exchange(url, HttpMethod.GET, entity, ActiveTim[].class);
        return Arrays.asList(response.getBody());
    }
}