package com.trihydro.tasks.actions;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.tasks.config.DataTasksConfiguration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CleanupActiveTims implements Runnable {
    private DataTasksConfiguration configuration;
    private ActiveTimService activeTimService;
    private RestTemplateProvider restTemplateProvider;

    @Autowired
    public void InjectDependencies(DataTasksConfiguration configuration, ActiveTimService activeTimService,
                                   RestTemplateProvider restTemplateProvider) {
        this.configuration = configuration;
        this.activeTimService = activeTimService;
        this.restTemplateProvider = restTemplateProvider;
    }

    public void run() {
        log.info("Running...");

        try {
            List<ActiveTim> activeTims = new ArrayList<>();
            List<ActiveTim> tmp = null;

            // select active tims missing ITIS codes
            tmp = activeTimService.getActiveTimsMissingItisCodes();
            if (!tmp.isEmpty()) {
                log.info("Found {} Active TIMs missing ITIS Codes", tmp.size());
                activeTims.addAll(tmp);
            }

            // add active tims that weren't sent to the SDX or any RSUs
            tmp = activeTimService.getActiveTimsNotSent();
            if (!tmp.isEmpty()) {
                log.info("Found {} Active TIMs that weren't sent to the SDX or any RSUs", tmp.size());
                activeTims.addAll(tmp);
            }

            if (activeTims.isEmpty()) {
                log.info("No Active TIMs to cleanup");
            }

            // delete from rsus and the SDX
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = null;
            String activeTimJson;
            Gson gson = new Gson();

            // send to tim type endpoint to delete from RSUs and SDWs
            for (ActiveTim activeTim : activeTims) {
                try {
                    activeTimJson = gson.toJson(activeTim);
                    entity = new HttpEntity<String>(activeTimJson, headers);

                    log.info("CleanupActiveTims - Deleting ActiveTim: { activeTimId: {}, clientId: {} }", activeTim.getActiveTimId(),
                            activeTim.getClientId());
                    ResponseEntity<String> response = restTemplateProvider.GetRestTemplate()
                            .exchange(configuration.getWrapperUrl() + "/delete-tim/", HttpMethod.DELETE, entity, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Successfully deleted ActiveTim: { activeTimId: {}, clientId: {} }", activeTim.getActiveTimId(),
                                activeTim.getClientId());
                    } else {
                        log.warn("Failed to delete ActiveTim: { activeTimId: {}, clientId: {} }, response status: {}, response body: {}",
                                activeTim.getActiveTimId(), activeTim.getClientId(), response.getStatusCode(), response.getBody());
                    }
                }
                catch (Exception e) {
                    log.error("Exception deleting ActiveTim: { activeTimId: {}, clientId: {} }, error: {}", activeTim.getActiveTimId(),
                            activeTim.getClientId(), e.getMessage());
                }

            }
        } catch (Exception e) {
            log.error("Unexpected error occurred while processing expired Active TIMs", e);
            // don't rethrow error, or the task won't be reran until the service is
            // restarted.
        }
    }
}