package iudx.gis.server.database.util;

import static iudx.gis.server.database.util.Constants.*;

import io.vertx.core.json.JsonObject;
import iudx.gis.server.apiserver.util.HttpStatusCode;

public class Util {
  public static JsonObject getResponse(HttpStatusCode code, String urn, String error) {
    return new JsonObject()
        .put(TYPE, code.getValue())
        .put(TITLE, urn)
        .put(ERROR_MESSAGE, error);
  }
}
