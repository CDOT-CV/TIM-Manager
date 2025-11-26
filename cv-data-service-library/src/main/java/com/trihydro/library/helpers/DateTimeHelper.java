package com.trihydro.library.helpers;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Date;

public interface DateTimeHelper {
    Date convertDate(String incomingDate);

    String getStartTime();

    String getIsoDateTimeString(Date date);

    String getIsoDateTimeString(ZonedDateTime date);

    /**
     * Checks if a given date string is in the table format defined by the class.
     * The expected format is checked against a predefined format in TABLE_FORMAT.
     *
     * @param dateString The date string to be checked for table format compliance.
     * @return true if the date string matches the table format, false otherwise.
     */
    boolean isInTableFormat(String dateString);

    /**
     * Checks whether the provided date string is in the ISO8601 format.
     *
     * @param dateString The date string to be validated against the ISO8601 format.
     * @return true if the date string complies with the ISO8601 format, false otherwise.
     */
    boolean isInISO8601Format(String dateString);

    String convertDateStringFromTableFormatIntoISO8601Format(String endDateTimeStr) throws ParseException;

    String convertZonedDateTimeToISO8601Format(ZonedDateTime zdt);

    /**
     * Converts a date string from the ISO8601 format into a {@link Timestamp} object.
     * <p>
     * The method validates if the provided date string is in the ISO8601 format. If the validation fails,
     * a {@link DateStringNotInISO8601FormatException} is thrown. Upon successful validation, the method
     * converts the date string into a {@link Date} object using the {@code convertDate} method,
     * and then converts it to a {@link Timestamp} object.
     *
     * @param dateString The date string in the ISO8601 format to be converted into a {@link Timestamp} object.
     * @return A {@link Timestamp} object representing the input date string.
     * @throws DateStringNotInISO8601FormatException If the provided date string is not in the ISO8601 format.
     */
    Timestamp convertDateStringFromISO8601FormatIntoTimestampObject(String dateString) throws
        DateStringNotInISO8601FormatException;
}
