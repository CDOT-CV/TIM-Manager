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
import com.trihydro.library.model.WydotTim;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.MilepostService;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.library.service.TimTypeService;
import com.trihydro.library.service.WydotTimService;
import com.trihydro.odewrapper.config.BasicConfiguration;
import com.trihydro.odewrapper.helpers.SetItisCodes;
import com.trihydro.odewrapper.model.ControllerResult;
import com.trihydro.odewrapper.model.TimVslList;
import com.trihydro.odewrapper.model.WydotTimVsl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import us.dot.its.jpo.ode.plugin.j2735.timstorage.FrameType.TravelerInfoType;

@CrossOrigin
@RestController
@Api(description = "Variable Speed Limits")
public class WydotTimVslController extends WydotTimBaseController {

    private final String type = "VSL";
    List<WydotTim> timsToSend = new ArrayList<>();

    @Autowired
    public WydotTimVslController(BasicConfiguration _basicConfiguration, WydotTimService _wydotTimService,
            TimTypeService _timTypeService, SetItisCodes _setItisCodes, ActiveTimService _activeTimService,
            RestTemplateProvider _restTemplateProvider, MilepostReduction _milepostReduction, Utility _utility,
            TimGenerationHelper _timGenerationHelper, MilepostService _milepostService) {
        super(_basicConfiguration, _wydotTimService, _timTypeService, _setItisCodes, _activeTimService,
                _restTemplateProvider, _milepostReduction, _utility, _timGenerationHelper, _milepostService);
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
                makeIncreasingTims(wydotTim);
            }
            else {
                makeDecreasingTims(wydotTim);
            }

            resultTim.getResultMessages().add("success");
            resultList.add(resultTim);
        }

        processRequestAsync(timsToSend);
        String responseMessage = gson.toJson(resultList);
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }

    public void makeIncreasingTims(WydotTimVsl wydotTim) {
        // i - add buffer for point TIMs
        WydotTimVsl timOneWay = wydotTim.copy();
        timOneWay.setDirection("I");

        addTimsToSend(timOneWay, false);

        makeBufferTims(timOneWay);
    }

    public void makeDecreasingTims(WydotTimVsl wydotTim) {
        // d - add buffer for point TIMs
        WydotTimVsl timOneWay = wydotTim.copy();
        timOneWay.setDirection("D");

        addTimsToSend(timOneWay, false);

        makeBufferTims(timOneWay);
    }

    private void addTimsToSend(WydotTimVsl tim, boolean isBuffer) {
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

            WydotTimVsl timToSend = tim.copy();
            timToSend.setItisCodes(itisCodes);
            var itisCodeAbb = SetItisCodes.getItisCodeAbbreviation(itisCodeEntry);
            String clientIdWithItis = tim.getClientId() + '-' + tim.getDirection() + '-' + itisCodeAbb;
            timToSend.setClientId(clientIdWithItis);
            timsToSend.add(timToSend);
        }
    }

    public void makeBufferTims(WydotTimVsl wydotTim) {
        // get mileposts for buffer
        List<Milepost> bufferMps = milepostService.getBufferForPath(wydotTim.getRoute().replace('-', '_'), 1.0, wydotTim.toMileposts());
        wydotTim.setGeometry(milepostToGeometry(bufferMps));
        wydotTim.setClientId(wydotTim.getClientId() + "%BUFF");

        addTimsToSend(wydotTim, true);
    }

    public void processRequestAsync(List<WydotTim> wydotTims) {
        // An Async task always executes in new thread
        new Thread(() -> {
            var startTime = getStartTime();
            for (WydotTim tim : wydotTims) {
                processRequest(tim, getTimType(type), startTime, null, null, ContentEnum.speedLimit,
                        TravelerInfoType.roadSignage);
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
