# saml-engine

This is the beating heart of our SAML processing system. saml-engine is responsible for the creation/encryption, consumption/decryption and validation1 of all SAML messages that pass through the hub. The general pattern for incoming messages is that saml-engine will be given a SAML Message, validate it, transform it into a JSON object then pass the message on to policy. For outgoing messages, saml-engine will be asked for a given message, then it will ask Policy for a JSON representation of the given message, translate that to SAML and pass it on to the requesting service.

saml-engine uses Redis to protect against replay attacks on both assertions and authn requests. We don’t accept duplicate assertions or authn requests for a two hour period.

saml-engine is the only place that holds the Verify Hub’s private signing & encryption keys: it is responsible for signing and decrypting all SAML messages

saml-engine relies on Policy sending protective monitoring and error events to event-sink on its behalf, such that they are captured in our audit system.

## Microservices that saml-engine uses

**config**: Fetches certificates for signature validation and encryption.

**federation metadata**: 

## Microservices that use saml-engine

**saml-soap-proxy**: 

**Policy**: 

## Resources:

* `/saml-engine/translate-*`: translate from an incoming encrypted SAML message to a deserialised domain object
* `/saml-engine/generate-*`: generate a (signed) and encrypted outgoing SAML message

On the app's admin port metadata can be force refreshed with an empty POST to `/tasks/metadata-refresh`

### Standard paths for all our apps
* `/internal/version-info`: json formatted version info about the current build of the service
* `/service-name`: name of this service
* `/service-status`: used to determine health of the app by haproxy when services are load balanced.  Also used to take the app out of service in haproxy to enable zero downtime releases

## History

Messages used to be passed directly from saml-proxy to saml-engine, before being passed to policy.  The interaction between microservices was unwound somewhat, so the message flow became saml-proxy to policy to saml-engine.  saml-engine thus basically became a REST interface to our SAML libraries.