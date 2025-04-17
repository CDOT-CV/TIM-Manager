package com.trihydro.odewrapper.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.trihydro.library.helpers.MilepostReduction;
import com.trihydro.library.helpers.TimGenerationHelper;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.Buffer;
import com.trihydro.library.model.ContentEnum;
import com.trihydro.library.model.Milepost;
import com.trihydro.library.model.TimRwList;
import com.trihydro.library.model.WydotTimRw;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.library.service.TimTypeService;
import com.trihydro.library.service.WydotTimService;
import com.trihydro.odewrapper.config.BasicConfiguration;
import com.trihydro.odewrapper.helpers.SetItisCodes;
import com.trihydro.odewrapper.model.ControllerResult;

import org.apache.commons.lang3.StringUtils;
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
@Api(description = "Road Construction")
public class WydotTimRwController extends WydotTimBaseController {

    private final String type = "RW";
    List<WydotTimRw> timsToSend;

    @Autowired
    public WydotTimRwController(BasicConfiguration _basicConfiguration, WydotTimService _wydotTimService,
            TimTypeService _timTypeService, SetItisCodes _setItisCodes, ActiveTimService _activeTimService,
            RestTemplateProvider _restTemplateProvider, MilepostReduction _milepostReduction, Utility _utility,
            TimGenerationHelper _timGenerationHelper, MilepostService _milepostService) {
        super(_basicConfiguration, _wydotTimService, _timTypeService, _setItisCodes, _activeTimService,
                _restTemplateProvider, _milepostReduction, _utility, _timGenerationHelper, _milepostService);
    }

    @RequestMapping(value = "/rw-tim", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<String> createRoadContructionTim(@RequestBody TimRwList timRwList) {
        utility.logWithDate("Create/Update RW TIM", this.getClass());
        String post = gson.toJson(timRwList);
        utility.logWithDate(post, this.getClass());

        List<ControllerResult> resultList = new ArrayList<>();
        ControllerResult resultTim;
        timsToSend = new ArrayList<>();

        // build TIM
        for (WydotTimRw wydotTim : timRwList.getTimRwList()) {

            // validate input
            resultTim = validateInputRw(wydotTim);

            // if there are invalidation messages skip to next TIM
            if (!resultTim.getResultMessages().isEmpty()) {
                resultList.add(resultTim);
                continue;
            }

            // if valid

            // sort buffers by distance
            if (wydotTim.getBuffers() != null)
                wydotTim.getBuffers().sort(Comparator.comparingDouble(Buffer::getDistance));

            // if bi-directional
            if (wydotTim.getDirection().equalsIgnoreCase("b")) {
                // make i TIMs
                makeIncreasingTims(wydotTim);
                // make d TIMs
                makeDecreasingTims(wydotTim);
            }
            // else make one direction TIMs
            else if (wydotTim.getDirection().equalsIgnoreCase("i")) {
                makeIncreasingTims(wydotTim);
            }
            else {
                makeDecreasingTims(wydotTim);
            }

            // compile result messages for user
            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }

        processRequestAsync();

        String responseMessage = gson.toJson(resultList);
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    public void makeIncreasingTims(WydotTimRw wydotTim) {
        // i - add buffer for point TIMs
        WydotTimRw timOneWay = copyTimWithStartDate(wydotTim);
        timOneWay.setDirection("I");

        addTimsToSend(timOneWay, false);

        makeBufferTims(timOneWay);
    }

    public void makeDecreasingTims(WydotTimRw wydotTim) {
        // d - add buffer for point TIMs
        WydotTimRw timOneWay = copyTimWithStartDate(wydotTim);
        timOneWay.setDirection("D");

        addTimsToSend(timOneWay, false);

        makeBufferTims(timOneWay);
    }

    private WydotTimRw copyTimWithStartDate(WydotTimRw wydotTim) {
        WydotTimRw timOneWay = wydotTim.copy();
        if (StringUtils.isBlank(timOneWay.getSchedStart())) {
            String startTime = getStartTime();
            timOneWay.setSchedStart(startTime);
        }
        return timOneWay;
    }

    private void addTimsToSend(WydotTimRw tim, boolean isBuffer) {
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

            WydotTimRw timToSend = tim.copy();
            timToSend.setItisCodes(itisCodes);
            String clientIdWithItis = tim.getClientId() + '-' + tim.getDirection() + '-' + itisCodeEntry.replace(' ', '-');
            timToSend.setClientId(clientIdWithItis);
            timsToSend.add(timToSend);
        }
    }

    public void makeBufferTims(WydotTimRw wydotTim) {
        // get mileposts for buffer
        List<Milepost> bufferMps = milepostService.getBufferForPath(wydotTim.getRoute().replace('-', '_'), 1.0, wydotTim.toMileposts());
        wydotTim.setGeometry(milepostToGeometry(bufferMps));
        wydotTim.setClientId(wydotTim.getClientId() + "%BUFF");

        addTimsToSend(wydotTim, true);
    }

    public void processRequestAsync() {
        // An Async task always executes in new thread
        new Thread(() -> {
            for (WydotTimRw tim : timsToSend) {
                // check for reduce speed, itis code 7443
                if (tim.getItisCodes() != null && tim.getItisCodes().size() == 3
                        && tim.getItisCodes().get(0).equals("7443")) {
                    processRequest(tim, getTimType(type), tim.getSchedStart(), tim.getSchedEnd(), null,
                            ContentEnum.speedLimit, TravelerInfoType.advisory);
                } else if (tim.getItisCodes() != null && tim.getItisCodes().get(0).equals("7186")) {
                    // prepare to stop
                    processRequest(tim, getTimType(type), tim.getSchedStart(), tim.getSchedEnd(), null,
                            ContentEnum.advisory, TravelerInfoType.advisory);
                } else {
                    // the rest are content=workZone
                    processRequest(tim, getTimType(type), tim.getSchedStart(), tim.getSchedEnd(), null,
                            ContentEnum.workZone, TravelerInfoType.advisory);
                }
            }
        }).start();

    }

    @RequestMapping(value = "/rw-tim/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<String> deleteRoadContructionTim(@PathVariable String id) {
        utility.logWithDate("Delete RW TIM", this.getClass());
        // expire and clear TIM
        wydotTimService.clearTimsById(type, id, null, true);

        String responseMessage = "success";
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    @RequestMapping(value = "/rw-tim/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public Collection<ActiveTim> getRoadContructionTimById(@PathVariable String id) {

        // get tims
        List<ActiveTim> activeTims = wydotTimService.selectTimByClientId(type, id);
        return activeTims;
    }

    @RequestMapping(value = "/rw-tim/itis-codes/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public Collection<ActiveTim> getRoadContructionTimByIdWithItisCodes(@PathVariable String id) {

        // get tims
        List<ActiveTim> activeTims = wydotTimService.selectTimByClientId(type, id);

        // add ITIS codes to TIMs
        for (ActiveTim activeTim : activeTims) {
            activeTimService.addItisCodesToActiveTim(activeTim);
        }

        return activeTims;
    }

    @RequestMapping(value = "/rw-tim", method = RequestMethod.GET, headers = "Accept=application/json")
    public Collection<ActiveTim> getRoadConstructionTim() {

        // get tims
        List<ActiveTim> activeTims = wydotTimService.selectTimsByType(type);

        return activeTims;
    }

}
