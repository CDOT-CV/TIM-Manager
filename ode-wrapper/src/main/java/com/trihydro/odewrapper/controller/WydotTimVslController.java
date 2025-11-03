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
import com.trihydro.odewrapper.model.TimVslList;
import com.trihydro.odewrapper.model.WydotTimVsl;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@CrossOrigin
@RestController
@Api(description = "Variable Speed Limits")
public class WydotTimVslController extends WydotTimBaseController implements BufferTimFactory {

    private final String type = "VSL";
    List<WydotTim> timsToSend = new ArrayList<>();

    @Autowired
    public WydotTimVslController(BasicConfiguration _basicConfiguration, WydotTimService _wydotTimService,
            TimTypeService _timTypeService, SetItisCodes _setItisCodes, ActiveTimService _activeTimService,
            RestTemplateProvider _restTemplateProvider, MilepostReduction _milepostReduction, Utility _utility,
            TimGenerationHelper _timGenerationHelper, MilepostService _milepostService, IdenticalPointsExceptionHandler identicalPointsExceptionHandler) {
        super(_basicConfiguration, _wydotTimService, _timTypeService, _setItisCodes, _activeTimService,
                _restTemplateProvider, _milepostReduction, _utility, _timGenerationHelper, _milepostService, identicalPointsExceptionHandler);
    }

    @RequestMapping(value = "/vsl-tim", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<String> createUpdateVslTim(@RequestBody TimVslList timVslList) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        utility.logWithDate(dateFormat.format(date) + " - Create/Update VSL TIM", this.getClass());
        String post = gson.toJson(timVslList);
        utility.logWithDate(post, this.getClass());

        List<ControllerResult> resultList = new ArrayList<>();
        ControllerResult resultTim;

        // build TIM
        for (WydotTimVsl wydotTim : timVslList.getTimVslList()) {
            resultTim = validateInputVsl(wydotTim);

            if (!resultTim.getResultMessages().isEmpty()) {
                resultList.add(resultTim);
                continue;
            }

            if (wydotTim.getDirection().equalsIgnoreCase("i")) {
                timsToSend.addAll(makeIncreasingTims(wydotTim, bufferTimITISCodes, milepostService));
            }
            else {
                timsToSend.addAll(makeDecreasingTims(wydotTim, bufferTimITISCodes, milepostService));
            }

            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }

        processRequestAsync(timsToSend);
        String responseMessage = gson.toJson(resultList);
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    public void processRequestAsync(List<WydotTim> wydotTims) {
        // An Async task always executes in new thread
        new Thread(() -> {
            var startTime = getStartTime();
            for (WydotTim tim : wydotTims) {
                processRequest(tim, getTimType(type), startTime, null, null);
            }
        }).start();
    }

    @RequestMapping(value = "/vsl-tim/{vslTimId}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<String> deleteVslTim(@PathVariable String vslTimId) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        utility.logWithDate(dateFormat.format(date) + " - Delete VSL TIM", this.getClass());

        // expire and clear TIM
        wydotTimService.clearTimsById("VSL", vslTimId, null, true);

        String responseMessage = "success";
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    @RequestMapping(value = "/vsl-tim", method = RequestMethod.GET, headers = "Accept=application/json")
    public Collection<ActiveTim> getVslTims() {

        // get active TIMs
        List<ActiveTim> activeTims = wydotTimService.selectTimsByType(type);

        // add ITIS codes to TIMs
        for (ActiveTim activeTim : activeTims) {
            activeTimService.addItisCodesToActiveTim(activeTim);
        }

        return activeTims;
    }

}
