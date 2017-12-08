# policy

The Policy service is the heart of the Verify hub. It contains the control logic for processing messages processed by saml-engine and Frontend. The state of a given user’s session is stored in an in-memory Infinispan shared between all Policy instances. Each appropriate action that the user can take will progress the state and update the state object in place. An orthogonal responsibility of Policy is to ensure that Audit receives a message for each business event that occurs.

## Microservices that policy uses

**saml-engine**: Policy uses saml-engine to translate incoming SAML messages into domain objects, and to generate outgoing SAML messages

**event-sink**: events are sent to event-sink for auditing and reporting purposes

**config**: Config service stores all of the details on where to send messages and how to construct them

## Microservices that use policy

**frontend-api**: to get/set information about a user session

**saml-proxy**: validates incoming/outgoing SAML messages to/from policy

## Resources

* `/policy/session`: resources to deal with a user session
* `/policy/received-authn-request`: resources to deal with a user session
* `/infinispan/details`: get information about the current infinispan cluster

### Standard paths for all our apps
* `/internal/version-info`: json formatted version info about the current build of the service
* `/service-name`: name of this service
* `/service-status`: used to determine health of the app by haproxy when services are load balanced.  Also used to take the app out of service in haproxy to enable zero downtime releases

## History

Responsible for storing, validating and managing the processing of authentication requests from a transaction.

1 Validation within our system comes in two flavours: Business Validation (ie: is a response from the correct idp) and SAML validation (ie: does a request have a valid NotOnOrAfter). Business validation happens in policy and SAML validation happens in Saml Engine.

2 Session in the context of the Hub refers to a single attempt by a user to authenticate with a Transaction. The session currently has a maximum lifetime of 1 hour. After the session has expired, the user has another hour before the session is removed from policy. During this time, the user can request to be sent back to the original Transaction in order to try again.

4. Admin Service
The admin service is responsible for the storage and management of the information required to integrate with identity providers and relying parties. The admin service provides a user interface to view and modify both relying parties and identity providers. This will also include storage of the Hub details that will be published as part of the Hub metadata. The majority of the details of both identity providers and relying parties will be delegated to the metadata service, however the registration and access will be in Admin.

5. Audit Service
The audit service is an asynchronous service that provides the functionality around the storage business events and errors. These will be stripped of all personally identifying information. The intent of this service will be for billing, security auditing and other currently unknown requirements. It uses Akka, an actor library to handle the asynchronous nature of the service.