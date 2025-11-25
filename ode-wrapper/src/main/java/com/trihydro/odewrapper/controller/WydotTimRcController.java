package com.trihydro.odewrapper.controller;

import com.trihydro.library.exceptionhandlers.IdenticalPointsExceptionHandler;
import com.trihydro.library.helpers.DateTimeHelper;
import com.trihydro.library.helpers.MilepostReduction;
import com.trihydro.library.helpers.TimGenerationHelper;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.ContentEnum;
import com.trihydro.library.model.ResubmitTimException;
import com.trihydro.library.model.WydotTim;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.library.service.TimTypeService;
import com.trihydro.library.service.WydotTimService;
import com.trihydro.odewrapper.config.BasicConfiguration;
import com.trihydro.odewrapper.helpers.SetItisCodes;
import com.trihydro.odewrapper.model.ControllerResult;
import com.trihydro.odewrapper.model.TimRcList;
import com.trihydro.odewrapper.model.WydotTimRc;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.FrameType.TravelerInfoType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CrossOrigin
@RestController
@Api(description = "Road Conditions")
@Slf4j
public class WydotTimRcController extends WydotTimBaseController {

    private final String type = "RC";
    protected static BasicConfiguration configuration;
    private final DateTimeHelper dateTimeHelper;

    @Autowired
    public WydotTimRcController(BasicConfiguration _basicConfiguration, WydotTimService _wydotTimService,
                                TimTypeService _timTypeService, SetItisCodes _setItisCodes, ActiveTimService _activeTimService,
                                RestTemplateProvider _restTemplateProvider, MilepostReduction _milepostReduction, Utility _utility,
                                TimGenerationHelper _timGenerationHelper, IdenticalPointsExceptionHandler identicalPointsExceptionHandler,
                                DateTimeHelper dateTimeHelper) {
        super(_basicConfiguration, _wydotTimService, _timTypeService, _setItisCodes, _activeTimService,
                _restTemplateProvider, _milepostReduction, _utility, _timGenerationHelper, identicalPointsExceptionHandler, dateTimeHelper);
        configuration = _basicConfiguration;
        this.dateTimeHelper = dateTimeHelper;
    }

    @RequestMapping(value = "/create-update-rc-tim", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<String> createUpdateRoadConditionsTim(@RequestBody TimRcList timRcList) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        String msg1 = dateFormat.format(date) + " - Create Update RC TIM";
        log.info("{}: {}", this.getClass().getSimpleName(), msg1);
        String post = gson.toJson(timRcList);
        log.info("{}: {}", this.getClass().getSimpleName(), post.toString());

        List<ControllerResult> resultList = new ArrayList<ControllerResult>();
        List<ControllerResult> errList = new ArrayList<ControllerResult>();
        ControllerResult resultTim = null;
        List<WydotTim> timsToSend = new ArrayList<WydotTim>();

        // build TIM
        for (WydotTimRc wydotTim : timRcList.getTimRcList()) {

            resultTim = validateInputRc(wydotTim);

            if (resultTim.getResultMessages().size() > 0) {
                resultList.add(resultTim);
                errList.add(resultTim);
                continue;
            }

            // add TIM to list for processing later
            timsToSend.add(wydotTim);

            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }

        processRequestAsync(timsToSend);

        String responseMessage = gson.toJson(resultList);
        if (errList.size() > 0) {
            String msg = "Failed to send TIMs: " + gson.toJson(errList);
            log.info("{}: {}", this.getClass().getSimpleName(), msg);
        }
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    @RequestMapping(value = "/submit-rc-ac", method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity<String> submitAllClearRoadConditionsTim(@RequestBody TimRcList timRcList) {
        List<ControllerResult> resultList = new ArrayList<ControllerResult>();

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        log.info("All Clear - {}", dateFormat.format(date));
        String post = gson.toJson(timRcList);
        log.debug("Request payload: {}", post);

        List<ControllerResult> errList = new ArrayList<ControllerResult>();
        ControllerResult resultTim = null;
        List<Long> existingTimIds = new ArrayList<Long>();

        log.info("Starting all-clear TIM processing for {} TIMs", timRcList.getTimRcList().size());

        for (WydotTimRc wydotTim : timRcList.getTimRcList()) {
            resultTim = validateRcAc(wydotTim);
            if (resultTim.getResultMessages().size() > 0) {
                log.warn("Validation failed for TIM with clientId: {}, Direction: {}", wydotTim.getClientId(), wydotTim.getDirection());
                log.warn("Validation messages: {}", String.join(", ", resultTim.getResultMessages()));
                resultList.add(resultTim);
                errList.add(resultTim);
                continue;
            }

            // get existing active tims from wydotTim
            var timType = getTimType(type);
            Long timTypeId = timType != null ? timType.getTimTypeId() : null;
            log.debug("Looking for active TIMs with clientId: {}, timTypeId: {}, direction: {}", wydotTim.getClientId(), timTypeId,
                wydotTim.getDirection());

            List<ActiveTim> existingActiveTims = new ArrayList<>();
            var direction = wydotTim.getDirection().toUpperCase();

            // 'B' TIMs are split it into 'I' and 'D' so they should be handled separately
            if (!direction.equals("B")) {
                existingActiveTims = activeTimService.getActiveTimsByClientIdDirection(wydotTim.getClientId(), timTypeId, wydotTim.getDirection());
            } else {
                existingActiveTims = activeTimService.getActiveTimsByClientIdDirection(wydotTim.getClientId(), timTypeId, null);
            }

            log.info("Found {} active TIMs for clientId: {}, direction: {}", existingActiveTims.size(), wydotTim.getClientId(),
                wydotTim.getDirection());

            // get ids from existingActiveTims
            for (ActiveTim existingActiveTim : existingActiveTims) {
                existingTimIds.add(existingActiveTim.getActiveTimId());
                log.debug("Added ActiveTimId to delete list: {}", existingActiveTim.getActiveTimId());
            }
            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }

        // Expire existing tims
        if (existingTimIds.size() > 0) {
            log.info("Attempting to expire {} TIMs: {}", existingTimIds.size(), existingTimIds);
            try {
                List<ResubmitTimException> exceptions = timGenerationHelper.expireTimAndResubmitToOde(existingTimIds);
                if (exceptions != null && !exceptions.isEmpty()) {
                    log.warn("Encountered {} exceptions while expiring TIMs", exceptions.size());
                    for (ResubmitTimException ex : exceptions) {
                        log.warn("Failed to expire TIM {}: {}", ex.getActiveTimId(), ex.getExceptionMessage());
                    }
                } else {
                    log.info("Successfully requested expiration for all {} TIMs", existingTimIds.size());
                }
            } catch (Exception e) {
                log.error("Unexpected error during TIM expiration", e);
            }
        } else {
            log.warn("No active TIMs found to expire - this may indicate an unnecessary all-clear request");
        }

        String responseMessage = gson.toJson(resultList);
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    public void processRequestAsync(List<WydotTim> wydotTims) {
        // An Async task always executes in new thread
        new Thread(new Runnable() {
            public void run() {
                var startTime = getStartTime();
                for (WydotTim tim : wydotTims) {
                    processRequest(tim, getTimType(type), startTime, null, null, ContentEnum.advisory,
                            TravelerInfoType.advisory);
                }
            }
        }).start();
    }
}
