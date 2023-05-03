package iudx.gis.server.apiserver.query;

import static iudx.gis.server.common.Constants.*;
import static iudx.gis.server.database.util.Constants.ACCESS_INFO;
import static iudx.gis.server.database.util.Constants.ID;
import static iudx.gis.server.database.util.Constants.PASSWORD;
import static iudx.gis.server.database.util.Constants.SECURE;
import static iudx.gis.server.database.util.Constants.SERVER_PORT;
import static iudx.gis.server.database.util.Constants.SERVER_URL;
import static iudx.gis.server.database.util.Constants.TOKEN_URL;
import static iudx.gis.server.database.util.Constants.USERNAME;

import io.vertx.core.json.JsonObject;
import java.util.Optional;

public class PgsqlQueryBuilder {

  public static String getAdminDetailsQuery(String resourceId) {
    return SELECT_ADMIN_DETAILS_QUERY.replace("$1", resourceId);
  }

  public static String insertAdminDetailsQuery(JsonObject queryParams) {

    String resourceId = queryParams.getString(ID);
    String serverUrl = queryParams.getString(SERVER_URL);
    Long serverPort = queryParams.getLong(SERVER_PORT);
    Boolean isSecure = queryParams.getBoolean(SECURE);

    Optional<JsonObject> accessInfo = Optional.ofNullable(queryParams.getJsonObject(ACCESS_INFO));

    String insertQuery =
        INSERT_ADMIN_DETAILS_QUERY
            .replace("$1", resourceId)
            .replace("$2", serverUrl)
            .replace("$3", serverPort.toString())
            .replace("$4", isSecure.toString());

    if (accessInfo.isPresent() && !accessInfo.get().isEmpty()) {
      JsonObject accessObject = accessInfo.get();
      String username = accessObject.getString(USERNAME);
      String password = accessObject.getString(PASSWORD);
      String tokenUrl = accessObject.getString(TOKEN_URL);
      insertQuery =
          insertQuery.replace("$5", username).replace("$6", password).replace("$7", tokenUrl);
    } else {
      insertQuery = insertQuery.replace("$5", "").replace("$6", "").replace("$7", "");
    }

    return insertQuery;
  }

  public static String updateAdminDetailsQuery(JsonObject queryParams) {

    String resourceId = queryParams.getString(ID);
    String serverUrl = queryParams.getString(SERVER_URL);
    Long serverPort = queryParams.getLong(SERVER_PORT);
    Boolean isSecure = queryParams.getBoolean(SECURE);

    Optional<JsonObject> accessInfo = Optional.ofNullable(queryParams.getJsonObject(ACCESS_INFO));

    String updateQuery =
        UPDATE_ADMIN_DETAILS_QUERY
            .replace("$1", serverUrl)
            .replace("$2", serverPort.toString())
            .replace("$3", isSecure.toString())
            .replace("$6", resourceId);

    if (accessInfo.isPresent() && !accessInfo.get().isEmpty()) {
      JsonObject accessObject = accessInfo.get();
      String username = accessObject.getString(USERNAME);
      String password = accessObject.getString(PASSWORD);
      String tokenUrl = accessObject.getString(TOKEN_URL);
      updateQuery =
          updateQuery.replace("$4", username).replace("$5", password).replace("$7", tokenUrl);
    } else {
      updateQuery = updateQuery.replace("$4", "").replace("$5", "").replace("$7", "");
    }

    return updateQuery;
  }

  public static String deleteAdminDetailsQuery(String resourceId) {
    return DELETE_ADMIN_DETAILS_QUERY.replace("$1", resourceId);
  }
}
