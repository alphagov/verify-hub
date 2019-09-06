package uk.gov.ida.integrationtest.hub.policy.builders;

import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

import java.util.Optional;

public class InboundResponseFromIdpDtoBuilder {
    public static InboundResponseFromIdpDto successResponse(String idpEntityId, LevelOfAssurance levelOfAssurance) {
        return buildDTO(IdpIdaStatus.Status.Success,
                idpEntityId,
                Optional.of(levelOfAssurance),
                Optional.empty(),
                Optional.empty());
    }

    public static InboundResponseFromIdpDto successResponse(String idpEntityId, LevelOfAssurance levelOfAssurance, String notOnOrAfter) {
        return buildDTO(IdpIdaStatus.Status.Success,
                idpEntityId,
                Optional.of(levelOfAssurance),
                Optional.empty(),
                Optional.of(notOnOrAfter));
    }

    public static InboundResponseFromIdpDto errorResponse(String idpEntityId, IdpIdaStatus.Status status) {
        return buildDTO(status, idpEntityId,
                Optional.of(LevelOfAssurance.LEVEL_2),
                Optional.empty(),
                Optional.empty());
    }

    public static InboundResponseFromIdpDto fraudResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.RequesterError,
                idpEntityId,
                Optional.of(LevelOfAssurance.LEVEL_X),
                Optional.of("fraudIndicator"),
                Optional.empty());
    }

    public static InboundResponseFromIdpDto failedResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.AuthenticationFailed,
                idpEntityId, Optional.of(LevelOfAssurance.LEVEL_2),
                Optional.empty(),
                Optional.empty());
    }

    public static InboundResponseFromIdpDto noAuthnContextResponse(String idpEntityId) {
        return buildDTO(IdpIdaStatus.Status.NoAuthenticationContext,
                idpEntityId,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static InboundResponseFromIdpDto buildDTO(IdpIdaStatus.Status status, String idpEntityId,
                                                      Optional<LevelOfAssurance> levelOfAssurance,
                                                      Optional<String> fraudText,
                                                      Optional<String> notOnOrAfter) {
        return new InboundResponseFromIdpDto(
                status,
                Optional.ofNullable("message"),
                idpEntityId,
                Optional.ofNullable("authnStatement"),
                Optional.of("encrypted-mds-assertion"),
                Optional.ofNullable("principalipseenbyidp"),
                Optional.ofNullable("pid"),
                levelOfAssurance,
                fraudText,
                fraudText,
                notOnOrAfter);
    }
}
