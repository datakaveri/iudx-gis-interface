package iudx.gis.server.authenticate.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class Constants {
  public static final String CONFIG_FILE = "config.properties";
  public static final String KEYSTORE_PATH = "keystore";
  public static final String KEYSTORE_PASSWORD = "keystorePassword";
  public static final String AUTH_SERVER_HOST = "authServerHost";
  public static final String AUTH_CERTINFO_PATH = "/auth/v1/certificate-info";
  public static final String PUBLIC_TOKEN = "public";
  public static final String AUTH_TIP_PATH = "/auth/v1/token/introspect";
  public static final long CACHE_TIMEOUT_AMOUNT = 30;
  public static final ChronoUnit TIP_CACHE_TIMEOUT_UNIT = ChronoUnit.MINUTES;
  public static final String CAT_RSG_PATH = "/iudx/cat/v1/search";
  public static final String CAT_ITEM_PATH = "/iudx/cat/v1/item";
  public static final String SERVER_MODE = "serverMode";

  public static final String AUDIENCE = "host";
  public static final String PORT = "catServerPort";
  public static final String HOST = "catServerHost";

  public static final String ENDPOINT = "apiEndpoint";
  public static final String ID = "id";
  public static final String TOKEN = "token";
  public static final String METHOD = "method";
}
