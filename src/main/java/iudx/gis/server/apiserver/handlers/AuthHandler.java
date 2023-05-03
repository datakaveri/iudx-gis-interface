package iudx.gis.server.apiserver.handlers;

import static iudx.gis.server.apiserver.response.ResponseUrn.INVALID_TOKEN;
import static iudx.gis.server.apiserver.response.ResponseUrn.RESOURCE_NOT_FOUND;
import static iudx.gis.server.apiserver.util.Constants.*;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.gis.server.apiserver.response.ResponseUrn;
import iudx.gis.server.apiserver.util.HttpStatusCode;
import iudx.gis.server.authenticator.AuthenticationService;
import iudx.gis.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthHandler implements Handler<RoutingContext> {

  private static final String AUTH_SERVICE_ADDRESS = "iudx.gis.authentication.service";
  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);
  static AuthenticationService authenticator;
  private static String dxApiBasePath;
  private static String adminBasePath;
  private static Api api;
  private HttpServerRequest request;

  public static AuthHandler create(Vertx vertx, JsonObject config) {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    dxApiBasePath = config.getString("dxApiBasePath");
    adminBasePath = config.getString("adminBasePath");
    api = Api.getInstance(dxApiBasePath, adminBasePath);
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();

    JsonObject requestJson = context.getBodyAsJson();

    if (requestJson == null) {
      requestJson = new JsonObject();
    }

    LOGGER.debug("Info : path " + request.path());

    String token = request.headers().get(HEADER_TOKEN);
    final String path = getNormalizedPath(request.path());
    final String method = context.request().method().toString();

    if (token == null) {
      token = "public";
    }

    String paramId = getId4rmRequest();

    String id = null;

    if (paramId != null && !paramId.isBlank()) {
      id = paramId;
    }

    JsonObject authInfo =
        new JsonObject()
            .put(API_ENDPOINT, path)
            .put(HEADER_TOKEN, token)
            .put(API_METHOD, method)
            .put(ID, id);

    LOGGER.debug("Info :" + context.request().path());
    LOGGER.debug("Info :" + context.request().path().split("/").length);

    authenticator.tokenIntrospect(
        requestJson,
        authInfo,
        authHandler -> {
          if (authHandler.succeeded()) {
            authInfo.put(IID, authHandler.result().getValue(IID));
            authInfo.put(USER_ID, authHandler.result().getValue(USER_ID));
            authInfo.put(EXPIRY, authHandler.result().getValue(EXPIRY));
            context.data().put(AUTH_INFO, authInfo);
          } else {
            processAuthFailure(context, authHandler.cause().getMessage());
            return;
          }
          context.next();
        });
  }

  public void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(RESOURCE_NOT_FOUND, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN, statusCode).toString());
    }
  }

  public String getNormalizedPath(String url) {
    LOGGER.debug("URL : {}", url);
    String path = null;
    if (url.matches(api.getEntitiesEndpoint())) {
      path = api.getEntitiesEndpoint();
    } else if (url.matches(api.getAdminPath())) {
      path = api.getAdminPath();
    }
    return path;
  }

  private String getId4rmRequest() {
    return request.getParam(ID);
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
