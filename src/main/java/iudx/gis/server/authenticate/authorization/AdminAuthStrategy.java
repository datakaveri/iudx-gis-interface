package iudx.gis.server.authenticate.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.gis.server.authenticate.model.JwtData;

public class AdminAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(AdminAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> adminAuthorizationRules = new HashMap<>();
  static {
    //TODO: add admin auth rules
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

}
