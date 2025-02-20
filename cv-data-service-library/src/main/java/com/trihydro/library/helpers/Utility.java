package com.trihydro.library.helpers;

import java.io.IOException;
import static java.lang.Math.toIntExact;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.trihydro.library.model.Coordinate;
import com.trihydro.library.model.Milepost;

@Component
public class Utility {
	private DateFormat utcFormatMilliSec = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private DateFormat utcFormatSec = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private DateFormat utcFormatMin = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
	public DateFormat timestampFormat = new SimpleDateFormat("dd-MMM-yy hh.mm.ss.SSS a");
	public DateFormat utcTextFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z[UTC]'");

	public Gson gson = new Gson();

	public Date convertDate(String incomingDate) {
		Date convertedDate = null;
		try {
			if (incomingDate != null) {
				if (incomingDate.contains("UTC")) {
					utcTextFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
					convertedDate = utcTextFormat.parse(incomingDate);
				}
				else if (incomingDate.contains(".")) {
					utcFormatMilliSec.setTimeZone(TimeZone.getTimeZone("UTC"));
					convertedDate = utcFormatMilliSec.parse(incomingDate);
				}
				else if (incomingDate.length() == 17) {
					utcFormatMin.setTimeZone(TimeZone.getTimeZone("UTC"));
					convertedDate = utcFormatMin.parse(incomingDate);
				}
				else {
					utcFormatSec.setTimeZone(TimeZone.getTimeZone("UTC"));
					convertedDate = utcFormatSec.parse(incomingDate);
				}
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		return convertedDate;
	}

	public <T> void logWithDate(String msg, Class<T> clazz) {
		logWithDate(clazz.getSimpleName() + ": " + msg);
	}

	public void logWithDate(String msg) {
		Date date = new Date();
		System.out.println(date + " " + msg);
	}

	public int getMinutesDurationBetweenTwoDates(String startDateTime, String endDateTime) {
		int duration = getMinutesDurationWithSimpleDateFormat(startDateTime, endDateTime);
		if (duration == -1) {
			duration = getMinutesDurationWithZonedDateTime(startDateTime, endDateTime);
		}
		if (duration == -1) {
			duration = getMinutesDurationWithYyMmDdFormat(startDateTime, endDateTime);
		}
		if (duration == -1) {
			// dates may be in different formats, attempt to identify formats & translate to ZonedDateTime, then calculate
			String startDateTimeInZonedDateTime;
			try {
				startDateTimeInZonedDateTime = translateToZonedDateTime(startDateTime);
			} catch (UnrecognizedDateFormatException e) {
				logWithDate("Failed to parse dates when getting minutes between: " + startDateTime + " and " + endDateTime + ". Unrecognized date format: " + startDateTime);
				return -1;
			}
			
			String endDateTimeInZonedDateTime;
			try {
				endDateTimeInZonedDateTime = translateToZonedDateTime(endDateTime);
			} catch (UnrecognizedDateFormatException e) {
				logWithDate("Failed to parse dates when getting minutes between: " + startDateTime + " and " + endDateTime + ". Unrecognized date format: " + startDateTime);
				return -1;
			}

			duration = getMinutesDurationWithZonedDateTime(startDateTimeInZonedDateTime, endDateTimeInZonedDateTime);
		}
		if (duration == -1) {
			logWithDate("Failed to parse dates when getting minutes between: " + startDateTime + " and " + endDateTime);
		}
		return duration;
	}

	/**
	 * Attempt to get duration in minutes between two dates parsed as ZonedDateTime.
	 * If this fails, return -1
	 * 
	 * @param startDateTime
	 * @param endDateTime
	 * @return The duration in minutes between the two given dates. If parsing
	 *         fails, returns -1
	 */
	private int getMinutesDurationWithZonedDateTime(String startDateTime, String endDateTime) {
		try {
			ZonedDateTime zdtStart = ZonedDateTime.parse(startDateTime);
			ZonedDateTime zdtEnd = ZonedDateTime.parse(endDateTime);

			java.time.Duration dateDuration = java.time.Duration.between(zdtStart, zdtEnd);
			long durationTime = Math.abs(dateDuration.toMinutes());

			return toIntExact(durationTime);
		} catch (DateTimeParseException exception) {
			return -1;
		}
	}

	/**
	 * Attempt to get duration in minutes between two dates parsed in
	 * SimpleDateFormat("dd-MMM-yy HH.mm.ss"). If parsing fails, returns -1
	 * 
	 * @param startDateTime
	 * @param endDateTime
	 * @return The duration in minutes between the two given dates. If parsing
	 *         fails, returns -1
	 */
	private int getMinutesDurationWithSimpleDateFormat(String startDateTime, String endDateTime) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yy HH.mm.ss");
			Date startDate = simpleDateFormat.parse(startDateTime);
			Date endDate = simpleDateFormat.parse(endDateTime);

			long duration = (endDate.getTime() - startDate.getTime()) / 60000; // milliseconds to minutes is 1/60000
			return toIntExact(duration);
		} catch (Exception ex) {
			return -1;
		}
	}

	/**
	 * Attempt to get duration in minutes between two dates parsed in
	 * SimpleDateFormat("yyyy-MM-dd HH:mm:ss"). If parsing fails, returns -1
	 * 
	 * @param startDateTime
	 * @param endDateTime
	 * @return The duration in minutes between the two given dates. If parsing
	 *         fails, returns -1
	 */
	private int getMinutesDurationWithYyMmDdFormat(String startDateTime, String endDateTime) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date startDate = simpleDateFormat.parse(startDateTime);
			Date endDate = simpleDateFormat.parse(endDateTime);

			long duration = (endDate.getTime() - startDate.getTime()) / 60000; // milliseconds to minutes is 1/60000
			return toIntExact(duration);
		} catch (Exception ex) {
			return -1;
		}
	}

	/**
	 * Checks the format of the date string and translates it to a ZonedDateTime if possible.
	 * Throws an UnrecognizedDateFormatException if the date format is not recognized.
	 * @param dateTimeString The date string to translate
	 * @return The date string translated to a ZonedDateTime
	 * @throws UnrecognizedDateFormatException If the date format is not recognized
	 */
	private String translateToZonedDateTime(String dateTimeString) throws UnrecognizedDateFormatException {
		// if already ZonedDateTime, return
		try {
			ZonedDateTime.parse(dateTimeString);
			return dateTimeString;
		} catch(DateTimeParseException exception) {}

		// if not ZonedDateTime, check for simple date format
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yy HH.mm.ss");
			Date startDate = simpleDateFormat.parse(dateTimeString);

			// translate to ZonedDateTime
			return ZonedDateTime.ofInstant(startDate.toInstant(), java.time.ZoneId.systemDefault()).toString();
		} catch(Exception e) {}

		// if not ZonedDateTime or SimpleDateFormat, check for YyMmDdFormat
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date startDate = simpleDateFormat.parse(dateTimeString);

			// translate to ZonedDateTime
			return ZonedDateTime.ofInstant(startDate.toInstant(), java.time.ZoneId.systemDefault()).toString();
		} catch (Exception e) {}

		throw new UnrecognizedDateFormatException("Unrecognized date format: " + dateTimeString);
	}

	/**
	 * Returns the value presented by the ResultSet at key if available. If the
	 * value presented is null, defaults to 1 (rather than short default of 0). Used
	 * for our purposes to default to 1 for various values.
	 * 
	 * @param rs  The ResultSet to pull data from
	 * @param key The key value to use to get data from the ResultSet
	 * @return Short value defaulted to 1 if not found
	 */
	public short GetShortValueFromResultSet(ResultSet rs, String key) {
		try {
			String value = rs.getString(key);
			if (value != null) {
				return Short.valueOf(value);
			}
		} catch (SQLException ex) {
			System.out.println("Error attempting to get short value '" + key + "' from ResultSet");
		}
		return (short) 1;
	}

	public int getDirection(Double bearing) {

		int direction = 0;

		if (bearing >= 0 && bearing <= 22.5)
			direction = 1;
		else if (bearing > 22.5 && bearing <= 45)
			direction = 2;
		else if (bearing > 45 && bearing <= 67.5)
			direction = 4;
		else if (bearing > 67.5 && bearing <= 90)
			direction = 8;
		else if (bearing > 90 && bearing <= 112.5)
			direction = 16;
		else if (bearing > 112.5 && bearing <= 135)
			direction = 32;
		else if (bearing > 135 && bearing <= 157.5)
			direction = 64;
		else if (bearing > 157.5 && bearing <= 180)
			direction = 128;
		else if (bearing > 180 && bearing <= 202.5)
			direction = 256;
		else if (bearing > 202.5 && bearing <= 225)
			direction = 512;
		else if (bearing > 225 && bearing <= 247.5)
			direction = 1024;
		else if (bearing > 247.5 && bearing <= 270)
			direction = 2048;
		else if (bearing > 270 && bearing <= 292.5)
			direction = 4096;
		else if (bearing > 292.5 && bearing <= 315)
			direction = 8192;
		else if (bearing > 315 && bearing <= 337.5)
			direction = 16384;
		else if (bearing > 337.5 && bearing <= 360)
			direction = 32768;

		return direction;
	}

	/**
	 * Creates a connection with authentication via an apikey and returning JSON.
	 * Used to send HTTP requests to the SDX api
	 * 
	 * @param method The HTTP method to use (GET,POST,PUT,DELETE)
	 * @param url    The URL to send the request to
	 * @param apiKey The apikey value to apply in the header
	 * @return
	 * @throws IOException
	 */
	public HttpURLConnection getSdxUrlConnection(String method, URL url, String apiKey) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("apikey", apiKey);

		return conn;
	}

    /**
	 *	Calculates an anchor point for a TIM path. The anchor point is determined by
	 *  moving 15 meters upstream from the first point of the original TIM path. The first
	 *  two points of the TIM path are used to determine a bearing direction of this initial
	 *  section of the path. The anchor point is calculated as 15 meters from the first path
	 *  point, in the opposite direction of the initial path bearing. The earth surface
	 *  distance calculations are implemented very roughly (optimized for fast calculation)
	 *  because there is no need for high accuracy for the anchor point position.
	 *  Two shortcuts were applied for the earth surface distance calculations:
	 *	    1) a spherical earth is assumed with a fixed 111195 meters per surface degree
	 *         distance to calculate the delta degrees difference between the first two path points
	 *		2) a flat plane assumption was used to calculate the bearing line distance from
     *         the previously calculated latitude and longitude delta distance (d0Latitude, d0Longitude),
	 *         and these delta distances provide the flat plane bearing direction to move upstream to
	 *         reach the anchor point
	 * 
     * @param firstPoint The first milepost.
     * @param secondPoint The second milepost.
     * @return The anchor coordinate.
     */
    public Coordinate calculateAnchorCoordinate(Milepost firstPoint, Milepost secondPoint) {
		int precision = 9;
		int metersPerSurfaceDegree = 111195;

        BigDecimal firstPointLatitude = firstPoint.getLatitude().round(new java.math.MathContext(precision));
        BigDecimal firstPointLongitude = firstPoint.getLongitude().round(new java.math.MathContext(precision));
        BigDecimal secondPointLatitude = secondPoint.getLatitude().round(new java.math.MathContext(precision));
        BigDecimal secondPointLongitude = secondPoint.getLongitude().round(new java.math.MathContext(precision));

		// 1) Get the difference in latitude between the first and second points.
        BigDecimal differenceInLatitude = firstPointLatitude.subtract(secondPointLatitude);

        // 2) Get the difference in longitude between the first and second points.
		BigDecimal differenceInLongitude = firstPointLongitude.subtract(secondPointLongitude);

		// 3) Multiply the difference in latitude by the meters per surface degree to get d0Latitude.
        BigDecimal d0Latitude = new BigDecimal(metersPerSurfaceDegree).multiply(differenceInLatitude);
        // 4) Multiply the difference in longitude by the cosine of the first point's latitude multiplied by the difference in longitude to get d0Longitude.
		BigDecimal firstPointLatitudeInRadians = firstPointLatitude.multiply(new BigDecimal(Math.PI)).divide(new BigDecimal(180), new java.math.MathContext(precision));
		BigDecimal d0Longitude = new BigDecimal(111195).multiply(new BigDecimal(Math.cos(firstPointLatitudeInRadians.doubleValue()))).multiply(differenceInLongitude);

		// 5) Take the square root of d0Latitude squared plus d0Longitude squared to get d0.
        BigDecimal d0 = d0Latitude.pow(2).add(d0Longitude.pow(2)).sqrt(new java.math.MathContext(6));

		// 6) Divide 15 by d0 to get mD.
        BigDecimal mD = new BigDecimal(15).divide(d0, new java.math.MathContext(6));

		// 7) Multiply mD by the difference in latitude and add the first point's latitude to get the anchor's latitude.
        BigDecimal anchorLatitude = firstPointLatitude.add(mD.multiply(differenceInLatitude)).round(new java.math.MathContext(precision));
        // 8) Multiply mD by the difference in longitude and add the first point's longitude to get the anchor's longitude.
		BigDecimal anchorLongitude = firstPointLongitude.add(mD.multiply(differenceInLongitude)).round(new java.math.MathContext(precision));

		// 9) The anchor coordinate is (anchor latitude, anchor longitude).
        return new Coordinate(anchorLatitude, anchorLongitude);
    }

	private class UnrecognizedDateFormatException extends Exception {
		public UnrecognizedDateFormatException(String message) {
			super(message);
		}
	}
}