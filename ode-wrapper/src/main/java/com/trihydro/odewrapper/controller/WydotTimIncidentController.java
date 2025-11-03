package com.trihydro.odewrapper.controller;

import com.trihydro.library.exceptionhandlers.IdenticalPointsExceptionHandler;
import com.trihydro.library.helpers.MilepostReduction;
import com.trihydro.library.helpers.TimGenerationHelper;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.WydotTim;
import com.trihydro.library.service.*;
import com.trihydro.odewrapper.config.BasicConfiguration;
import com.trihydro.odewrapper.factory.BufferTimFactory;
import com.trihydro.odewrapper.helpers.SetItisCodes;
import com.trihydro.odewrapper.model.ControllerResult;
import com.trihydro.odewrapper.model.TimIncidentList;
import com.trihydro.odewrapper.model.WydotTimIncident;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.FrameType.TravelerInfoType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@CrossOrigin
@RestController
@Api(description = "Incidents")
@Slf4j
public class WydotTimIncidentController extends WydotTimBaseController implements BufferTimFactory {

    private final String type = "I";
    List<WydotTim> timsToSend = new ArrayList<>();

    @Autowired
    public WydotTimIncidentController(BasicConfiguration _basicConfiguration, WydotTimService _wydotTimService,
            TimTypeService _timTypeService, SetItisCodes _setItisCodes, ActiveTimService _activeTimService,
            RestTemplateProvider _restTemplateProvider, MilepostReduction _milepostReduction, Utility _utility,
            TimGenerationHelper _timGenerationHelper, MilepostService _milepostService, IdenticalPointsExceptionHandler identicalPointsExceptionHandler) {
        super(_basicConfiguration, _wydotTimService, _timTypeService, _setItisCodes, _activeTimService,
                _restTemplateProvider, _milepostReduction, _utility, _timGenerationHelper, _milepostService, identicalPointsExceptionHandler);
    }

    @RequestMapping(value = "/incident-tim", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<String> createIncidentTim(@RequestBody TimIncidentList timIncidentList) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        utility.logWithDate(dateFormat.format(date) + " - Create Incident TIM", this.getClass());
        String post = gson.toJson(timIncidentList);
        utility.logWithDate(post, this.getClass());

        List<ControllerResult> resultList = new ArrayList<>();
        ControllerResult resultTim;

    // build TIM
    for (WydotTimIncident wydotTim : timIncidentList.getTimIncidentList()) {

      resultTim = validateInputIncident(wydotTim);

            if (wydotTim.getDirection().equalsIgnoreCase("i")) {
                timsToSend.addAll(makeIncreasingTims(wydotTim, bufferTimITISCodes, milepostService));
            }
            else {
                timsToSend.addAll(makeDecreasingTims(wydotTim, bufferTimITISCodes, milepostService));
            }

      resultTim.getResultMessages().add("success");
      resultList.add(resultTim);
    }

    makeTimsAsync(timsToSend);

    String responseMessage = gson.toJson(resultList);
    return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
  }

    @RequestMapping(value = "/incident-tim", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity<String> updateIncidentTim(@RequestBody TimIncidentList timIncidentList) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        utility.logWithDate(dateFormat.format(date) + " - Update Incident TIM", this.getClass());
        String post = gson.toJson(timIncidentList);
        utility.logWithDate(post, this.getClass());

        List<ControllerResult> resultList = new ArrayList<>();
        ControllerResult resultTim;

        // delete TIMs
        for (WydotTimIncident wydotTim : timIncidentList.getTimIncidentList()) {

            resultTim = validateInputIncident(wydotTim);

            if (!resultTim.getResultMessages().isEmpty()) {
                resultList.add(resultTim);
                continue;
            }

            // make tims
            timsToSend.add(wydotTim);

            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }
        if (!timsToSend.isEmpty()) {
            // make tims, expire existing ones, and send them
            makeTimsAsync(timsToSend);
        }

        String responseMessage = gson.toJson(resultList);
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    public void makeTimsAsync(List<WydotTim> wydotTims) {

        new Thread(() -> {
            var startTime = getStartTime();
            for (var wydotTim : wydotTims) {
                var wydotTimIncident = (WydotTimIncident)wydotTim;
                // set route
                wydotTim.setRoute(wydotTimIncident.getRoute());
                processRequest(wydotTimIncident, getTimType(type), startTime, null, wydotTimIncident.getPk());
            }
        }).start();
    }

  @RequestMapping(value = "/incident-tim/{incidentId}", method = RequestMethod.DELETE, headers = "Accept=application/json")
  public ResponseEntity<String> deleteIncidentTim(@PathVariable String incidentId) {
    log.info("Delete Incident TIM");

        // expire and clear TIM
        wydotTimService.clearTimsById("I", incidentId, null, true);

        String responseMessage = "success";
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

  @RequestMapping(value = "/incident-tim", method = RequestMethod.GET, headers = "Accept=application/json")
  public Collection<ActiveTim> getIncidentTims() {
    // get active TIMs
    return wydotTimService.selectTimsByType("I");
  }

  @RequestMapping(value = "/incident-tim/{incidentId}", method = RequestMethod.GET, headers = "Accept=application/json")
  public Collection<ActiveTim> getIncidentTimById(@PathVariable String incidentId) {
    // get active TIMs
    return wydotTimService.selectTimByClientId("I", incidentId);
  }
}
