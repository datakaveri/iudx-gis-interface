package iudx.gis.server.authenticate;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.gis.server.authenticate.authorization.*;
import iudx.gis.server.authenticate.model.JwtData;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.gis.server.authenticate.util.Constants.*;

public class AuthenticatorServiceImpl implements AuthenticatorService {

  final JWTAuth jwtAuth;
  final String host;
  final int port;
  final String path;
  final String audience;
  final WebClient catWebClient;

  private static final Logger LOGGER = LogManager.getLogger(AuthenticatorServiceImpl.class);

  public AuthenticatorServiceImpl(Vertx vertx, JWTAuth auth, WebClient client, JsonObject config) {
    jwtAuth = auth;
    host = config.getString(HOST);
    port = config.getInteger(PORT);
    path = CAT_RSG_PATH;
    audience = config.getString(AUDIENCE);

    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true)
        .setVerifyHost(false)
        .setSsl(true);
    catWebClient = WebClient.create(vertx, options);
  }

  @Override
  public AuthenticatorService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler) {

    String endPoint = authenticationInfo.getString(ENDPOINT);
    String id = authenticationInfo.getString(ID);
    String token = authenticationInfo.getString(TOKEN);
    String method = authenticationInfo.getString(METHOD);

    ResultContainer result = new ResultContainer();

    return this;
  }

  // Class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
    boolean isResourceExist;
    boolean isOpen;
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth.authenticate(creds)
        .onSuccess(user -> {
          JwtData jwtData = new JwtData(user.principal());
          promise.complete(jwtData);
        }).onFailure(err -> {
      LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
      promise.fail("failed");
    });

    return promise.future();
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidId(JwtData jwtData, String id) {
    Promise<Boolean> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];
    if (id.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect id value in jwt");
      promise.fail("Incorrect id value in jwt");
    }

    return promise.future();
  }

  public Future<JsonObject> validateAccess(JwtData jwtData, boolean openResource, JsonObject authInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();

    Method method = Method.valueOf(authInfo.getString("method"));
    Api api = Api.fromEndpoint(authInfo.getString("apiEndpoint"));
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    IudxRole role = IudxRole.fromRole(jwtData.getRole());
    AuthorizationStrategy authStrategy = AuthorizationContextFactory.create(role);
    LOGGER.info("strategy : " + authStrategy.getClass().getSimpleName());
    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.info("endPoint : " + authInfo.getString("apiEndpoint"));
    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      LOGGER.info("User access is allowed.");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      promise.complete(jsonResponse);
    } else {
      LOGGER.info("failed");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  private Future<Boolean> isResourceExist(String id, String groupACL) {
    LOGGER.debug("isResourceExist() started");
    Promise<Boolean> promise = Promise.promise();

    catWebClient.get(port, host, path).addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + id + "]]").addQueryParam("filter", "[id]")
        .expect(ResponsePredicate.JSON).send(responseHandler -> {
      if (responseHandler.failed()) {
        promise.fail("false");
      }
      HttpResponse<Buffer> response = responseHandler.result();
      JsonObject responseBody = response.bodyAsJsonObject();
      if (response.statusCode() != HttpStatus.SC_OK) {
        promise.fail("false");
      } else if (!responseBody.getString("status").equals("success")) {
        promise.fail("Not Found");
        return;
      } else if (responseBody.getInteger("totalHits") == 0) {
        LOGGER.debug("Info: Resource ID invalid : Catalogue item Not Found");
        promise.fail("Not Found");
      } else {
        LOGGER.debug("is Exist response : " + responseBody);
        promise.complete(true);
      }
    });

    return promise.future();
  }
}
