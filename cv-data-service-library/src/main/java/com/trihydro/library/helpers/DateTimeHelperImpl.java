package com.trihydro.library.helpers;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DateTimeHelperImpl implements DateTimeHelper {
    /**
     * A constant {@link SimpleDateFormat} using the pattern "yyyy-MM-dd HH:mm:ss".
     * This format matches the default output of Timestamp.toString(), which is commonly
     * how timestamps appear in PostgreSQL tables. It represents a timestamp with year,
     * month, day, hour, minute, and second.
     *
     * Format pattern: "yyyy-MM-dd HH:mm:ss"
     * Example value: "2023-10-15 14:30:45"
     */
    private static final SimpleDateFormat TABLE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * A constant {@link SimpleDateFormat} used for formatting and parsing ISO 8601 date-time strings
     * with millisecond precision and a trailing 'Z' to denote UTC time.
     *
     * Example value:  "2023-10-15T12:34:56.789Z"
     *
     * This seems to be the most stable format for our application.
     */
    private static final SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    // other date formats
    // TODO: identify if formats can be removed in favor of the newer TABLE_FORMAT and ISO8601_FORMAT formats
    private final DateFormat utcFormatMilliSec = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final DateFormat utcFormatSec = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final DateFormat utcFormatMin = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    private final DateFormat utcTextFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z[UTC]'");

    public DateTimeHelperImpl() {
    }

    @Override
    public Date convertDate(String incomingDate) {
        Date convertedDate = null;
        try {
            if (incomingDate != null) {
                if (incomingDate.contains("UTC")) {
                    utcTextFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    convertedDate = utcTextFormat.parse(incomingDate);
                } else if (incomingDate.contains(".")) {
                    utcFormatMilliSec.setTimeZone(TimeZone.getTimeZone("UTC"));
                    convertedDate = utcFormatMilliSec.parse(incomingDate);
                } else if (incomingDate.length() == 17) {
                    utcFormatMin.setTimeZone(TimeZone.getTimeZone("UTC"));
                    convertedDate = utcFormatMin.parse(incomingDate);
                } else {
                    utcFormatSec.setTimeZone(TimeZone.getTimeZone("UTC"));
                    convertedDate = utcFormatSec.parse(incomingDate);
                }
            }
        } catch (ParseException e1) {
            log.error("Exception", e1);
        }
        return convertedDate;
    }

    @Override
    public String getStartTime() {
        Date date = new Date();
        return getIsoDateTimeString(date);
    }

    @Override
    public String getIsoDateTimeString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    @Override
    public String getIsoDateTimeString(ZonedDateTime date) {
        if (date == null) {
            return null;
        }

        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        var utcDate = date.withZoneSameInstant(ZoneOffset.UTC);
        return utcDate.format(formatter);
    }

    /**
     * Checks if a given date string is in the table format defined by the class.
     * The expected format is checked against a predefined format in TABLE_FORMAT.
     *
     * @param dateString The date string to be checked for table format compliance.
     * @return true if the date string matches the table format, false otherwise.
     */
    @Override
    public boolean isInTableFormat(String dateString) {
        try {
            TABLE_FORMAT.parse(dateString); // check if date is in table format ("2025-04-30 06:00:00")
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Checks whether the provided date string is in the ISO8601 format.
     *
     * @param dateString The date string to be validated against the ISO8601 format.
     * @return true if the date string complies with the ISO8601 format, false otherwise.
     */
    @Override
    public boolean isInISO8601Format(String dateString) {
        try {
            ISO8601_FORMAT.parse(dateString); // check if date is in ISO8601 format ("2025-04-30T06:00:00.000Z")
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public String convertDateStringFromTableFormatIntoISO8601Format(String endDateTimeStr) throws ParseException {
        return ISO8601_FORMAT.format(TABLE_FORMAT.parse(endDateTimeStr));
    }

    @Override
    public String convertZonedDateTimeToISO8601Format(ZonedDateTime zdt) {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return ISO8601_FORMAT.format(convertDate(zdt.toString()));
    }

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
    @Override
    public Timestamp convertDateStringFromISO8601FormatIntoTimestampObject(String dateString) throws
        DateStringNotInISO8601FormatException {
        if (!isInISO8601Format(dateString)) {
            throw new DateStringNotInISO8601FormatException(
                "Date string expected to be in ISO8601 format but was not: " + dateString);
        }
        Date desiredEndDateTime = convertDate(dateString);
        return new Timestamp(desiredEndDateTime.getTime());
    }

}