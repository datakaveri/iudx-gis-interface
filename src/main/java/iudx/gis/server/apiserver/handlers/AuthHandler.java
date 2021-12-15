package iudx.gis.server.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.gis.server.apiserver.response.ResponseUrn;
import iudx.gis.server.apiserver.util.HttpStatusCode;
import iudx.gis.server.authenticate.AuthenticatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static iudx.gis.server.apiserver.response.ResponseUrn.INVALID_TOKEN;
import static iudx.gis.server.apiserver.response.ResponseUrn.RESOURCE_NOT_FOUND;
import static iudx.gis.server.apiserver.util.Constants.*;

public class AuthHandler implements Handler<RoutingContext> {

  private static AuthenticatorService authenticator;
  private static final String AUTH_SERVICE_ADDRESS = "iudx.gis.authentication.service";
  private HttpServerRequest request;

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  public static AuthHandler create(Vertx vertx) {
    authenticator = AuthenticatorService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
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

    if (token == null)
      token = "public";

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    LOGGER.debug("Info :" + context.request().path());
    LOGGER.debug("Info :" + context.request().path().split("/").length);

    String pathId = getId4rmPath(context);
    LOGGER.info("id from path : " + pathId);
    String paramId = getId4rmRequest();
    LOGGER.info("id from param : " + paramId);
    String bodyId = getId4rmBody(context, path);
    LOGGER.info("id from body : " + bodyId);

    String id;
    if (pathId != null && !pathId.isBlank()) {
      id = pathId;
    } else {
      if (paramId != null && !paramId.isBlank()) {
        id = paramId;
      } else {
        id = bodyId;
      }
    }
    LOGGER.info("id : " + id);

    LOGGER.debug("Request JSON: {}", requestJson);
    authenticator.tokenInterospect(requestJson, authInfo, authHandler -> {
      if (authHandler.succeeded()) {
        authInfo.put(USER_ID, authHandler.result().getValue(USER_ID));
        context.data().put(AUTH_INFO, authInfo);
      } else {
        processAuthFailure(context, authHandler.cause().getMessage());
        return;
      }
      context.next();
    });
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
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

  private String getNormalizedPath(String url) {
    LOGGER.debug("URL : {}", url);
    String path = null;
    if (url.matches(ENTITITES_URL_REGEX)) {
      path = NGSILD_ENTITIES_URL;
    } else if (url.matches(ADMIN_URL_REGEX)) {
      path = ADMIN_BASE_PATH;
    }
    return path;
  }

  private String getId4rmPath(RoutingContext context) {
    StringBuilder id = null;
    Map<String, String> pathParams = context.pathParams();
    LOGGER.info("path params :" + pathParams);
    if (pathParams != null && !pathParams.isEmpty()) {
      if (pathParams.containsKey(DOMAIN)
          && pathParams.containsKey(USERSHA)
          && pathParams.containsKey(RESOURCE_SERVER)
          && pathParams.containsKey(RESOURCE_GROUP)) {
        id = new StringBuilder();
        id.append(pathParams.get(DOMAIN));
        id.append("/").append(pathParams.get(USERSHA));
        id.append("/").append(pathParams.get(RESOURCE_SERVER));
        id.append("/").append(pathParams.get(RESOURCE_GROUP));
        if (pathParams.containsKey(RESOURCE_NAME)) {
          id.append("/").append(pathParams.get(RESOURCE_NAME));
        }
        LOGGER.info("id :" + id.toString());
      } else if (pathParams.containsKey(USER_ID) && pathParams.containsKey(JSON_ALIAS)) {
        id = new StringBuilder();
        id.append(pathParams.get(USER_ID))
            .append("/")
            .append(pathParams.get(JSON_ALIAS));
      }

    }
    LOGGER.info("id :" + id);
    return id != null ? id.toString() : null;
  }

  private String getId4rmRequest() {
    return request.getParam(ID);
  }

  private String getId4rmBody(RoutingContext context, String api) {
    JsonObject body = context.getBodyAsJson();
    String id = null;
    if (body != null) {
      JsonArray array = body.getJsonArray(JSON_ENTITIES);
      if (array != null) {
          JsonObject json = array.getJsonObject(0);
          if (json != null) {
            id = json.getString(ID);
        }
      }
    }
    LOGGER.info("id : " + id);
    return id;
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
