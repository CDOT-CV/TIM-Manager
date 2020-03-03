package com.trihydro.tasks.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.trihydro.library.helpers.EmailHelper;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.AdvisorySituationDataDeposit;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.SdwService;
import com.trihydro.tasks.models.CActiveTim;
import com.trihydro.tasks.models.CAdvisorySituationDataDeposit;
import com.trihydro.tasks.models.SdxComparableSorter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidateSDX {
    private EmailHelper mailHelper;
    private SdwService sdwService;
    private ActiveTimService activeTimService;

    @Autowired
    public void InjectDependencies(EmailHelper _mailHelper, SdwService _sdwService,
            ActiveTimService _activeTimService) {
        mailHelper = _mailHelper;
        sdwService = _sdwService;
        activeTimService = _activeTimService;
    }

    public void run() {
        List<CActiveTim> oracleRecords = new ArrayList<>();
        List<CAdvisorySituationDataDeposit> sdxRecords = new ArrayList<>();

        // Fetch records from Oracle
        for (ActiveTim activeTim : activeTimService.getActiveTimsForSDX()) {
            oracleRecords.add(new CActiveTim(activeTim));
        }

        // Fetch records from SDX
        for (AdvisorySituationDataDeposit asdd : sdwService.getAllSdwRecords()) {
            sdxRecords.add(new CAdvisorySituationDataDeposit(asdd));
        }

        Collections.sort(oracleRecords, new SdxComparableSorter());
        Collections.sort(sdxRecords, new SdxComparableSorter());

        // Actions to perform
        List<CActiveTim> toResend = new ArrayList<CActiveTim>();
        List<CAdvisorySituationDataDeposit> deleteFromSdx = new ArrayList<CAdvisorySituationDataDeposit>();

        // Metrics to collect
        List<CActiveTim> invOracleRecords = new ArrayList<CActiveTim>();
        int numSdxOrphanedRecords = 0;
        int numOutdatedSdxRecords = 0;
        int numRecordsNotOnSdx = 0;

        int i = 0;
        int j = 0;

        while (i < oracleRecords.size() || j < sdxRecords.size()) {

            // If either list is at the end, push the remainder of the other list onto their
            // corresponding action
            if (i == oracleRecords.size()) {
                // Any remaining sdx records don't have a corresponding oracle record
                deleteFromSdx.addAll(sdxRecords.subList(j, sdxRecords.size() - 1));
                j = sdxRecords.size();
                continue;
            }
            if (j == sdxRecords.size()) {
                // Any remaining oracle records don't have a corresponding sdx record
                toResend.addAll(oracleRecords.subList(i, oracleRecords.size() - 1));
                i = oracleRecords.size();
                continue;
            }

            CActiveTim oracleRecord = oracleRecords.get(i);
            CAdvisorySituationDataDeposit sdxRecord = sdxRecords.get(j);
            Integer sdxRecordId = sdxRecord.getRecordId();
            Integer oracleRecordId = oracleRecord.getRecordId();

            // If the SAT_RECORD_ID string isn't valid hex, the SDX will reject the record.
            // Push onto invOracleRecords
            if (oracleRecordId == null) {
                invOracleRecords.add(oracleRecord);
                i++;
                continue;
            }

            if (oracleRecordId == sdxRecordId) {
                // make sure the messages are the same
                if (!sameItisCodes(oracleRecord.getItisCodes(), sdxRecord.getItisCodes())) {
                    numOutdatedSdxRecords++;
                    toResend.add(oracleRecord);
                }
            } else if (oracleRecordId > sdxRecordId) {
                // The current SDX record doesn't have a corresponding Oracle record...
                numSdxOrphanedRecords++;
                deleteFromSdx.add(sdxRecord);
                j++;
            } else {
                // The current Oracle record doesn't have a corresponding SDX record...
                numRecordsNotOnSdx++;
                toResend.add(oracleRecord);
                i++;
            }
        }
    }

    private boolean sameItisCodes(List<Integer> o1, List<Integer> o2) {
        boolean result = true;

        if (o1 == null || o2 == null || o1.size() != o2.size()) {
            result = false;
        } else {
            for (int i = 0; i < o1.size(); i++) {
                boolean inBoth = false;

                for (int j = 0; j < o2.size(); j++) {
                    if (o1.get(i) == o2.get(j)) {
                        inBoth = true;
                        break;
                    }
                }

                if (!inBoth) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }
}