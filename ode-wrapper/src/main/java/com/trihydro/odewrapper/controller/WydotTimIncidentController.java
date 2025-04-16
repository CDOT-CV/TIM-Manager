package com.trihydro.odewrapper.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.trihydro.library.helpers.MilepostReduction;
import com.trihydro.library.helpers.TimGenerationHelper;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.ContentEnum;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.library.service.TimTypeService;
import com.trihydro.library.service.WydotTimService;
import com.trihydro.odewrapper.config.BasicConfiguration;
import com.trihydro.odewrapper.helpers.SetItisCodes;
import com.trihydro.odewrapper.model.ControllerResult;
import com.trihydro.odewrapper.model.TimIncidentList;
import com.trihydro.odewrapper.model.WydotTimIncident;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.trihydro.library.service.MilepostService;

import io.swagger.annotations.Api;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.FrameType.TravelerInfoType;

@CrossOrigin
@RestController
@Api(description = "Incidents")
public class WydotTimIncidentController extends WydotTimBaseController {

    private final String type = "I";
    List<WydotTimIncident> timsToSend = new ArrayList<>();

    @Autowired
    public WydotTimIncidentController(BasicConfiguration _basicConfiguration, WydotTimService _wydotTimService,
            TimTypeService _timTypeService, SetItisCodes _setItisCodes, ActiveTimService _activeTimService,
            RestTemplateProvider _restTemplateProvider, MilepostReduction _milepostReduction, Utility _utility,
            TimGenerationHelper _timGenerationHelper, MilepostService _milepostService) {
        super(_basicConfiguration, _wydotTimService, _timTypeService, _setItisCodes, _activeTimService,
                _restTemplateProvider, _milepostReduction, _utility, _timGenerationHelper, _milepostService);
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
                makeIncreasingTims(wydotTim);
            }
            else {
                makeDecreasingTims(wydotTim);
            }

            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }

        makeTimsAsync(timsToSend);

        String responseMessage = gson.toJson(resultList);
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    public void makeIncreasingTims(WydotTimIncident wydotTim) {
        // i - add buffer for point TIMs
        WydotTimIncident timOneWay = wydotTim.copy();
        timOneWay.setDirection("I");

        addTimsToSend(timOneWay, false);

        makeBufferTims(timOneWay);
    }

    public void makeDecreasingTims(WydotTimIncident wydotTim) {
        // d - add buffer for point TIMs
        WydotTimIncident timOneWay = wydotTim.copy();
        timOneWay.setDirection("D");

        addTimsToSend(timOneWay, false);

        makeBufferTims(timOneWay);
    }

    private void addTimsToSend(WydotTimIncident tim, boolean isBuffer) {
        for (String itisCodeEntry : tim.getItisCodes()) {
            // CTW Update requires that individual TIMs are created for each ITIS ordering
            List<String> itisCodes = Arrays.asList(itisCodeEntry.split(" "));

            // only generate appropriate TIMs for geometry list (buffer, workZone)
            String lastItisCode = itisCodes.get(itisCodes.size() - 1);
            boolean isBufferTim = bufferTimITISCodes.contains(Integer.valueOf(lastItisCode));
            if (isBuffer && !isBufferTim) {
                continue;
            } else if (!isBuffer && isBufferTim) {
                continue;
            }

            WydotTimIncident timToSend = tim.copy();
            timToSend.setItisCodes(itisCodes);
            String clientIdWithItis = tim.getClientId() + '-' + itisCodeEntry.replace(' ', '-');
            timToSend.setClientId(clientIdWithItis);
            timsToSend.add(timToSend);
        }
    }

    public void makeBufferTims(WydotTimIncident wydotTim) {
        // get mileposts for buffer
        List<Milepost> bufferMps = milepostService.getBufferForPath(wydotTim.getRoute().replace('-', '_'), 1.0, wydotTim.toMileposts());
        wydotTim.setGeometry(milepostToGeometry(bufferMps));
        wydotTim.setClientId(wydotTim.getClientId() + "%BUFF");

        addTimsToSend(wydotTim, true);
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

    public void makeTimsAsync(List<WydotTimIncident> wydotTims) {

        new Thread(() -> {
            var startTime = getStartTime();
            for (WydotTimIncident wydotTim : wydotTims) {
                // set route
                wydotTim.setRoute(wydotTim.getRoute());
                processRequest(wydotTim, getTimType(type), startTime, null, wydotTim.getPk(), ContentEnum.advisory,
                        TravelerInfoType.advisory);
            }
        }).start();
    }

    @RequestMapping(value = "/incident-tim/{incidentId}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<String> deleteIncidentTim(@PathVariable String incidentId) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        utility.logWithDate(dateFormat.format(date) + " - Delete Incident TIM", this.getClass());

        // expire and clear TIM
        wydotTimService.clearTimsById("I", incidentId, null, true);

        String responseMessage = "success";
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    @RequestMapping(value = "/incident-tim", method = RequestMethod.GET, headers = "Accept=application/json")
    public Collection<ActiveTim> getIncidentTims() {

        // get active TIMs
        List<ActiveTim> activeTims = wydotTimService.selectTimsByType("I");

        return activeTims;
    }

    @RequestMapping(value = "/incident-tim/{incidentId}", method = RequestMethod.GET, headers = "Accept=application/json")
    public Collection<ActiveTim> getIncidentTimById(@PathVariable String incidentId) {

        // get active TIMs
        List<ActiveTim> activeTims = wydotTimService.selectTimByClientId("I", incidentId);
        return activeTims;
    }
}
