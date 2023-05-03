package iudx.gis.server.metering.util;

public class Constants {

  public static final String ID = "id";
  public static final String IID = "iid";
  public static final String ADMIN = "rs-admin";
  /* Errors */
  public static final String SUCCESS = "Success";
  public static final String DETAIL = "detail";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String PRIMARY_KEY = "primaryKey";
  public static final String PROVIDER_ID = "providerID";
  public static final String ORIGIN = "origin";
  public static final String ORIGIN_SERVER = "gis-server";
  public static final String EXCHANGE_NAME = "auditing";
  public static final String ROUTING_KEY = "#";

  // admin API endpoints
  public static final String ADMIN_BASE_PATH = "/admin/gis/serverInfo";
  public static final String RESPONSE_SIZE = "response_size";

  public static final String TABLE_NAME = "databaseTableName";

  /* Database */
  public static final String QUERY_KEY = "query";
  public static final String TOTAL = "total";
  public static final String TYPE_KEY = "type";
  public static final String API = "api";
  public static final String USER_ID = "userid";
  public static final String WRITE_QUERY =
      "INSERT INTO $0 (id,api,userid,epochtime,resourceid,isotime,providerid,size) "
          + "VALUES ('$1','$2','$3',$4,'$5','$6','$7',$8)";
  public static final String MESSAGE = "message";
}
