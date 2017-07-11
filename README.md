Hub SAML
========

Responsible for SAML behaviours which are specific to the domain of the hub. At a high level:

* Handling requests from RPs
* Generating requests for IDPs
* Handling responses from IDPs
* Generating requests for MSAs
* Handling responses from MSAs
* Generating responses for RPs

At a lower level this includes:

* Converting OpenSAML objects to Hub domain objects
* Converting Hub domain objects to OpenSAML objects
* Validating assertions from IDP responses
* Validating assertions from MSA responses
* Generating Attribute Query Requests
* Generating Cycle 3 Dataset Assertions
* Hashing PIDs

Common tasks (e.g. validating signatures) are handled by dependencies such as saml-security and saml-serialisers.

Hub SAML Test Utils
-------------------

`hub-saml-test-utils` is provided for services which require hub-like behaviour to set up state for their tests.
For example: in order to test stub-idp we need to generate an example AuthnRequest. `hub-saml-test-utils` provides
helpful builders for situations like this.

