# config

config hosts the technical configuration of the Verify federation, used as a source of truth by all of our microservices.  It contains no real logic.

## Microservices that config uses

_none_

## Microservices that use config

**frontend-api**: 

**saml-proxy**: 

**saml-soap-proxy**: 

**policy**: 

**saml-engine**: 

**sensu**: initiates OCSP checks for certificates served by config

## Resource paths

* `/config/certificates`: various resources to get and check certs
* `/config/idps`: various resources to get information about configured IDPs
* `/config/matching-services`: various resources to get information about configured matching services
* `/config/transactions`: various resources to get information about configured transactions

### Standard paths for all our apps
* `/internal/version-info`: json formatted version info about the current build of the service
* `/service-name`: name of this service
* `/service-status`: used to determine health of the app by haproxy when services are load balanced.  Also used to take the app out of service in haproxy to enable zero downtime releases

## History

The config service used to contain all display strings for the now old frontend (frontend-api).  Changes to this text was painful as it required a deply from a protected laptop via a usb stick.  All that text was first moved into the old frontend, and included by default in the new verify-frontend.

The config service also used to determine which IDPs are appropriate for users to be shown.  Identity evidence selections by the user were sent to the config service and a list of suitable IDPs was returned.  That has since been removed and similar but more complex functionality has been implemented in the verify-frontend.
