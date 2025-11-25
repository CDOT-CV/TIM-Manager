package com.trihydro.library.models;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.trihydro.library.model.ActiveTim;

public class ActiveTimTest {
    
    final String EXISTING_END_DATE_TIME = "2023-04-16 06:00:00";
    final String END_DATE_TIME_TO_COMPARE = "2023-04-16T06:00:00.000Z";
    
    /**
     * This test verifies that the isIdenticalConditions method returns true
     * when the conditions and end date time are identical.
     */
    @Test
    public void testIdenticalConditionsWithIdenticalITISCodesAndEndDate() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);
        String existingEndDateTime = EXISTING_END_DATE_TIME;
        activeTim.setEndDateTime(existingEndDateTime);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 3);
        String endDateTimeToCompare = END_DATE_TIME_TO_COMPARE;
        int minutesUntilEndDateTimeToCompare = 1000;

        boolean expectedResult = true;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns true
     * when the existing end date is null and the end date to compare is null.
     * 
     * Note: When the end date time to compare is null, the minutes until end date
     * time to compare is not used and should be set to -1.
     */
    @Test
    public void testIdenticalConditionsWithNoEndDate() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 3);
        String endDateTimeToCompare = null;
        int minutesUntilEndDateTimeToCompare = -1;

        boolean expectedResult = true;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns true
     * when the existing end date is null and the end date to compare is not null
     * (and the minutes until end date time to compare is more than 32000).
     * 
     * This will be the scenario when a planned county road condition with a total
     * duration of more than 32000 minutes is being compared to an existing active
     * county road condition with no end date. Since the planned condition will end
     * more than 32000 minutes in the future, it is considered identical to the
     * existing active condition with no end date.
     */
    @Test
    public void testIdenticalConditionsWithNoEndDateAndEndDateToCompare() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 3);
        String endDateTimeToCompare = END_DATE_TIME_TO_COMPARE;
        int minutesUntilEndDateTimeToCompare = 32001;

        boolean expectedResult = true;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns true
     * when the existing end date is null and the end date to compare is not null
     * (and the minutes until end date time to compare is equal to 32000).
     * 
     * This will be the scenario when a planned county road condition with a total
     * duration of exactly 32000 minutes is being compared to an existing active
     * county road condition with no end date. Since the planned condition will end
     * exactly 32000 minutes in the future, it is considered identical to the
     * existing active condition with no end date.
     */
    @Test
    public void testIdenticalConditionsWithNoEndDateAndEndDateToCompare32000() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 3);
        String endDateTimeToCompare = END_DATE_TIME_TO_COMPARE;
        int minutesUntilEndDateTimeToCompare = 32000;

        boolean expectedResult = true;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns false
     * when the conditions are identical but the end date time is different.
     */
    @Test
    public void testIdenticalConditionsWithIdenticalITISCodesAndDifferentEndDate() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);
        String existingEndDateTime = EXISTING_END_DATE_TIME;
        activeTim.setEndDateTime(existingEndDateTime);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 3);
        String endDateTimeToCompare = END_DATE_TIME_TO_COMPARE.replace("23", "24");
        int minutesUntilEndDateTimeToCompare = 1000;

        boolean expectedResult = false;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns false
     * when the conditions are different but the end date time is identical.
     */
    @Test
    public void testIdenticalConditionsWithDifferentITISCodesAndIdenticalEndDate() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);
        String existingEndDateTime = EXISTING_END_DATE_TIME;
        activeTim.setEndDateTime(existingEndDateTime);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 4);
        String endDateTimeToCompare = END_DATE_TIME_TO_COMPARE;
        int minutesUntilEndDateTimeToCompare = 1000;

        boolean expectedResult = false;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns false
     * when the existing end date is not null and the end date to compare is null.
     * 
     * Note: When the end date time to compare is null, the minutes until end date
     * time to compare is not used and should be set to -1.
     */
    @Test
    public void testIdenticalConditionsWithExistingEndDateAndNoEndDateToCompare() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);
        String existingEndDateTime = EXISTING_END_DATE_TIME;
        activeTim.setEndDateTime(existingEndDateTime);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 3);
        String endDateTimeToCompare = null;
        int minutesUntilEndDateTimeToCompare = -1;

        boolean expectedResult = false;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns false
     * when the conditions and end date time are different.
     */
    @Test
    public void testIdenticalConditionsWithDifferentITISCodesAndEndDate() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);
        String existingEndDateTime = EXISTING_END_DATE_TIME;
        activeTim.setEndDateTime(existingEndDateTime);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 4);
        String endDateTimeToCompare = END_DATE_TIME_TO_COMPARE.replace("23", "24");
        int minutesUntilEndDateTimeToCompare = 1000;

        boolean expectedResult = false;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

    /**
     * This test verifies that the isIdenticalConditions method returns false
     * when the existing end date is null and the end date to compare is not null
     * (and the minutes until end date time to compare is less than 32000).
     * 
     * This will be the scenario when a planned county road condition with a total
     * duration of more than 32000 minutes is being compared to an existing active
     * county road condition with no end date. Since the planned condition will end
     * less than 32000 minutes in the future, it is not considered identical to the
     * existing active condition with no end date. The planned condition will be
     * expired and re-submitted as a new active condition with an end date.
     */
    @Test
    public void testIdenticalConditionsWithNoEndDateAndEndDateToCompareLessThan32000() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        List<Integer> existingItisCodes = Arrays.asList(1, 2, 3);
        activeTim.setItisCodes(existingItisCodes);

        List<Integer> itisCodesToCompare = Arrays.asList(1, 2, 3);
        String endDateTimeToCompare = END_DATE_TIME_TO_COMPARE;
        int minutesUntilEndDateTimeToCompare = 31999;

        boolean expectedResult = false;

        // execute
        boolean actualResult = activeTim.isIdenticalConditions(itisCodesToCompare, endDateTimeToCompare, minutesUntilEndDateTimeToCompare);

        // verify
        assertEquals(expectedResult, actualResult);
    }

}
