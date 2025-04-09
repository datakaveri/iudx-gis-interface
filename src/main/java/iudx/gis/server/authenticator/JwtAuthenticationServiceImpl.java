package iudx.gis.server.authenticator;

import static iudx.gis.server.authenticator.Constants.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.gis.server.authenticator.authorization.AuthorizationContextFactory;
import iudx.gis.server.authenticator.authorization.AuthorizationRequest;
import iudx.gis.server.authenticator.authorization.AuthorizationStrategy;
import iudx.gis.server.authenticator.authorization.IudxRole;
import iudx.gis.server.authenticator.authorization.JwtAuthorization;
import iudx.gis.server.authenticator.authorization.Method;
import iudx.gis.server.authenticator.model.JwtData;
import iudx.gis.server.cache.CacheService;
import iudx.gis.server.cache.cacheimpl.CacheType;
import iudx.gis.server.common.Api;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final WebClient catWebClient;
  final String host;
  final int port;
  final String path;
  final String audience;
  final String iss;
  final CacheService cache;
  final String adminBasePath;
  final Api api;
  final String catBasePath;
  // resourceIdCache will contain info about resources available(& their ACL) in resource server.
  public Cache<String, String> resourceIdCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES)
          .build();

  public JwtAuthenticationServiceImpl(
      Vertx vertx,
      final JWTAuth jwtAuth,
      final JsonObject config,
      final Api api,
      final CacheService cacheService) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("audience");
    this.iss = config.getString("iss");
    this.host = config.getString("catServerHost");
    this.port = config.getInteger("catServerPort");
    this.catBasePath = config.getString("dxCatalogueBasePath");
    this.path = catBasePath + CAT_SEARCH_PATH;
    this.cache = cacheService;
    this.api = api;
    this.adminBasePath = config.getString("adminBasePath");
    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
  }

  @Override
  public AuthenticationService tokenIntrospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {

    String id = authenticationInfo.getString("id");
    String token = authenticationInfo.getString("token");
    String endPoint = authenticationInfo.getString("apiEndpoint");
    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    ResultContainer result = new ResultContainer();
    if (endPoint != null && endPoint.equals(api.getAdminPath())) {
      jwtDecodeFuture
          .compose(
              decodeHandler -> {
                result.jwtData = decodeHandler;
                return isValidAudienceValue(result.jwtData);
              })
          .compose(
              audienceHandler -> {
                if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                  return isRevokedClientToken(result.jwtData);
                } else {
                  return Future.succeededFuture(true);
                }
              })
          .compose(revokeClientTokenHandler -> isValidIid(result.jwtData))
          .compose(idHandler -> isValidRole(result.jwtData))
          .onSuccess(successHandler -> handler.handle(Future.succeededFuture(successHandler)))
          .onFailure(
              failureHandler -> handler.handle(Future.failedFuture(failureHandler.getMessage())));
      return this;
    } else {
      jwtDecodeFuture
          .compose(
              decodeHandler -> {
                result.jwtData = decodeHandler;
                return isValidAudienceValue(result.jwtData);
              })
          .compose(
              audienceHandler -> {
                if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                  return isRevokedClientToken(result.jwtData);
                } else {
                  return Future.succeededFuture(true);
                }
              })
          .compose(revokeClientTokenHandler -> isValidIssuerValue(result.jwtData))
          .compose(
              issuerHandler -> {
                if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                  return isOpenResource(id);
                } else {
                  return Future.succeededFuture("OPEN");
                }
              })
          .compose(
              openResourceHandler -> {
                result.isOpen = openResourceHandler.equalsIgnoreCase("OPEN");
                if (result.isOpen) {
                  JsonObject json = new JsonObject();
                  json.put(JSON_USERID, result.jwtData.getSub());
                  return Future.succeededFuture(true);
                } else {
                  return isValidId(result.jwtData, id);
                }
              })
          .compose(
              validIdHandler -> validateAccess(result.jwtData, result.isOpen, authenticationInfo))
          .onSuccess(successHandler -> handler.handle(Future.succeededFuture(successHandler)))
          .onFailure(
              failureHandler -> handler.handle(Future.failedFuture(failureHandler.getMessage())));
    }
    return this;
  }

  public Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed");
            });

    return promise.future();
  }

  private Future<String> isOpenResource(String id) {
    LOGGER.trace("isOpenResource() started");
    Promise<String> promise = Promise.promise();
    String acl = resourceIdCache.getIfPresent(id);
    if (acl != null) {
      LOGGER.info("Cache Hit");
      promise.complete(acl);
    } else {
      LOGGER.info("Cache miss calling cat server");
      Future<String> aclFuture = getAccessPolicy(id);
      aclFuture
          .onSuccess(
              accessPolicy -> {
                promise.complete(accessPolicy);
              })
          .onFailure(
              failure -> {
                promise.fail(failure.getMessage());
              });
    }
    return promise.future();
  }

  public Future<JsonObject> validateAccess(
      JwtData jwtData, boolean openResource, JsonObject authInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];

    if (jwtData.getRole().equals("consumer")) {

      if (openResource && checkOpenEndPoints(authInfo.getString("apiEndpoint"))) {
        LOGGER.debug("IS OPEN");
        LOGGER.debug("User access is allowed.");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.put(JSON_IID, jwtId);
        jsonResponse.put(JSON_USERID, jwtData.getSub());
        return Future.succeededFuture(jsonResponse);
      }

      Method method = Method.valueOf(authInfo.getString("method"));
      String apiEndpoint = authInfo.getString("apiEndpoint");
      AuthorizationRequest authRequest = new AuthorizationRequest(method, apiEndpoint);

      IudxRole role = IudxRole.fromRole(jwtData.getRole());
      AuthorizationStrategy authStrategy = AuthorizationContextFactory.create(role, api);
      LOGGER.debug("strategy : " + authStrategy.getClass().getSimpleName());
      JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
      LOGGER.debug("endPoint : " + authInfo.getString("apiEndpoint"));
      if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
        LOGGER.debug("User access is allowed.");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.put(JSON_USERID, jwtData.getSub());
        jsonResponse.put(JSON_IID, jwtId);
        jsonResponse.put(
            JSON_EXPIRY,
            LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                    ZoneId.systemDefault())
                .toString());
        promise.complete(jsonResponse);
      } else {
        LOGGER.debug("failed");
        JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
        promise.fail(result.toString());
      }
    } else {
      LOGGER.debug("failed");
      JsonObject result = new JsonObject().put("401", "only consumer access allowed.");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  private boolean checkOpenEndPoints(String endPoint) {
    for (String item : OPEN_ENDPOINTS) {
      if (endPoint.contains(item)) {
        return true;
      }
    }
    return false;
  }

  public Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();

    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidIid(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];

    if (audience != null && audience.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect iid value in jwt");
      promise.fail("Incorrect iid value in jwt");
    }
    return promise.future();
  }

  Future<JsonObject> isValidRole(JwtData jwtData) {
    Promise<JsonObject> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];
    String role = jwtData.getRole();
    JsonObject jsonResponse = new JsonObject();
    if (role.equalsIgnoreCase("admin")) {
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(JSON_IID, jwtId);
      promise.complete(jsonResponse);
    } else {
      LOGGER.debug("failed");
      JsonObject result = new JsonObject().put("401", "Only admin access allowed.");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  Future<Boolean> isValidIssuerValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (iss != null && iss.equalsIgnoreCase(jwtData.getIss())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect iss value in jwt");
      promise.fail("Incorrect iss value in jwt");
    }
    return promise.future();
  }

  public Future<Boolean> isValidId(JwtData jwtData, String id) {
    LOGGER.trace("Is Valid Started. ");
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

  Future<Boolean> isRevokedClientToken(JwtData jwtData) {
    LOGGER.trace("isRevokedClientToken started param : ");
    Promise<Boolean> promise = Promise.promise();
    CacheType cacheType = CacheType.REVOKED_CLIENT;
    String subId = jwtData.getSub();
    JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", subId);

    cache.get(
        requestJson,
        handler -> {
          if (handler.succeeded()) {
            JsonObject responseJson = handler.result();
            String timestamp = responseJson.getString("value");

            LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
            LOGGER.info("token revoked At: {}", revokedAt);

            LocalDateTime jwtIssuedAt =
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(jwtData.getIat()), ZoneId.systemDefault());

            LOGGER.info(" Token issued At: {}", jwtIssuedAt);

            if (jwtIssuedAt.isBefore(revokedAt)) {
              LOGGER.error("Privileges for client are revoked.");
              JsonObject result = new JsonObject().put("401", "revoked token passes");
              promise.fail(result.toString());
            } else {
              promise.complete(true);
            }
          } else {
            // since no value in cache, this means client_id is valid and not revoked
            LOGGER.error("cache call result : [fail] " + handler.cause());
            promise.complete(true);
          }
        });
    return promise.future();
  }

  public Future<String> getAccessPolicy(String id) {
    LOGGER.trace("getGroupAccessPolicy() started");
    Promise<String> promise = Promise.promise();

    catWebClient
        .get(port, host, path)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + id + "]]")
        .addQueryParam("filter", "[accessPolicy]")
        .expect(ResponsePredicate.JSON)
        .send(
            httpResponseAsyncResult -> {
              if (httpResponseAsyncResult.failed()) {
                LOGGER.error(httpResponseAsyncResult.cause());
                promise.fail("Resource not found");
                return;
              }
              HttpResponse<Buffer> response = httpResponseAsyncResult.result();
              if (response.statusCode() != HttpStatus.SC_OK) {
                promise.fail("Resource not found");
                return;
              }
              JsonObject responseBody = response.bodyAsJsonObject();
              if (!responseBody.getString("type").equals("urn:dx:cat:Success")) {
                promise.fail("Resource not found");
                return;
              }
              String resourceAcl = "SECURE";
              try {
                resourceAcl =
                    responseBody.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
                resourceIdCache.put(id, resourceAcl);
                LOGGER.debug("Info: Resource ID is valid : Catalogue item Found");
                promise.complete(resourceAcl);
              } catch (Exception ignored) {
                LOGGER.error(ignored.getMessage());
                LOGGER.error(
                    "Info: Resource ID is invalid : Empty response in results from Catalogue");
                promise.fail("Resource not found");
              }
            });
    return promise.future();
  }

  // class to contain intermediate data for token interospection
  final class ResultContainer {
    JwtData jwtData;
    boolean isOpen;
  }
}
