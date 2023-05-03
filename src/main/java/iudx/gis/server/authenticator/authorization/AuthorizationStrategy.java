package iudx.gis.server.authenticator.authorization;

import iudx.gis.server.authenticator.model.JwtData;

public interface AuthorizationStrategy {

  boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData);
}
