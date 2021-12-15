package iudx.gis.server.authenticate.authorization;

import iudx.gis.server.authenticate.model.JwtData;

public interface AuthorizationStrategy {

  boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData);

}
