package uk.gov.ida.hub.policy.domain;

public class Cycle3UserInput {
    private String cycle3Input;
    private String principalIpAddress;

    @SuppressWarnings("unused")//Needed by JAXB
    private Cycle3UserInput() {
    }

    public Cycle3UserInput(String cycle3Input, String principalIpAddress) {
        this.cycle3Input = cycle3Input;
        this.principalIpAddress = principalIpAddress;
    }

    public String getCycle3Input() {
        return cycle3Input;
    }

    public String getPrincipalIpAddress() {
        return principalIpAddress;
    }
}
