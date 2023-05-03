package iudx.gis.server.common;

import static iudx.gis.server.apiserver.util.Constants.*;

public class Api {

  private static volatile Api apiInstance;
  private final String dxApiBasePath;
  private final String adminBasePath;
  private StringBuilder entitiesEndpoint;
  private StringBuilder entitiesRegex;
  private StringBuilder adminPath;

  private Api(String dxApiBasePath, String adminBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    this.adminBasePath = adminBasePath;
    buildEndpoints();
  }

  public static Api getInstance(String dxApiBasePath, String adminBasePath) {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api(dxApiBasePath, adminBasePath);
        }
      }
    }
    return apiInstance;
  }

  public void buildEndpoints() {
    entitiesEndpoint = new StringBuilder(dxApiBasePath).append(NGSILD_ENTITIES_URL);
    entitiesRegex = new StringBuilder(dxApiBasePath).append(ENTITITES_URL_REGEX);
    adminPath = new StringBuilder(adminBasePath).append(ADMIN_PATH);
  }

  public String getEntitiesEndpoint() {
    return entitiesEndpoint.toString();
  }

  public String getEntitiesRegex() {
    return entitiesRegex.toString();
  }

  public String getAdminPath() {
    return adminPath.toString();
  }
}
