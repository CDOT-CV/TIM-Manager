package com.trihydro.library.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

@Data
@Slf4j
public class ActiveTim {

    private Long activeTimId;
    private Long timId;
    private String timType;
    private Long timTypeId;
    private String direction;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private Timestamp startTimestamp;
    private String startDateTime;
    private String endDateTime;
    // the expirationDateTime is used by the tim refresh module to resign a TIM and resubmit
    private String expirationDateTime;
    private String route;
    private String clientId;
    private String satRecordId;
    private Integer pk;
    private String rsuTarget;
    private Integer rsuIndex;
    private List<Integer> itisCodes;
    private Coordinate startPoint;
    private Coordinate endPoint;
    private Integer projectKey;

    public boolean isIdenticalConditions(List<Integer> itisCodesToCompare, String endDateTimeToCompare, int minutesUntilEndDateTimeToCompare) {
        // check if existing condition is identical to requested condition
        boolean identicalITISCodes = false;
        boolean identicalEndDate = false;
        List<Integer> existingITISCodes = getItisCodes();
        if (existingITISCodes != null) {
            if (existingITISCodes.equals(itisCodesToCompare)) {
                identicalITISCodes = true;
            }
        }

        // format (turn 2025-04-16T06:00:00.000Z into 2025-04-16 06:00:00)
        SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            // Check if endDateTimeToCompare matches source format
            if (endDateTimeToCompare != null) {
                sourceFormat.parse(endDateTimeToCompare); // Validate format
                endDateTimeToCompare = targetFormat.format(sourceFormat.parse(endDateTimeToCompare));
            }
        } catch (Exception e) {
            log.error("Unable to parse or format endDateTimeToCompare: {}", endDateTimeToCompare, e);
            return false;
        }

        // check if end_date is identical
        String existingEndDateTime = getEndDateTime();
        if (existingEndDateTime != null) {
            log.trace("End date of existing condition: {}", existingEndDateTime);
            log.trace("End date of requested condition: {}", endDateTimeToCompare);
            // existing condition has an end date, check if it is identical
            if (existingEndDateTime.equals(endDateTimeToCompare)) {
                identicalEndDate = true;
            }
        } else {
            // existing condition has no end date, check if requested condition has no end
            // date or if the end date is more than 32000 minutes in the future (if end date is
            // more than 32000 minutes in the future, it is considered identical to no end date)
            if (endDateTimeToCompare == null || minutesUntilEndDateTimeToCompare >= 32000) {
                identicalEndDate = true;
            }
        }
        log.trace("identicalITISCodes: {}", identicalITISCodes);
        log.trace("identicalEndDate: {}", identicalEndDate);
        return identicalITISCodes && identicalEndDate;
    }
}