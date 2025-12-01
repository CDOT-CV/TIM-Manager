package com.trihydro.library.helpers;

public class DateStringNotInISO8601FormatException extends Exception {
    public DateStringNotInISO8601FormatException(String message) {
        super(message);
    }
}
