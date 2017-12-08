# stub-event-sink

A basic stub service used when running the Verify Hub locally

## Microservices that stub-event-sink uses

_none_

## Microservices that use stub-event-sink

most of them

## Resources

* `/event-sink/hub-support-hub-events`: the same interface used by _event-sink_ to consume messages from hub
* `/test/events`: read/delete events recorded by stub-event-sink

### Standard paths for all our apps
* `/internal/version-info`: json formatted version info about the current build of the service
* `/service-name`: name of this service
* `/service-status`: used to determine health of the app by haproxy when services are load balanced.  Also used to take the app out of service in haproxy to enable zero downtime releases

## History

_none_
