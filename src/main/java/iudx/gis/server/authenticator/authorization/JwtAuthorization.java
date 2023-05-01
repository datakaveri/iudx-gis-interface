package iudx.gis.server.authenticator.authorization;

import iudx.gis.server.authenticator.model.JwtData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JwtAuthorization {
  private final AuthorizationStrategy authStrategy;

  public JwtAuthorization(final AuthorizationStrategy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return authStrategy.isAuthorized(authRequest, jwtData);
  }
  
}
