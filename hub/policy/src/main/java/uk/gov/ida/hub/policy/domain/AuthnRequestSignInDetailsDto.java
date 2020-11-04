package uk.gov.ida.hub.policy.domain;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AuthnRequestSignInDetailsDto {

    private String requestIssuerId;
    private boolean transactionSupportsEidas;
    private boolean beforeEUExit;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestSignInDetailsDto() { }

    public AuthnRequestSignInDetailsDto(
            String requestIssuerId,
            boolean transactionSupportsEidas) {

        this.requestIssuerId = requestIssuerId;
        this.transactionSupportsEidas = transactionSupportsEidas;
        this.beforeEUExit = evaluateBeforeEUExit();
    }

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }

    public boolean getBeforeEUExit() {
        return beforeEUExit;
    }

    public boolean evaluateBeforeEUExit() {
        LocalDateTime euExit = LocalDateTime.of(2020, Month.OCTOBER, 28, 23, 59, 59);
        ZonedDateTime exitDateTime = ZonedDateTime.of(euExit, ZoneId.systemDefault());
        return ZonedDateTime.now(ZoneId.systemDefault()).isBefore(exitDateTime);
    }
}
