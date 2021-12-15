package iudx.gis.server.apiserver.response;

import java.util.stream.Stream;

public enum ResponseUrn {
  SUCCESS("urn:dx:gis:success", "Successful operation"),
  INVALID_ATTR_PARAM("urn:dx:gis:invalidAttributeParam", "Invalid attribute param"),
  INVALID_ATTR_VALUE("urn:dx:gis:invalidAttributeValue", "Invalid attribute value"),
  INVALID_OPERATION("urn:dx:gis:invalidOperation", "Invalid operation"),
  UNAUTHORIZED_ENDPOINT("urn:dx:gis:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE("urn:dx:gis:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN("urn:dx:gis:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN("urn:dx:gis:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN("urn:dx:gis:invalidAuthorizationToken", "Token is invalid"),

  RESOURCE_ALREADY_EXISTS("urn:dx:gis:resourceAlreadyExists", "Document of given ID already exists"),

  INVALID_PAYLOAD_FORMAT("urn:dx:gis:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
  RESOURCE_NOT_FOUND("urn:dx:gis:resourceNotFound", "Document of given id does not exist"),
  METHOD_NOT_FOUND("urn:dx:gis:MethodNotAllowed", "Method not allowed for given endpoint"),
  UNSUPPORTED_MEDIA_TYPE("urn:dx:gis:UnsupportedMediaType", "Requested/Presented media type not supported"),

  RESPONSE_PAYLOAD_EXCEED("urn:dx:gis:responsePayloadLimitExceeded", "Search operations exceeds the default response payload limit"),
  REQUEST_PAYLOAD_EXCEED("urn:dx:gis:requestPayloadLimitExceeded", "Operation exceeds the default request payload limit"),
  REQUEST_OFFSET_EXCEED("urn:dx:gis:requestOffsetLimitExceeded", "Operation exceeds the default value of offset"),
  REQUEST_LIMIT_EXCEED("urn:dx:gis:requestLimitExceeded", "Operation exceeds the default value of limit"),

  BACKING_SERVICE_FORMAT("urn:dx:gis:backend", "format error from backing service [cat,auth etc.]"),

  YET_NOT_IMPLEMENTED("urn:dx:gis:general", "urn yet not implemented in backend verticle.");

  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public static ResponseUrn fromCode(final String urn) {
    return Stream.of(values())
        .filter(v -> v.urn.equalsIgnoreCase(urn))
        .findAny()
        .orElse(YET_NOT_IMPLEMENTED); /* If backend services don't respond with URN */
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }
}
