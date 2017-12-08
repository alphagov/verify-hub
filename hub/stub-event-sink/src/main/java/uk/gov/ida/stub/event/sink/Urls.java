package uk.gov.ida.stub.event.sink;

public interface Urls {

    /** NOTE: the general form for this class should be
    * *_ROOT - used to annotate the Resource Class (root path for the resource)
    * *_PATH - used to annotate the Methods within the Resource Class
    * *_PARAM - used to annotate the parameters for the methods
    * *_RESOURCE - used by the Proxy classes in order to reference the resource. Internal to the hub (may be external to the micro service)
    * *_ENDPOINT - used for external (to the hub) endpoints
    *
    * If the parameter you are referencing/adding doesn't fit this style, perhaps it needs a different place to live.
    */
    interface HubSupportUrls {
        String EVENT_SINK_ROOT = "/event-sink";
        String HUB_SUPPORT_EVENT_SINK_RESOURCE = EVENT_SINK_ROOT + "/hub-support-hub-events";
    }

}
