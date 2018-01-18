# 3. Simple SAML Profile

Date: 2017-10-03 (but covering previous work)

## Status

Accepted

## Context

This is being written some time after the the Simple SAML Profile was created and implemented.

The SAML profile in use by the Verify Hub did not work with commercial, off-the-shelf software (COTS) such at Shibboleth, and work was played to ensure comatibility with COTS software.

The two main changes to the SAML profile, thus also the incompatibilities removed, were:

1. Hub does not sign the outer messages sent back to relying parties.  The main profile sees assertions created and signed by the MSA, with the outer message signed by Hub.  That outer signature is no longer applied in the Simple SAML Profile.
2. The no-match response in the standard SAML profile returns Success:no-match to the relying party service in the no-match case, but this is technically not valid as _Success_ responses require one or more assertions to be present in the message.  That is not the case for a _no-match_ response.  Therefore the change in the Simple SAML Profile was to return Responder:no-match.

## Decision

Implement

## Consequences

Non-success responses in the Simple SAML Profile are not signed at all (either by the MSA or Hub).

The standard SAML profile can be used by other COTS implementations such as SimpleSAMLphp, however, that experiences issues with the _no-match_ response being Success:no-match instead of Responder:no-match.  Further work will be required.

