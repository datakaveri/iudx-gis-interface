package iudx.gis.server.authenticate;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.gis.server.authenticate.util.Constants.*;

public class AuthenticatorServiceImpl implements AuthenticatorService {

  final String host;
  final int port;
  final String path;
  final String audience;

  private static final Logger LOGGER = LogManager.getLogger(AuthenticatorServiceImpl.class);

  public AuthenticatorServiceImpl(Vertx vertx, WebClient client, JsonObject config) {
    host = config.getString(HOST);
    port = config.getInteger(PORT);
    path = CAT_RSG_PATH;
    audience = config.getString(AUDIENCE);
  }

  @Override
  public AuthenticatorService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler) {

    String endPoint = authenticationInfo.getString(ENDPOINT);
    String id = authenticationInfo.getString(ID);
    String token = authenticationInfo.getString(TOKEN);
    String method = authenticationInfo.getString(METHOD);

    return this;
  }
}
