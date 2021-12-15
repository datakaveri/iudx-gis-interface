package iudx.gis.server.apiserver.util;

import java.util.List;

public class Constants {
  // date-time format
  public static final String APP_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]'Z'";
  public static final String APP_NAME_REGEX = "[a-zA-Z0-9._\\-]*$";

  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String API_METHOD = "method";
  public static final String ID = "id";
  public static final String IDS = "ids";

  // config
  public static final String CONFIG_FILE = "config.properties";

  // NGSI-LD endpoints
  public static final String NGSILD_BASE_PATH = "/ngsi-ld/v1";
  public static final String NGSILD_ENTITIES_URL = NGSILD_BASE_PATH + "/entities";

  // admin API endpoints
  public static final String ADMIN_BASE_PATH = "/admin/gis/serverInfo";

  /** API Documentation endpoint */
  public static final String ROUTE_STATIC_SPEC = "/apis/spec";
  public static final String ROUTE_DOC = "/apis";

  public static final List<String> bypassEndpoint = List.of(ROUTE_STATIC_SPEC, ROUTE_DOC);
  public static final List<String> openEndPoints = List.of("/ngsi-ld/v1/temporal/entities",
      "/ngsi-ld/v1/entities", "/ngsi-ld/v1/entityOperations/query");

  // path regex
  public static final String ENTITITES_URL_REGEX = NGSILD_ENTITIES_URL + "(.*)";

  /** Accept Headers and CORS */
  public static final String MIME_APPLICATION_JSON = "application/json";
  public static final String MIME_TEXT_HTML = "text/html";

  // ngsi-ld/IUDX query paramaters
  public static final String NGSILDQUERY_ID = "id";
  public static final String NGSILDQUERY_IDPATTERN = "idpattern";
  public static final String NGSILDQUERY_TYPE = "type";
  public static final String IUDXQUERY_OPTIONS = "options";
  public static final String NGSILDQUERY_ENTITIES = "entities";

  // Header params
  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final String HEADER_OPTIONS = "options";

  // request/response params
  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";

  // json fields
  public static final String ERROR_MESSAGE = "errorMessage";
  public static final String JSON_INSTANCEID = "instanceID";
  public static final String JSON_TYPE = "type";
  public static final String JSON_DOMAIN = "domain";
  public static final String JSON_USERSHA = "userSHA";
  public static final String JSON_NAME = "name";
  public static final String JSON_ENTITIES = "entities";
  public static final String JSON_ID = "id";
  public static final String JSON_VALUE = "value";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";
  public static final String JSON_RESOURCE_SERVER = "resourceServer";
  public static final String JSON_RESOURCE_GROUP = "resourceGroup";
  public static final String JSON_RESOURCE_NAME = "resourceName";


  // messages (Error, Exception, messages..)
  public static final String MSG_INVALID_PARAM = "Invalid parameter in request.";
  public static final String MSG_PARAM_DECODE_ERROR = "Error while decoding query params.";
  public static final String MSG_INVALID_EXCHANGE_NAME = "Invalid exchange name";
  public static final String MSG_INVALID_QUEUE_NAME = "Invalid queue name";
  public static final String MSG_INVALID_VHOST_NAME = "Invalid vhost name";
  public static final String MSG_INVALID_NAME = "Invalid name.";
  public static final String MSG_FAILURE = "failure";
  public static final String MSG_FAILURE_NO_VHOST = "No vhosts found";
  public static final String MSG_FAILURE_VHOST_EXIST = "vHost already exists";
  public static final String MSG_FAILURE_EXCHANGE_NOT_FOUND = "Exchange not found";
  public static final String MSG_FAILURE_QUEUE_NOT_EXIST = "Queue does not exist";
  public static final String MSG_FAILURE_QUEUE_EXIST = "Queue already exists";
  public static final String MSG_EXCHANGE_EXIST = "Exchange already exists";
  public static final String MSG_SUB_TYPE_NOT_FOUND = "Subscription type not present in body";
  public static final String MSG_SUB_INVALID_TOKEN = "Invalid/no token found in header";
  public static final String MSG_BAD_QUERY = "Bad query";

  // results
  public static final String SUCCCESS = "success";

  // Validations
  public static final int VALIDATION_ID_MIN_LEN = 0;
  public static final int VALIDATION_ID_MAX_LEN = 512;
  public static final String VALIDATION_ID_PATTERN = ".*";// TODO : create a regex for IUDX ID
                                                          // pattern
  public static final int VALIDATION_MAX_ATTRS = 5;
  public static final int VALIDATION_MAX_DAYS_INTERVAL_ALLOWED = 10;
  public static final int VALIDATION_COORDINATE_PRECISION_ALLOWED = 6;
  public static final int VALIDATIONS_MAX_ATTR_LENGTH = 100;
  public static final int VALIDATION_ALLOWED_COORDINATES = 10;
  public static final List<String> VALIDATION_ALLOWED_HEADERS = List.of("token", "options");


}
