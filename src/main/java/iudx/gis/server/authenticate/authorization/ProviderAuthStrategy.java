package iudx.gis.server.authenticate.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.gis.server.authenticate.model.JwtData;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  static {
    //provider allowed to access all endpoints
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

}
