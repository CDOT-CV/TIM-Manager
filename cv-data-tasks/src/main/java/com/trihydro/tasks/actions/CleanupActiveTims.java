package com.trihydro.tasks.actions;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.tasks.config.DataTasksConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.trihydro.library.service.MilepostService;

@Component
public class CleanupActiveTims implements Runnable {
    private DataTasksConfiguration configuration;
    private Utility utility;
    private ActiveTimService activeTimService;
    private RestTemplateProvider restTemplateProvider;
    private MilepostService milepostService;

    @Autowired
    public void InjectDependencies(DataTasksConfiguration _configuration, Utility _utility,
            ActiveTimService _activeTimService, RestTemplateProvider _restTemplateProvider, MilepostService _milepostService) {
        configuration = _configuration;
        utility = _utility;
        activeTimService = _activeTimService;
        restTemplateProvider = _restTemplateProvider;
        milepostService = _milepostService;
    }

    @Override
    public void run() {
        utility.logWithDate("Running...", this.getClass());

        try {
            List<ActiveTim> activeTims = new ArrayList<>();
            List<ActiveTim> tmp;

            // select active tims missing ITIS codes
            tmp = activeTimService.getActiveTimsMissingItisCodes();
            if (tmp.isEmpty()) {
            } else {
                utility.logWithDate("Found " + tmp.size() + " Active TIMs missing ITIS Codes", this.getClass());
                activeTims.addAll(tmp);
            }

            // add active tims that weren't sent to the SDX or any RSUs
            tmp = activeTimService.getActiveTimsNotSent();
            if (!tmp.isEmpty()) {
                utility.logWithDate("Found " + tmp.size() + " Active TIMs that weren't distributed", this.getClass());
                activeTims.addAll(tmp);
            }

            if (!activeTims.isEmpty()) {
                utility.logWithDate("Found 0 Active TIMs", this.getClass());
            }

            // delete from rsus and the SDX
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity;
            String activeTimJson;
            Gson gson = new Gson();

            // send to tim type endpoint to delete from RSUs and SDWs
            for (ActiveTim activeTim : activeTims) {

                activeTimJson = gson.toJson(activeTim);
                entity = new HttpEntity<>(activeTimJson, headers);

                utility.logWithDate(
                        "CleanupActiveTims - Deleting ActiveTim: { activeTimId: " + activeTim.getActiveTimId() + " }",
                        this.getClass());
                restTemplateProvider.GetRestTemplate().exchange(configuration.getWrapperUrl() + "/delete-tim/",
                        HttpMethod.DELETE, entity, String.class);
            }

            // clear the milepostService cache to ensure that the milepost data is up to date
            utility.logWithDate("Deleting milepost cache", this.getClass());
            milepostService.clearMilepostCache();
        } catch (Exception e) {
            e.printStackTrace();
            // don't rethrow error, or the task won't be reran until the service is
            // restarted.
        }
    }
}