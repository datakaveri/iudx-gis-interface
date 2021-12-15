package iudx.gis.server.apiserver;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import iudx.gis.server.apiserver.handlers.AuthHandler;
import iudx.gis.server.apiserver.handlers.ValidationHandler;
import iudx.gis.server.apiserver.response.ResponseType;
import iudx.gis.server.apiserver.response.RestResponse;
import iudx.gis.server.apiserver.service.CatalogueService;
import iudx.gis.server.apiserver.util.RequestType;
import iudx.gis.server.apiserver.validation.ValidationFailureHandler;
import iudx.gis.server.authenticate.AuthenticatorService;
import iudx.gis.server.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static iudx.gis.server.apiserver.util.Constants.*;

public class ApiServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  /** Service addresses */
  private static final String DATABASE_SERVICE_ADDRESS = "iudx.gis.database.service";
  private static final String AUTH_SERVICE_ADDRESS = "iudx.gis.authentication.service";

  private HttpServer server;
  private Router router;
  private int port = 11111;
  private boolean isSSL, isProduction;
  private String keystore;
  private String keystorePassword;
  private CatalogueService catalogueService;

  private DatabaseService database;
  private AuthenticatorService authenticator;
  // private ParamsValidator validator;
  private static Set<String> validParams = new HashSet<String>();
  private static Set<String> validHeaders = new HashSet<String>();
  static {
    validParams.add(NGSILDQUERY_ID);
    validParams.add(NGSILDQUERY_IDPATTERN);
    validHeaders.add(HEADER_TOKEN);
    validHeaders.add("User-Agent");
    validHeaders.add("Content-Type");
  }

  @Override
  public void start() throws Exception {
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add(HEADER_ACCEPT);
    allowedHeaders.add(HEADER_TOKEN);
    allowedHeaders.add(HEADER_CONTENT_LENGTH);
    allowedHeaders.add(HEADER_CONTENT_TYPE);
    allowedHeaders.add(HEADER_HOST);
    allowedHeaders.add(HEADER_ORIGIN);
    allowedHeaders.add(HEADER_REFERER);
    allowedHeaders.add(HEADER_ALLOW_ORIGIN);

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    router = Router.router(vertx);

    router = Router.router(vertx);
    router.route().handler(
        CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    router.route().handler(requestHandler -> {
      requestHandler.response()
          .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
          .putHeader("Pragma", "no-cache").putHeader("Expires", "0")
          .putHeader("X-Content-Type-Options", "nosniff");
      requestHandler.next();
    });

    router.route().handler(BodyHandler.create());

    /* Read ssl configuration. */
    isSSL = config().getBoolean("ssl");

    /* Read server deployment configuration. */
    isProduction = config().getBoolean("production");

    HttpServerOptions serverOptions = new HttpServerOptions();

    if (isSSL) {
      LOGGER.debug("Info: Starting HTTPs server");

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions.setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));

    } else {
      LOGGER.debug("Info: Starting HTTP server");

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
      if (isProduction) {
        port = 80;
      } else {
        port = 8080;
      }
    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    /* Get a handler for the Service Discovery interface. */

    database = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    authenticator = AuthenticatorService.createProxy(vertx, AUTH_SERVICE_ADDRESS);

    ValidationHandler entityQueryValidationHandler =
        new ValidationHandler(vertx, RequestType.ENTITY_QUERY);
    ValidationFailureHandler validationsFailureHandler = new ValidationFailureHandler();

    router.get(NGSILD_ENTITIES_URL).handler(entityQueryValidationHandler)
        .handler(this::handleEntitiesQuery).failureHandler(validationsFailureHandler);

    ValidationHandler entityPathValidationHandler =
        new ValidationHandler(vertx, RequestType.ENTITY_PATH);
    router
        .get(NGSILD_ENTITIES_URL + "/:domain/:userSha/:resourceServer/:resourceGroup/:resourceName")
        .handler(entityPathValidationHandler).handler(this::handleEntitiesPath)
        .failureHandler(validationsFailureHandler);

    ValidationHandler adminCrudPathValidationHandler =
        new ValidationHandler(vertx, RequestType.ADMIN_CRUD_PATH);

    router
        .post(ADMIN_BASE_PATH)
        .handler(adminCrudPathValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handlePostAdminPath)
        .failureHandler(validationsFailureHandler);

    router
        .put(ADMIN_BASE_PATH)
        .handler(adminCrudPathValidationHandler)
        .handler(this::handlePutAdminPath)
        .failureHandler(validationsFailureHandler);

    router
        .delete(ADMIN_BASE_PATH)
        .handler(this::handleDeleteAdminPath)
        .failureHandler(validationsFailureHandler);

    catalogueService = new CatalogueService(vertx, config());
  }

  private void handleDeleteAdminPath(RoutingContext routingContext) {
    LOGGER.debug("Info:handleDeleteAdminPath method started.;");
    HttpServerResponse response = routingContext.response();
    String resourceId = routingContext.queryParams().get("id");

    database.deleteAdminDetails(resourceId, ar -> {
      if (ar.succeeded()) {
        LOGGER.debug("Success: Delete operation successful");
        handleSuccessResponse(response, ResponseType.Ok.getCode(), ar.result().toString());
      } else {
        LOGGER.error("Fail: Delete operation Failed");
        processBackendResponse(response, ar.cause().getMessage());
      }
    });
  }

  private void handlePutAdminPath(RoutingContext routingContext) {
    LOGGER.debug("Info:handlePutAdminPath method started.;");
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = routingContext.getBodyAsJson();

    database.updateAdminDetails(requestBody, ar -> {
      if (ar.succeeded()) {
        LOGGER.debug("Success: Update operation successful");
        handleSuccessResponse(response, ResponseType.Ok.getCode(), ar.result().toString());
      } else {
        LOGGER.error("Fail: Update operation Failed");
        processBackendResponse(response, ar.cause().getMessage());
      }
    });
  }

  private void handlePostAdminPath(RoutingContext routingContext) {
    LOGGER.debug("Info:handlePostAdminPath method started.;");
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = routingContext.getBodyAsJson();

    database.insertAdminDetails(requestBody, ar -> {
      if (ar.succeeded()) {
        LOGGER.debug("Success: Insert operation successful");
        handleSuccessResponse(response, ResponseType.Ok.getCode(), ar.result().toString());
      } else {
        LOGGER.error("Fail: Insert operation Failed");
        processBackendResponse(response, ar.cause().getMessage());
      }
    });
  }

  private void handleEntitiesPath(RoutingContext routingContext) {
    LOGGER.debug("Info:handleLatestEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    LOGGER.debug("authInfo : " + authInfo);
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    String domain = request.getParam(JSON_DOMAIN);
    String userSha = request.getParam(JSON_USERSHA);
    String resourceServer = request.getParam(JSON_RESOURCE_SERVER);
    String resourceGroup = request.getParam(JSON_RESOURCE_GROUP);
    String resourceName = request.getParam(JSON_RESOURCE_NAME);
    String id =
        domain + "/" + userSha + "/" + resourceServer + "/" + resourceGroup + "/" + resourceName;
    JsonObject json = new JsonObject();
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);
    json.put(JSON_INSTANCEID, instanceID);
    json.put(JSON_ID, id);
    LOGGER.debug("Info: IUDX query json;" + json);
    // check Catalogue Cache before search
    Future<Boolean> isIdPresent = catalogueService.isItemExist(id);
    isIdPresent.onSuccess(handler -> executeSearchQuery(json, response)).onFailure(handler -> handleResponse(response, ResponseType.NotFound));
  }

  private void handleEntitiesQuery(RoutingContext routingContext) {
    LOGGER.debug("Info:handleEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    LOGGER.debug("authInfo : " + authInfo);
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    MultiMap headerParams = request.headers();
    // validate request parameters
    if (validateParams(params)) {
      JsonObject json = new JsonObject();
      json.put(ID, params.entries().get(0).toString());
      // check Catalogue Cache before calling search
      Future<Boolean> isIdPresent =
          catalogueService.isIdPresent(params.entries().get(0).toString());
      isIdPresent.onComplete(handler -> executeSearchQuery(json, response));
    } else {
      LOGGER.error("Fail: Validation failed");
      handleResponse(response, ResponseType.BadRequestData, MSG_BAD_QUERY);
    }
  }

  /**
   * Execute a search query in DB
   *
   * @param json valid json query
   * @param response
   */
  private void executeSearchQuery(JsonObject json, HttpServerResponse response) {
    database.searchQuery(json, handler -> {
      if (handler.succeeded()) {
        LOGGER.info("Success: Search Success");
        handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
      } else if (handler.failed()) {
        LOGGER.error("Fail: Search Fail");
        processBackendResponse(response, handler.cause().getMessage());
      }
    });
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.debug("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      ResponseType responseType = ResponseType.fromCode(type);
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(type)
          .end(generateResponse(responseType).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json received else from backend service");
      handleResponse(response, ResponseType.BadRequestData);
    }

  }

  private void handleResponse(HttpServerResponse response, ResponseType responseType) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(responseType.getCode())
        .end(generateResponse(responseType).toString());
  }

  private void handleResponse(HttpServerResponse response, ResponseType responseType,
      String message) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(responseType.getCode())
        .end(generateResponse(responseType, message).toString());
  }

  private JsonObject generateResponse(ResponseType responseType) {
    int type = responseType.getCode();
    return new RestResponse.Builder().withType(type)
        .withTitle(ResponseType.fromCode(type).getMessage())
        .withMessage(ResponseType.fromCode(type).getMessage()).build().toJson();
  }

  private JsonObject generateResponse(ResponseType responseType, String message) {
    int type = responseType.getCode();
    return new RestResponse.Builder().withType(type)
        .withTitle(ResponseType.fromCode(type).getMessage()).withMessage(message).build().toJson();

  }

  private Optional<MultiMap> getQueryParams(RoutingContext routingContext,
      HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      String uri = routingContext.request().uri();
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(uri, HttpConstants.DEFAULT_CHARSET, true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        LOGGER.debug("Info: param :" + entry.getKey() + " value : " + entry.getValue());
        queryParams.add(entry.getKey(), entry.getValue());
      }
    } catch (IllegalArgumentException ex) {
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(ResponseType.BadRequestData.getCode())
          .end(generateResponse(ResponseType.BadRequestData, MSG_BAD_QUERY).toString());


    }
    return Optional.of(queryParams);
  }

  private boolean validateParams(MultiMap parameterMap) {
    final List<Map.Entry<String, String>> entries = parameterMap.entries();
    for (final Map.Entry<String, String> entry : entries) {
      // System.out.println(entry.getKey());
      if (!validParams.contains(entry.getKey())) {
        return false;
      }
    }
    return true;
  }
}
