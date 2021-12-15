package iudx.gis.server.apiserver.util;

public enum HttpStatusCode {

  // 1xx: Informational
  CONTINUE(100, "Continue", "urn:dx:gis:continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols", "urn:dx:gis:switchingProtocols"),
  PROCESSING(102, "Processing", "urn:dx:gis:processing"),
  EARLY_HINTS(103, "Early Hints", "urn:dx:gis:earlyHints"),

  // 2XX: codes
  SUCCESS(200, "Successful Operation", "urn:dx:gis:success"),
  NO_CONTENT(204, "No Content", "urn:dx:gis:noContent"),

  // 4xx: Client Error
  BAD_REQUEST(400, "Bad Request", "urn:dx:gis:badRequest"),
  UNAUTHORIZED(401, "Not Authorized", "urn:dx:gis:notAuthorized"),
  PAYMENT_REQUIRED(402, "Payment Required", "urn:dx:gis:paymentRequired"),
  FORBIDDEN(403, "Forbidden", "urn:dx:gis:forbidden"),
  NOT_FOUND(404, "Not Found", "urn:dx:gis:notFound"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed", "urn:dx:gis:methodNotAllowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable", "urn:dx:gis:notAcceptable"),
  PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required", "urn:dx:gis:proxyAuthenticationRequired"),
  REQUEST_TIMEOUT(408, "Request Timeout", "urn:dx:gis:requestTimeout"),
  CONFLICT(409, "Conflict", "urn:dx:gis:conflict"),
  GONE(410, "Gone", "urn:dx:gis:gone"),
  LENGTH_REQUIRED(411, "Length Required", "urn:dx:gis:lengthRequired"),
  PRECONDITION_FAILED(412, "Precondition Failed", "urn:dx:gis:preconditionFailed"),
  REQUEST_TOO_LONG(413, "Payload Too Large", "urn:dx:gis:payloadTooLarge"),
  REQUEST_URI_TOO_LONG(414, "URI Too Long", "urn:dx:gis:uriTooLong"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "urn:dx:gis:unsupportedMediaType"),
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", "urn:dx:gis:rangeNotSatisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed", "urn:dx:gis:expectation Failed"),
  MISDIRECTED_REQUEST(421, "Misdirected Request", "urn:dx:gis:misdirected Request"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "urn:dx:gis:unprocessableEntity"),
  LOCKED(423, "Locked", "urn:dx:gis:locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency", "urn:dx:gis:failedDependency"),
  TOO_EARLY(425, "Too Early", "urn:dx:gis:tooEarly"),
  UPGRADE_REQUIRED(426, "Upgrade Required", "urn:dx:gis:upgradeRequired"),
  PRECONDITION_REQUIRED(428, "Precondition Required", "urn:dx:gis:preconditionRequired"),
  TOO_MANY_REQUESTS(429, "Too Many Requests", "urn:dx:gis:tooManyRequests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large", "urn:dx:gis:requestHeaderFieldsTooLarge"),
  UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons", "urn:dx:gis:unavailableForLegalReasons"),

  // 5xx: Server Error
  INTERNAL_SERVER_ERROR(500, "Internal Server Error", "urn:dx:gis:internalServerError"),
  NOT_IMPLEMENTED(501, "Not Implemented", "urn:dx:gis:notImplemented"),
  BAD_GATEWAY(502, "Bad Gateway", "urn:dx:gis:badGateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable", "urn:dx:gis:serviceUnavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout", "urn:dx:gis:gatewayTimeout"),
  HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported", "urn:dx:gis:httpVersionNotSupported"),
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "urn:dx:gis:variantAlsoNegotiates"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage", "urn:dx:gis:insufficientStorage"),
  LOOP_DETECTED(508, "Loop Detected", "urn:dx:gis:loopDetected"),
  NOT_EXTENDED(510, "Not Extended", "urn:dx:gis:notExtended"),
  NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required", "urn:dx:gis:networkAuthenticationRequired");

  private final int value;
  private final String description;
  private final String urn;

  HttpStatusCode(int value, String description, String urn) {
    this.value = value;
    this.description = description;
    this.urn = urn;
  }

  public int getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public String getUrn() {
    return urn;
  }

  @Override
  public String toString() {
    return value + " " + description;
  }

  public static HttpStatusCode getByValue(int value) {
    for (HttpStatusCode status : values()) {
      if (status.value == value)
        return status;
    }
    throw new IllegalArgumentException("Invalid status code: " + value);
  }
}
