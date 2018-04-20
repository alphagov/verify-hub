package uk.gov.ida.saml.core;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

public abstract class DateTimeFreezer {

    public static void freezeTime (DateTime dateTime) {
        DateTimeUtils.setCurrentMillisFixed(dateTime.getMillis());
    }

    public static void freezeTime() {
        unfreezeTime();
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().getMillis());
    }

    public static void unfreezeTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }
}
