package iudx.gis.server.apiserver;

import static iudx.gis.server.apiserver.response.ResponseUrn.BACKING_SERVICE_FORMAT;
import static iudx.gis.server.apiserver.response.ResponseUrn.YET_NOT_IMPLEMENTED;
import static iudx.gis.server.apiserver.util.Constants.*;
import static iudx.gis.server.common.Constants.AUTHENTICATION_SERVICE_ADDRESS;
import static iudx.gis.server.common.Constants.METERING_SERVICE_ADDRESS;
import static iudx.gis.server.common.Constants.PG_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import iudx.gis.server.apiserver.handlers.AuthHandler;
import iudx.gis.server.apiserver.handlers.ValidationFailureHandler;
import iudx.gis.server.apiserver.handlers.ValidationHandler;
import iudx.gis.server.apiserver.query.PgsqlQueryBuilder;
import iudx.gis.server.apiserver.response.ResponseType;
import iudx.gis.server.apiserver.response.ResponseUrn;
import iudx.gis.server.apiserver.service.CatalogueService;
import iudx.gis.server.apiserver.util.HttpStatusCode;
import iudx.gis.server.apiserver.util.RequestType;
import iudx.gis.server.authenticator.AuthenticationService;
import iudx.gis.server.common.Api;
import iudx.gis.server.database.postgres.PostgresService;
import iudx.gis.server.metering.MeteringService;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApiServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  // private ParamsValidator validator;
  private static final Set<String> validParams = new HashSet<String>();
  private static final Set<String> validHeaders = new HashSet<String>();

  static {
    validParams.add(NGSILDQUERY_ID);
    validParams.add(NGSILDQUERY_IDPATTERN);
    validHeaders.add(HEADER_TOKEN);
    validHeaders.add("User-Agent");
    validHeaders.add("Content-Type");
  }

  public String dxApiBasePath;
  public String adminBasePath;
  private HttpServer server;
  private Router router;
  private int port;
  private boolean isssl;
  private String keystore;
  private String keystorePassword;
  // private DatabaseService database;
  private PostgresService postgresService;

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

    dxApiBasePath = config().getString("dxApiBasePath");
    adminBasePath = config().getString("adminBasePath");
    config().getString("dxCatalogueBasePath");
    config().getString("dxAuthBasePath");

    router = Router.router(vertx);
    router
        .route()
        .handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    router
        .route()
        .handler(
            requestHandler -> {
              requestHandler
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              requestHandler.next();
            });

    router.route().handler(BodyHandler.create());
    /* Read ssl configuration. */
    isssl = config().getBoolean("ssl");

    HttpServerOptions serverOptions = new HttpServerOptions();
    if (isssl) {

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /*
       * Default port when ssl is enabled is 8443. If set through config, then that value is taken
       */
      port = config().getInteger("httpPort") == null ? 8443 : config().getInteger("httpPort");

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
      LOGGER.info("Info: Starting HTTPs server at port" + port);

    } else {

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
      /*
       * Default port when ssl is disabled is 8080. If set through config, then that value is taken
       */
      port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
      LOGGER.info("Info: Starting HTTP server at port" + port);
    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    /* Get a handler for the Service Discovery interface. */
    AuthenticationService.createProxy(vertx, AUTHENTICATION_SERVICE_ADDRESS);
    MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    Api api = Api.getInstance(dxApiBasePath, adminBasePath);

    ValidationHandler entityQueryValidationHandler =
        new ValidationHandler(vertx, RequestType.ENTITY_QUERY);
    ValidationFailureHandler validationsFailureHandler = new ValidationFailureHandler();

    router
        .get(api.getEntitiesEndpoint())
        .handler(entityQueryValidationHandler)
        .handler(AuthHandler.create(vertx, config()))
        .handler(this::handleEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler adminCrudPathValidationHandler =
        new ValidationHandler(vertx, RequestType.ADMIN_CRUD_PATH);

    ValidationHandler adminCrudPathIdValidationHandler =
        new ValidationHandler(vertx, RequestType.ADMIN_CRUD_PATH_DELETE);

    router
        .post(api.getAdminPath())
        .handler(adminCrudPathValidationHandler)
        .handler(AuthHandler.create(vertx, config()))
        .handler(this::handlePostAdminPath)
        .failureHandler(validationsFailureHandler);

    router
        .put(api.getAdminPath())
        .handler(adminCrudPathValidationHandler)
        .handler(AuthHandler.create(vertx, config()))
        .handler(this::handlePutAdminPath)
        .failureHandler(validationsFailureHandler);

    router
        .delete(api.getAdminPath())
        .handler(adminCrudPathIdValidationHandler)
        .handler(AuthHandler.create(vertx, config()))
        .handler(this::handleDeleteAdminPath)
        .failureHandler(validationsFailureHandler);
    router
        .get(ROUTE_STATIC_SPEC)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });
    /* Get redoc */
    router
        .get(ROUTE_DOC)
        .produces(MIME_TEXT_HTML)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });

    router
        .route()
        .last()
        .handler(
            requestHandler -> {
              HttpServerResponse response = requestHandler.response();
              response
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(404)
                  .end(generateResponse(HttpStatusCode.NOT_FOUND, YET_NOT_IMPLEMENTED));
            });

    new CatalogueService(vertx, config());

    /* Print the deployed endpoints */
    printDeployedEndpoints(router);
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.info("API Endpoints deployed : " + route.methods() + " : " + route.getPath());
      }
    }
  }

  private void handleDeleteAdminPath(RoutingContext routingContext) {
    LOGGER.trace("Info:handleDeleteAdminPath method started.;");
    HttpServerResponse response = routingContext.response();
    String resourceId = routingContext.queryParams().get("id");

    String adminDetailsQuery = PgsqlQueryBuilder.getAdminDetailsQuery(resourceId);
    String deleteAdminDetailsQuery = PgsqlQueryBuilder.deleteAdminDetailsQuery(resourceId);

    postgresService.executeQuery(
        adminDetailsQuery,
        selectHandler -> {
          if (selectHandler.succeeded()) {
            JsonObject result = selectHandler.result();
            JsonArray rows = result.getJsonArray("result");
            if (rows.size() < 1) {
              handleResponse(response, HttpStatusCode.NOT_FOUND, ResponseUrn.RESOURCE_NOT_FOUND);
            } else {
              postgresService.executeQuery(
                  deleteAdminDetailsQuery,
                  deleteHandler -> {
                    if (deleteHandler.succeeded()) {
                      handleResponse(response, HttpStatusCode.SUCCESS, ResponseUrn.SUCCESS);
                      routingContext.data().put(RESPONSE_SIZE, 0);
                      // Future.future(fu -> updateAuditTable(routingContext));
                    } else {
                      LOGGER.info("insert failed :{}", deleteHandler.cause().getMessage());
                    }
                  });
            }
          } else {
            LOGGER.info("select failed :{}", selectHandler.cause().getMessage());
          }
        });
  }

  private void handlePutAdminPath(RoutingContext routingContext) {
    LOGGER.trace("Info:handlePutAdminPath method started.;");
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = routingContext.body().asJsonObject();

    String resourceId = requestBody.getString("id");

    String adminDetailsQuery = PgsqlQueryBuilder.getAdminDetailsQuery(resourceId);
    String updateAdminDetailsQuery = PgsqlQueryBuilder.updateAdminDetailsQuery(requestBody);

    postgresService.executeQuery(
        adminDetailsQuery,
        selectHandler -> {
          if (selectHandler.succeeded()) {
            JsonObject result = selectHandler.result();
            JsonArray rows = result.getJsonArray("result");
            if (rows.size() < 1) {
              handleResponse(response, HttpStatusCode.NOT_FOUND, ResponseUrn.RESOURCE_NOT_FOUND);
            } else {
              postgresService.executeQuery(
                  updateAdminDetailsQuery,
                  updateHandler -> {
                    if (updateHandler.succeeded()) {
                      handleResponse(response, HttpStatusCode.SUCCESS, ResponseUrn.SUCCESS);
                      routingContext.data().put(RESPONSE_SIZE, 0);
                      // Future.future(fu -> updateAuditTable(routingContext));
                    } else {
                      LOGGER.info("insert failed :{}", updateHandler.cause().getMessage());
                    }
                  });
            }
          } else {
            LOGGER.info("select failed :{}", selectHandler.cause().getMessage());
          }
        });
  }

  private void handlePostAdminPath(RoutingContext routingContext) {
    LOGGER.trace("Info:handlePostAdminPath method started.;");
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = routingContext.body().asJsonObject();

    String resourceId = requestBody.getString("id");

    String adminDetailsQuery = PgsqlQueryBuilder.getAdminDetailsQuery(resourceId);
    String insertAdminDetailsQuery = PgsqlQueryBuilder.insertAdminDetailsQuery(requestBody);

    postgresService.executeQuery(
        adminDetailsQuery,
        selectHandler -> {
          if (selectHandler.succeeded()) {
            JsonObject result = selectHandler.result();
            JsonArray rows = result.getJsonArray("result");
            if (rows.size() > 0) {
              handleResponse(
                  response, HttpStatusCode.CONFLICT, ResponseUrn.RESOURCE_ALREADY_EXISTS);
            } else {
              postgresService.executeQuery(
                  insertAdminDetailsQuery,
                  insertHandler -> {
                    if (insertHandler.succeeded()) {
                      handleResponse(response, HttpStatusCode.CREATED, ResponseUrn.CREATED);
                      routingContext.data().put(RESPONSE_SIZE, 0);
                      // Future.future(fu -> updateAuditTable(routingContext));
                    } else {
                      LOGGER.info("insert failed :{}", insertHandler.cause().getMessage());
                    }
                  });
            }
          } else {
            LOGGER.info("select failed :{}", selectHandler.cause().getMessage());
          }
        });
  }

  private void handleEntitiesQuery(RoutingContext routingContext) {
    LOGGER.trace("Info:handleEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query paramaters
    String id = request.getParam("id");
    JsonObject json = new JsonObject();
    json.put("id", id);

    executeSearchQuery(routingContext, json, response);
  }

  /**
   * Execute a search query in DB
   *
   * @param json valid json query
   * @param response HttpServerResponse
   */
  private void executeSearchQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {

    String id = json.getString("id");
    String query = PgsqlQueryBuilder.getAdminDetailsQuery(id);

    postgresService.executeQuery(
        query,
        handler -> {
          JsonObject result = handler.result();
          if (handler.succeeded()) {
            LOGGER.debug("Success: Search Success");
            JsonArray rows = result.getJsonArray("result");
            if (rows.size() < 1) {
              handleResponse(
                  response, HttpStatusCode.NOT_FOUND, ResponseUrn.RESOURCE_NOT_FOUND);
            } else {
              handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result());
              context.data().put(RESPONSE_SIZE, response.bytesWritten());
              // Future.future(fu -> updateAuditTable(context));
            }
          } else if (handler.failed()) {
            LOGGER.error("Fail: Search Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void handleSuccessResponse(
      HttpServerResponse response, int statusCode, JsonObject result) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode)
        .end(result.toString());
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.trace("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = ResponseUrn.fromCode(urnTitle);
      } else {
        urn = ResponseUrn.fromCode(type + "");
      }
      String errorMessage = json.getString(ERROR_MESSAGE);
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(type);
      if (errorMessage == null || errorMessage.isEmpty()) {
        response.end(generateResponse(status, urn));
      } else {
        response.end(generateResponse(status, urn, errorMessage));
      }
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, HttpStatusCode.BAD_REQUEST, BACKING_SERVICE_FORMAT);
    }
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message));
  }

  private String generateResponse(HttpStatusCode statusCode, ResponseUrn urn) {
    return generateResponse(statusCode, urn, statusCode.getDescription());
  }

  private String generateResponse(HttpStatusCode statusCode, ResponseUrn urn, String message) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, message)
        .toString();
  }
}
