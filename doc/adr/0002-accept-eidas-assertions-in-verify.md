# 2. Accept eIDAS Assertions In Verify

Date: 2017-10-03

## Status

Accepted

# Context

For the alpha, we elected to build a proof concept eIDAS implementation for receiving identities from other countries that would require minimal changes to the Verify Hub. We opted to create a service called the [Verify-eIDAS-Bridge](https://github.com/alphagov/verify-eidas-bridge) that acted as a translation layer for converting between Verify and eIDAS' SAML specifications.

![Alpha diagram](https://github.gds/christopherholmes/eidas-beta-plan/raw//214a7fb2fbcd451aae1c6758b292fc07bb6b29bf/alpha_arch_two.png)

When receiving AuthnRequests from the Hub, it validated them and, after selecting a country, it generated a new AuthnRequest compatible with eIDAS for that country.

When receiving Responses from eIDAS Proxy Nodes, it validated and decrypted them then generated a new Response compatible with Verify. Importantly, this required the Bridge to become the producer of signatures and Assertions that the Hub and Relying Party (RP) had to trust.

Within Verify, we have a goal of maintaining end-to-end trust between RP and the Identity Provider (IDP) where Identity Assertions consumed by the RP's Matching Service Adapter (MSA) are signed by the IDP and not the Verify Hub, or any other Verify-managed service. This is important because it mitigates potential attack vectors as an attacker who compromises the Hub cannot inject false identities into the ecosystem.

# Decision
By adopting a new technical design for beta, we believe that end-to-end trust will be possible when connecting to another country's Proxy Nodes to act as IDPs. The simplified process flow in the new design will be:

1. Verify receives an AuthnRequest from a RP that supports eIDAS authentication
1. User chooses to use a country, Country A, as an IDP
1. Verify validates and reads Country A's metadata to find the location of its Proxy Node
1. Verify sends an eIDAS-style AuthnRequest to Country A via the User's browser
1. User logs into an IDP at Country A
1. Verify receives and validates a Response from Country A
1. Verify decrypts the Assertions, validates its signature and encrypts it for the RP's MSA
1. Verify starts the Matching Process by sending an AttributeQueryRequest containing the eIDAS assertion to the MSA
1. On receiving the AQR, the MSA will decrypt the eIDAS Assertion and validate its signature by reading and validating Country A's metadata then converts it to a format compatible with the RP's Local Matching Service before attempting to Match
1. After the Matching Process is complete the User will either be redirected to the service or be shown an appropriate failure message

This new design requires making changes to the existing Verify Hub architecture, but we could provide a second independent eIDAS Hub that provides the required behaviour. However, we believe that this would make moving between choosing a certified company and a country far more complicated.

In order to achieve this we will need to make the following changes:
* The Hub and MSA will need to be able to consume eIDAS metadata from different countries
* The Hub will need to be able to send eIDAS-style AuthnRequests
* The Hub will need to be able to understand, trust and verify, eIDAS Identity Assertions and validate them.
* The Hub will need to be able pass signed eIDAS Identity Assertions to Matching Services during Matching Requests similarly to what it currently does with the Matching Dataset Assertion.
* The MSA will need to be able to receive eIDAS Identity Assertions, trust and verify them, and convert them into the data format required for matching.
* We will need to provide a journey on the Hub that is appropriate for a user that wishes to use an eID.

In addition to changes to the Hub and MSA there are some new components/ that will be needed:
* A Metadata Aggregator that aggregates all EU IDP metadata into a single location/service that can be consumed by the Hub and all MSAs
* A signed document containing trust anchors that will be consumed by the Hub and all MSAs
* A metadata document describing the eIDAS Hub as a Service Provider/Connector Node that EU IDPs will need to consume.

## Design Diagram
![Beta diagram](https://github.gds/christopherholmes/eidas-beta-plan/raw/be4fb122a367302a5aa3d0bfcadff83c0d9f935e/forward%20journey.png)

## Summary of Advantages

* Consistent with existing Verify operational model
* Doesn't require an independent eIDAS enabled Hub
* Orchestration of the matching process requires only minimal changes
* Compromising Hub signing keys shouldn't enable attackers to inject false identities into the ecosystem
* Doesn't require running any service in a stricter or more secure environment compared to the existing Hub

## Risks to Design
At the time of writing no service design has been produced for a eIDAS IDP journey and we do not know how coupled we want the typical Verify journey with it. Importantly, we don't know if we want the ability to switch between choosing Verify IDPs or eIDAS IDPs during a user's journey. These decisions will impact the changes required to FE and Policy and our ability to introduce similar, but independent services.

If Assertions received from a country have short lifetimes there could be negative impacts on matching. Currently, the Hub has a 20 minute matching window to facilitate Cycle 3, but if the Assertion expires then matching will be cut short. If necessary, a lifetime workaround could be implemented in the MSA to support the full matching window.

#Trusting and Verifying EU countries

In order to verify the signatures of Responses and Assertion from an EU country the Hub and MSAs need to be able to consume signed Metadata hosted by the respective country. Each country will pre-share a trust anchor with the UK that can be used to verify the Metadata. In order to reduce the number of metadata locations that the Hub and MSA will need to connect to (via proxy configuration) we will instead provide a single location containing aggregated metadata for all notified MSswhere signatures are maintained. This aggregated metadata will be produced by a service, Metadata Aggregator, deployed outside the Hub, that is capable of consuming metadata from each notified country's metadata location. All Country metadata will be aggregated into a single document or a single country's metadata will be available on a path containing the url encoded form of the country's Entity ID.

## Trust Anchor Metadata
After downloading the appropriate metadata document for a country the Hub and MSA will still need to verify its signature using the country's trust anchor. Verify currently provides its metadata trust anchor alongside releases of the MSA. We cannot follow this pattern with other country's trust anchor as it will not scale when there are up to 27 different trust anchors in play. A change to one trust anchor would require rolling out a new version of the MSA to every Relying Party.

We can provide trust anchors to the Verify federation using a resource that hosts a document containing a map of MS entity id as keys and a list of their trust anchors as their values. This document will be cryptographically signed in a process similar to the Verify Federation Metadata signing where signing happens as an offline process. An appropriately managed offline signing process should prevent a rogue operator or a compromise of the online service from introducing fake trust anchors. The document will preferably follow a JSON Web Signature format and not follow the SAML Metadata format.

## Consequences

_*Implementation in Progress*_
