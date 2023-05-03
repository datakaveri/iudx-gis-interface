package iudx.gis.server.authenticator.authorization;

import static iudx.gis.server.authenticator.authorization.Method.GET;

import io.vertx.core.json.JsonArray;
import iudx.gis.server.authenticator.model.JwtData;
import iudx.gis.server.common.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  static Api api;
  private static volatile ConsumerAuthStrategy instance;

  private ConsumerAuthStrategy(Api apis) {
    api = apis;
    buildPermissions(api);
  }

  public static ConsumerAuthStrategy getInstance(Api apis) {
    if (instance == null) {
      synchronized (ConsumerAuthStrategy.class) {
        if (instance == null) {
          instance = new ConsumerAuthStrategy(apis);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {
    // api access list/rules
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(GET, api.getEntitiesEndpoint()));
    consumerAuthorizationRules.put(IudxAccess.API.getAccess(), apiAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
    boolean result = false;
    if (access == null) {
      return result;
    }
    String endpoint = authRequest.getApi();
    Method method = authRequest.getMethod();
    LOGGER.debug("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.debug("allowed access : " + access);

    if (!result && access.contains(IudxAccess.API.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.API.getAccess()).contains(authRequest);
    }
    return result;
  }
}
