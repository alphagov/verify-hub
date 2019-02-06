package uk.gov.ida.integrationtest.hub.config.apprule.support;

public class Message {
    private final String message;
    private final boolean present;

    private Message(final String message, final boolean present) {
        this.message = message;
        this.present = present;
    }

    public static Message messageShouldBePresent(final String message) {
        return new Message(message, true);
    }

    public static Message messageShouldNotBePresent(final String message) {
        return new Message(message, false);
    }

    public String getMessage() {
        return message;
    }

    public boolean isPresent() {
        return present;
    }
}
