package com.trihydro.tasks.actions;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.trihydro.library.helpers.EmailHelper;
import com.trihydro.library.helpers.TimGenerationHelper;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.AdvisorySituationDataDeposit;
import com.trihydro.library.model.ResubmitTimException;
import com.trihydro.library.model.SemiDialogID;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.SdwService;
import com.trihydro.tasks.config.DataTasksConfiguration;
import com.trihydro.tasks.helpers.EmailFormatter;
import com.trihydro.tasks.models.CActiveTim;
import com.trihydro.tasks.models.CAdvisorySituationDataDeposit;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ValidateSdx implements Runnable {
    private final DataTasksConfiguration config;
    private final SdwService sdwService;
    private final ActiveTimService activeTimService;
    private final EmailHelper mailHelper;
    private final EmailFormatter emailFormatter;
    private final TimGenerationHelper timGenerationHelper;

    public void run() {
        log.info("Running SDX validation task...");

        try {
            identifyAndReconcileDiscrepancies();
        } catch (Exception ex) {
            log.error("Exception during SDX validation", ex);
            // don't rethrow error, or the task won't be reran until the service is
            // restarted.
        }
    }

    private void identifyAndReconcileDiscrepancies() throws SdwService.SdwServiceException, MessagingException {
        // list of invalid records found in the database (missing or invalid sat_record_id)
        List<CActiveTim> invalidDbRecords = new ArrayList<>();

        // fetch all active tim records with sat_record_id
        log.info("Fetching active TIMs from database for SDX validation");
        List<ActiveTim> activeTims = activeTimService.getActiveTimsForSDX();
        HashMap<Integer, CActiveTim> activeTimMap = new HashMap<>();
        for (ActiveTim activeTim : activeTims) {
            var cActiveTim = new CActiveTim(activeTim);
            var recordId = cActiveTim.getRecordId();
            // Note: since getRecordId fails to parse integers silently, we must handle the null case instead
            // as it represents either a missing (sat)recordId OR an invalid value
            if (recordId == null) {
                log.warn("Database record {} has no recordId", activeTim.getSatRecordId());
                invalidDbRecords.add(cActiveTim);
                continue;
            }
            activeTimMap.put(recordId, cActiveTim);
        }
        // note: we only care about satellite records (sat_record_id is NOT null)

        // get every TIM in the ASD Data Deposit format from the SDX
        // for each tim
        // ask SDX to decode each message so we can access the ITIS codes
        // create a CAdvisorySituationDataDeposit record
        log.info("Fetching AdvisorySituationDataDeposit records from SDX");
        List<AdvisorySituationDataDeposit> sdxRecords = sdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);
        HashMap<Integer, CAdvisorySituationDataDeposit> sdxRecordMap = new HashMap<>();
        for (AdvisorySituationDataDeposit sdxRecord : sdxRecords) {
            List<Integer> itisCodes = new ArrayList<>();
            try {
                itisCodes = sdwService.getItisCodesFromAdvisoryMessage(sdxRecord.getAdvisoryMessage());
            } catch (SdwService.SdwServiceException e) {
                log.warn("Exception retrieving ITIS Codes from AdvisoryMessage for SDX record {}, treating as no ITIS Codes", sdxRecord.getRecordId(), e);
                // TODO: consider adding to an "invalid SDX records" list to include in the email? continuing?
            }
            sdxRecordMap.put(sdxRecord.getRecordId(), new CAdvisorySituationDataDeposit(sdxRecord, itisCodes));
        }

        // use the SDX TIMs to identify the active TIMs to delete from the SDX
        List<CAdvisorySituationDataDeposit> orphanedRecords = getTimsToDeleteFromSDX(activeTimMap, sdxRecordMap);
        log.info("Identified {} SDX TIMs to delete from the SDX", orphanedRecords.size());

        // use the SDX TIMs to identify the active TIMs to resend to the SDX
        GetTimsToResendResult getTimsToResendResults = getTimsToResend(activeTimMap, sdxRecordMap);
        List<CActiveTim> toResend = new ArrayList<>(getTimsToResendResults.getRecordsNotOnSdx());
        toResend.addAll(getTimsToResendResults.getOutdatedRecords());
        log.info("Identified {} TIMs to resend to the SDX ({} not on SDX, {} outdated)", toResend.size(),
                getTimsToResendResults.getRecordsNotOnSdx().size(), getTimsToResendResults.getOutdatedRecords().size());

        // delete the SDX TIMs from the SDX
        SdxDeletionResults deletionResults = new SdxDeletionResults(new HashMap<>());
        if (!orphanedRecords.isEmpty()) {
            log.info("Deleting {} records from the SDX", orphanedRecords.size());
            deletionResults = deleteTimsFromSdx(orphanedRecords);
        }

        // resend the active TIMs to the SDX
        List<ResubmitTimException> resendResults = new ArrayList<>();
        if (!toResend.isEmpty()) {
            log.info("Resending {} TIMs to the SDX", toResend.size());
            resendResults = resendTimsToSdx(toResend);
        }

        // email a summary of what was done, including invalid records
        log.info("Sending SDX validation summary email...");
        sendSummaryEmail(toResend,
                orphanedRecords,
                resendResults,
                deletionResults,
                invalidDbRecords,
                getTimsToResendResults.getOutdatedRecords().size(),
                orphanedRecords.size(),
                getTimsToResendResults.getRecordsNotOnSdx().size());
    }

    private GetTimsToResendResult getTimsToResend(HashMap<Integer, CActiveTim> activeTimMap,
                                             HashMap<Integer, CAdvisorySituationDataDeposit> sdxRecordMap) {
        List<CActiveTim> outdatedRecords = new ArrayList<>();
        List<CActiveTim> recordsNotOnSdx = new ArrayList<>();
        // to identify what needs to be resubmitted:
        // the goal is to get the condition back on the SDX
        // if an active TIM exists && it has no SDX TIM then
        //  check if active TIM endDateTime is in the future
        //  then we resubmit it to the SDX
        for (CActiveTim activeTim : activeTimMap.values()) {
            var sdxRecord = sdxRecordMap.get(activeTim.getRecordId());
            if (sdxRecord != null) {
                log.trace("Found sdxRecord with recordId {}", activeTim.getRecordId());

                // if the ITIS codes are different, we should resubmit it
                if (!activeTim.getItisCodes().equals(sdxRecord.getItisCodes())) {
                    log.trace("Active tim ITIS codes differ from SDX record, so we should resubmit it.");
                    outdatedRecords.add(activeTim);
                } else {
                    log.trace("Active tim ITIS codes match SDX record, not resubmitting.");
                }
                continue;
            }
            // at this point, we know there is no SDX record for this active tim
            recordsNotOnSdx.add(activeTim);
        }
        return new GetTimsToResendResult(recordsNotOnSdx, outdatedRecords);
    }

    private List<CAdvisorySituationDataDeposit> getTimsToDeleteFromSDX(HashMap<Integer, CActiveTim> activeTimMap,
                                                                       HashMap<Integer, CAdvisorySituationDataDeposit> sdxRecordMap) {
        List<CAdvisorySituationDataDeposit> orphanedRecords = new ArrayList<>();
        // to identify what needs to be deleted:
        //  to identify an SDX TIM exists && no active TIM exists
        //      SDX TIM recordId == Active TIM recordId
        //      compare the ITIS codes
        for (CAdvisorySituationDataDeposit sdxRecord : sdxRecordMap.values()) {
            var activeTim = activeTimMap.get(sdxRecord.getRecordId());
            if (activeTim != null) {
                log.trace("Found active_tim record with recordId {}", sdxRecord.getRecordId());
                continue;
            }
            log.trace("No activeTim found with recordId {}, so we should delete the SDX record", sdxRecord.getRecordId());
            orphanedRecords.add(sdxRecord);
        }
        return orphanedRecords;
    }

    private List<ResubmitTimException> resendTimsToSdx(List<CActiveTim> toResend) {
        var activeTimIds = toResend.stream().map(x -> x.getActiveTim().getActiveTimId()).collect(Collectors.toList());
        log.trace("Resubmitting the following ActiveTim IDs to ODE: {}", activeTimIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        return timGenerationHelper.resubmitToOde(activeTimIds); // TODO: check if we should be using a different method here (this method uses original start time)
    }

    private SdxDeletionResults deleteTimsFromSdx(List<CAdvisorySituationDataDeposit> toDelete) {
        var satRecordIds = toDelete.stream().map(x -> x.getAsdd().getRecordId()).collect(Collectors.toList());
        log.trace("Deleting the following SAT_RECORD_ID records from the SDX: {}", satRecordIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        return new SdxDeletionResults(sdwService.deleteSdxDataByRecordIdIntegers(satRecordIds));
    }

    private void sendSummaryEmail(List<CActiveTim> toResend,
                                  List<CAdvisorySituationDataDeposit> toDelete,
                                  List<ResubmitTimException> resendResults,
                                  SdxDeletionResults deletionResults,
                                  List<CActiveTim> invalidDbRecords,
                                  int numOutdatedSdxRecords,
                                  int numSdxOrphanedRecords,
                                  int numRecordsNotOnSdx) throws MessagingException {

        log.trace("Generating SDX summary email...");

        StringBuilder exceptionText = new StringBuilder();
        if (deletionResults.hasErrors()) {
            exceptionText.append("The following recordIds failed to delete from the SDX: ")
                    .append(deletionResults.getFailedRecordIds())
                    .append("<br>");
        }
        if (!resendResults.isEmpty()) {
            Gson gson = new Gson();
            exceptionText.append("The following exceptions were found while attempting to resubmit TIMs: ");
            exceptionText.append("<br/>");
            for (ResubmitTimException rte : resendResults) {
                exceptionText.append(gson.toJson(rte));
                exceptionText.append("<br/>");
            }
        }

        // clarify that SDX deletion failures may indicate errant data in the SDX, suggest next steps
        if (deletionResults.hasErrors()) {
            exceptionText.append("Note: SDX deletion failures may indicate errant data in the SDX. "
                    + "Please investigate the failed deletions and consider reaching out to the SDX support team for assistance.<br>");
        }
        // clarify that TIM resubmission failures may indicate issues with the TIM data or the ODE service, suggest next steps
        if (!resendResults.isEmpty()) {
            exceptionText.append("Note: TIM resubmission failures may indicate issues with the TIM data or the ODE service. "
                    + "Please investigate the failed resubmissions and consider reaching out to the ODE support team for assistance.<br>");
        }

        String email = emailFormatter.generateSdxSummaryEmail(numSdxOrphanedRecords, numOutdatedSdxRecords,
                numRecordsNotOnSdx, toResend, toDelete, invalidDbRecords, exceptionText.toString());

        if (!toResend.isEmpty() || !toDelete.isEmpty() || !invalidDbRecords.isEmpty()) {
            log.warn("Discrepancies found between the database and the SDX:"
                            + " {} records resent to SDX, {} records deleted from SDX, {} invalid database records",
                    toResend.size(), toDelete.size(), invalidDbRecords.size());
        } else {
            log.info("No discrepancies found between the database and the SDX.");
        }


        log.trace("Invoking email helper to send SDX summary email to specified addresses...");
        mailHelper.SendEmail(config.getAlertAddresses(), "SDX Validation Results", email);
    }

    // wrapper around HashMap<Integer, Boolean>
    private static class SdxDeletionResults {
        private final HashMap<Integer, Boolean> results;

        public SdxDeletionResults(HashMap<Integer, Boolean> results) {
            this.results = results;
        }

        public boolean hasErrors() {
            return results.entrySet().stream().anyMatch(x -> x.getValue() != null && !x.getValue());
        }

        public String getFailedRecordIds() {
            return results.entrySet().stream().filter(x -> x.getValue() == false)
                    .map(x -> x.getKey().toString()).collect(Collectors.joining(","));
        }
    }

    @Data
    @RequiredArgsConstructor
    private static class GetTimsToResendResult {
        private final List<CActiveTim> recordsNotOnSdx;
        private final List<CActiveTim> outdatedRecords;
    }

}