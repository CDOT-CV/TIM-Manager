package com.trihydro.odewrapper.controller;

import org.springframework.web.bind.annotation.RestController;

import com.trihydro.odewrapper.model.WydotTimRcList;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.Arrays;
import com.trihydro.odewrapper.service.WydotTimRcService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@CrossOrigin
@RestController
@Api(description="Road Conditions")
public class WydotTimRcController extends WydotTimBaseController {

    // services   
    private final WydotTimRcService wydotTimRcService;

    @Autowired
    WydotTimRcController(WydotTimRcService wydotTimRcService) {
        this.wydotTimRcService = wydotTimRcService;
    }
   
    @RequestMapping(value="/create-update-rc-tim", method = RequestMethod.POST, headers="Accept=application/json")    
    public ResponseEntity<String> createRcTim(@RequestBody WydotTimRcList wydotTimRcs) { 
       
        System.out.println("Create RC TIM");

        // build TIM
        ArrayList<Long> activeTimIds = wydotTimRcService.createRcTim(wydotTimRcs.getTimRcList());        

        Long[] activeTimIdsArr = new Long[activeTimIds.size()];
        activeTimIds.toArray(activeTimIdsArr);

        return ResponseEntity.status(HttpStatus.OK).body(jsonKeyValue("active_tims", Arrays.toString(activeTimIdsArr)));           
    }

    @RequestMapping(value="/submit-rc-ac", method = RequestMethod.PUT, headers="Accept=application/json")
    public ResponseEntity<String> submitAcRcTim(@RequestBody WydotTimRcList wydotTimRcs) { 
       
        System.out.println("All clear");

        // clear TIM
        wydotTimRcService.allClear(wydotTimRcs.getTimRcList());        

        String responseMessage = "success";
        return ResponseEntity.status(HttpStatus.OK).body(responseMessage);
    }
}
