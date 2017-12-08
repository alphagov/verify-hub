# saml-soap-proxy:

* saml-proxy and saml-soap-proxy make up the ‘Proxy Services’ that are intended to provide the hub system with a front line of defence against malicious attack. These services provide basic XML and signature validation on all incoming and outgoing messages.

* saml-soap-proxy is a _possibly doomed service_ that’s main responsibility is to manage the asynchronous nature of requests to the matching service as well as signature and basic SAML validation for attribute query requests and responses to matching-service-adapters (MSAs).

## Microservices that saml-soap-proxy uses

**config**: fetches the signing certificates for the matching services in order to validate message signatures, and also get lists of matching service adapters so it can run healthchecks against them

**saml-engine**: used to create SAML messages for MSA healthchecks, and translate the healthcheck responses 

## Microservices that use saml-soap-proxy

**policy**: Informs policy when the response has been returned. The initial request will include a timeout time, after which policy will no longer accept responses. In this case Saml SOAP proxy will simply LOG the failure to Kibana and be done.

**sensu**: sensu uses saml-soap-proxy to initiate healthchecks to MSAs

## Resources

* `/matching-service-request-sender`: used to send matching requests and user account creation requests to MSAs
* `/saml-soap-proxy/matching-service-health-check`: used by sensu to initiate a healthcheck on all MSAs with healthchecking enabled
* `/saml-soap-proxy/matching-service-version-check`: forces a healthcheck on all configured MSAs, regardless of whether healthchecks are enabled or not.  Allows Yaks to look at the health of all MSAs.

On the app's admin port metadata can be force refreshed with an empty POST to `/tasks/metadata-refresh`

### Standard paths for all our apps
* `/internal/version-info`: json formatted version info about the current build of the service
* `/service-name`: name of this service
* `/service-status`: used to determine health of the app by haproxy when services are load balanced.  Also used to take the app out of service in haproxy to enable zero downtime releases

## History

