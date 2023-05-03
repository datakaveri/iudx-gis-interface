package iudx.gis.server.common;

public class Constants {

  /** service proxy addresses * */
  public static final String PG_SERVICE_ADDRESS = "iudx.gis.pgsql.service";

  public static final String CACHE_SERVICE_ADDRESS = "iudx.gis.cache.service";
  public static final String AUTHENTICATION_SERVICE_ADDRESS = "iudx.gis.authentication.service";
  public static final String DATABASE_SERVICE_ADDRESS = "iudx.gis.database.service";
  public static final String DATABROKER_SERVICE_ADDRESS = "iudx.gis.broker.service";
  public static final String METERING_SERVICE_ADDRESS = "iudx.gis.metering.service";
  // RMQ
  public static final String GIS_INVALID_SUB = "gis-invalid-sub";
  // queries
  public static final String TABLE_NAME = "gis";
  public static final String SELECT_ADMIN_DETAILS_QUERY =
      "SELECT * FROM " + TABLE_NAME + " WHERE iudx_resource_id = '$1'";
  public static final String INSERT_ADMIN_DETAILS_QUERY =
      "INSERT INTO "
          + TABLE_NAME
          + "(iudx_resource_id, url, port, isOpen, username, password,tokenurl) "
          + "VALUES ('$1', '$2', '$3', '$4', '$5', '$6','$7')";
  public static final String UPDATE_ADMIN_DETAILS_QUERY =
      "UPDATE "
          + TABLE_NAME
          + " SET url='$1', port='$2', isOpen='$3', username='$4', password='$5', tokenurl='$7' "
          + "WHERE iudx_resource_id='$6'";
  public static final String DELETE_ADMIN_DETAILS_QUERY =
      "DELETE FROM " + TABLE_NAME + " WHERE iudx_resource_id = '$1'";
  // postgres queries
  public static String SELECT_REVOKE_TOKEN_SQL = "SELECT * FROM revoked_tokens";
}
