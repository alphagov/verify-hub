# 1. Record architecture decisions

Date: 15/05/2017

## Status

Accepted

## Context

At present approximately 20% of users take >1 hour to verify at an IDP. In order to improve completion rate in the hub we are proposing to increase this time to 1.5 hours.
This will mean that almost all users should verify before the session times out in Policy.

## Decision

The policy session timeout value is set via application config (authn_session_validity_period) managed by ida-webops.

## Consequences

There should be no negative impact on the hub journey. However if the RP's timeout is <1.5h the user will see a failure when they reach the RP
There is also a the  at 2 hours (infinispan_expiration). This value (also managed by ida-webops)will also be increased to 2.5 hours.
