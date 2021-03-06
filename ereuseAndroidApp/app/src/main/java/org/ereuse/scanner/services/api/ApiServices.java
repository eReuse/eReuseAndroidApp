package org.ereuse.scanner.services.api;

public interface ApiServices {

    String METHOD_LOGIN = "login";
    String METHOD_DEVICE = "device";
    String METHOD_LOCATE = "locate";
    String METHOD_RECEIVE = "receive";
    String METHOD_RECYCLE = "recycle";
    String METHOD_PLACE = "place";
    String METHOD_EVENTS = "events";
    String METHOD_SNAPSHOT = "snapshot";
    String METHOD_DEVICE_COMPONENT_REMOVE = "remove";
    String METHOD_EVENT_UNDO = "undo";
    String METHOD_GENERIC_EVENT = "generic";
    String METHOD_MANUFACTURERS = "manufacturers";
    // TODO Add other API methods

    ApiResponse execute(ApiRequest request, String... method) throws ApiException;

}
