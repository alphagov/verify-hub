# saml-proxy

* saml-proxy and saml-soap-proxy make up the ‘Proxy Services’ that are intended to provide the hub system with a front line of defence against malicious attack. These services provide basic XML and signature validation on all incoming and outgoing messages.

* saml-proxy is responsible for handling the basic base64, SAML and signature validation on all incoming Requests From a Transaction and Responses from an IDP. For responses from the IDP the relay state is validated against the secure cookie created during the initial request.

* saml-proxy also does signature validation on outgoing messages from the hub. In order to do this, saml-proxy uses the federation metadata

* saml-proxy hosts the Hub’s now deprecated but still in use _sp_ and _idp_ SAML Metadata

## Microservices that saml-proxy uses

**policy**: saml-proxy will pass validated SAML messages to policy and ask policy to fetch outgoing SAML messages

**config**: to retrieve certificates for RPs and MSAs for signature validation

**federation metadata** to retrieve certificates for the Verify Hub and IDPs for signature validation

## Microservices that use saml-proxy

**frontend-api**: passes saml messages from/to the frontend via frontend-api.

## Resources

* `/API/metadata/*`: see below
* `/SAML2/SSO/API/RECEIVER/*`: these resources receive and passes on SAML messages
* `/SAML2/SSO/API/SENDER/*`: these resources are used to send SAML messages. The user agent will only be able to send messages from the Hub if they have a valid session, and the session is in the correct state.

On the app's admin port metadata can be force refreshed with an empty POST to `/tasks/metadata-refresh`

### Standard paths for all our apps
* `/internal/version-info`: json formatted version info about the current build of the service
* `/service-name`: name of this service
* `/service-status`: used to determine health of the app by haproxy when services are load balanced.  Also used to take the app out of service in haproxy to enable zero downtime releases

### Hub Metadata

This resource hosts the hub’s now deprecated metadata, which should be served externally with the content type `application/samlmetadata+xml`.
 
 There are two types of SAML Metadata hosted within this resource:

1. Metadata for RPs lives at `/SAML/metadata/sp` on the frontend and provides metadata for both the matching service and the digital transactions.
2. Metadata for IDPs lives at `/SAML/metadata/idp` on the frontend and provides metadata for the IDPs to connect with us.

## History

saml-proxy used to be directly user-facing, but is now fully hidden behind other microservices.  The API it exposes shows how it was derived, with the paths similar to those it used to consume directly from users.