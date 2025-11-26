package com.trihydro.odewrapper.controller;

import com.trihydro.library.exceptionhandlers.IdenticalPointsExceptionHandler;
import com.trihydro.library.helpers.DateTimeHelper;
import com.trihydro.library.helpers.MilepostReduction;
import com.trihydro.library.helpers.TimGenerationHelper;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.*;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.library.service.TimTypeService;
import com.trihydro.library.service.WydotTimService;
import com.trihydro.odewrapper.config.BasicConfiguration;
import com.trihydro.odewrapper.factory.BufferTimFactory;
import com.trihydro.odewrapper.helpers.SetItisCodes;
import com.trihydro.odewrapper.model.ControllerResult;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@CrossOrigin
@RestController
@Slf4j
@Api(description = "Road Construction")
public class WydotTimRwController extends WydotTimBaseController implements BufferTimFactory {

    private final String type = "RW";
    List<WydotTim> timsToSend;

    @Autowired
    public WydotTimRwController(BasicConfiguration _basicConfiguration, WydotTimService _wydotTimService,
                                TimTypeService _timTypeService, SetItisCodes _setItisCodes, ActiveTimService _activeTimService,
                                RestTemplateProvider _restTemplateProvider, MilepostReduction _milepostReduction, Utility _utility,
                                TimGenerationHelper _timGenerationHelper, MilepostService _milepostService, IdenticalPointsExceptionHandler identicalPointsExceptionHandler,
                                DateTimeHelper dateTimeHelper) {
        super(_basicConfiguration, _wydotTimService, _timTypeService, _setItisCodes, _activeTimService,
                _restTemplateProvider, _milepostReduction, _utility, _timGenerationHelper, _milepostService, identicalPointsExceptionHandler, dateTimeHelper);
    }

    @RequestMapping(value = "/rw-tim", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<String> createRoadContructionTim(@RequestBody TimRwList timRwList) {
        log.info("{}: " + "Create/Update RW TIM", this.getClass().getSimpleName());
        String post = gson.toJson(timRwList);
        log.info("{}: {}", this.getClass().getSimpleName(), post.toString());

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

            if (wydotTim.getDirection().equalsIgnoreCase("b")) {
                // if bi-directional, make both increasing and decreasing TIMs
                timsToSend.addAll(makeIncreasingTims(wydotTim, bufferTimITISCodes, milepostService));
                timsToSend.addAll(makeDecreasingTims(wydotTim, bufferTimITISCodes, milepostService));
            } else if (wydotTim.getDirection().equalsIgnoreCase("i")) {
                timsToSend.addAll(makeIncreasingTims(wydotTim, bufferTimITISCodes, milepostService));
            } else {
                timsToSend.addAll(makeDecreasingTims(wydotTim, bufferTimITISCodes, milepostService));
            }

            // compile result messages for user
            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }

        processRequestAsync();

        String responseMessage = gson.toJson(resultList);
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    private WydotTimRw copyTimWithStartDate(WydotTimRw wydotTim) {
        WydotTimRw timOneWay = wydotTim.copy();
        if (StringUtils.isBlank(timOneWay.getSchedStart())) {
            String startTime = getStartTime();
            timOneWay.setSchedStart(startTime);
        }
        return timOneWay;
    }

    public void processRequestAsync() {
        // An Async task always executes in new thread
        new Thread(() -> {
            for (var tim : timsToSend) {
                WydotTimRw timRw = (WydotTimRw) tim;
                processRequest(timRw, getTimType(type), timRw.getSchedStart(), timRw.getSchedEnd(), null);
            }
        }).start();

    }

    @RequestMapping(value = "/rw-tim/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<String> deleteRoadContructionTim(@PathVariable String id) {
        log.info("{}: " + "Delete RW TIM", this.getClass().getSimpleName());
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
