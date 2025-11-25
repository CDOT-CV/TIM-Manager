package com.trihydro.library.helpers;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DateTimeHelperImplTest {
    private final DateTimeHelperImpl dateTimeHelper = new DateTimeHelperImpl();

    @Test
    public void convertDate_min_SUCCESS() {
        // Arrange
        var date = "2020-10-28T14:53Z";

        // Act
        var convertedDate = new DateTimeHelperImpl().convertDate(date);

        // Assert
        Assertions.assertNotNull(convertedDate);
        Assertions.assertEquals(1603896780000L, convertedDate.getTime());
    }

    @Test
    public void convertDate_sec_SUCCESS() {
        // Arrange
        // SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        var date = "2020-10-28T14:53:00Z";

        // Act
        var convertedDate = dateTimeHelper.convertDate(date);

        // Assert
        Assertions.assertNotNull(convertedDate);
        Assertions.assertEquals(1603896780000L, convertedDate.getTime());
    }

    @Test
    public void convertDate_milli_SUCCESS() {
        // Arrange
        // SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        var date = "2020-10-28T14:53:00.123Z";

        // Act
        var convertedDate = dateTimeHelper.convertDate(date);

        // Assert
        Assertions.assertNotNull(convertedDate);
        Assertions.assertEquals(1603896780123L, convertedDate.getTime());
    }

    @Test
    public void convertDate_utcText_SUCCESS() {
        // Arrange
        // SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        var date = "2020-02-10T17:00:00.000Z[UTC]";

        // Act
        var convertedDate = dateTimeHelper.convertDate(date);

        // Assert
        Assertions.assertNotNull(convertedDate);
        Assertions.assertEquals(1581354000000L, convertedDate.getTime());
    }

    @Test
    public void testConvertDateStringFromISO8601FormatIntoTimestampObject_ISO8601Format_ShouldReturnTimestamp() throws
        DateStringNotInISO8601FormatException {
        // Arrange
        String dateString = "2025-04-30T06:00:00.000Z";
        String expectedTimestampString = "2025-04-30 06:00:00.0";

        // Act
        Timestamp timestamp = dateTimeHelper.convertDateStringFromISO8601FormatIntoTimestampObject(dateString);

        // Assert
        Assertions.assertNotNull(timestamp);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Assertions.assertEquals(expectedTimestampString, timestamp.toString());
    }

    @Test
    public void testConvertDateStringFromISO8601FormatIntoTimestampObject_NotISO8601Format_ShouldThrowException() {
        // Arrange
        String dateString = "2025-04-30 06:00:00";

        // Act & Assert
        Assertions.assertThrows(DateStringNotInISO8601FormatException.class, () -> {
            dateTimeHelper.convertDateStringFromISO8601FormatIntoTimestampObject(dateString);
        });
    }

    @Test
    public void testIsInTableFormat_TableFormat_ShouldReturnTrue() {
        // Arrange
        String dateString = "2025-04-30 06:00:00";

        // Act
        boolean result = dateTimeHelper.isInTableFormat(dateString);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testIsInTableFormat_NotTableFormat_ShouldReturnFalse() {
        // Arrange
        String dateString = "2025-04-30T06:00:00.000Z";

        // Act
        boolean result = dateTimeHelper.isInTableFormat(dateString);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testIsInTableFormat_Null_ShouldReturnFalse() {
        // Arrange
        String dateString = null;

        // Act
        boolean result = dateTimeHelper.isInTableFormat(dateString);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testIsInISO8601Format_ISO8601Format_ShouldReturnTrue() {
        // Arrange
        String dateString = "2025-04-30T06:00:00.000Z";

        // Act
        boolean result = dateTimeHelper.isInISO8601Format(dateString);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testIsInISO8601Format_NotISO8601Format_ShouldReturnFalse() {
        // Arrange
        String dateString = "2025-04-30 06:00:00";

        // Act
        boolean result = dateTimeHelper.isInISO8601Format(dateString);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testIsInISO8601Format_Null_ShouldReturnFalse() {
        // Arrange
        String dateString = null;

        // Act
        boolean result = dateTimeHelper.isInISO8601Format(dateString);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testConvertZonedDateTimeToISO8601Format_ValidZonedDateTime_ShouldReturnISO8601Format() {
        // Arrange
        ZonedDateTime zdt = ZonedDateTime.parse("2025-04-30T06:00:00Z");

        // Act
        String result = dateTimeHelper.convertZonedDateTimeToISO8601Format(zdt);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("2025-04-30T06:00:00.000Z", result);
    }

    @Test
    public void testConvertZonedDateTimeToISO8601Format_NullInput_ShouldThrowException() {
        // Arrange
        ZonedDateTime zdt = null;

        // Act & Assert
        Assertions.assertThrows(NullPointerException.class, () -> {
            dateTimeHelper.convertZonedDateTimeToISO8601Format(zdt);
        });
    }
}