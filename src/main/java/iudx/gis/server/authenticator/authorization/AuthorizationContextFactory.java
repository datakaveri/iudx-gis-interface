package iudx.gis.server.authenticator.authorization;

import iudx.gis.server.common.Api;

public class AuthorizationContextFactory {

  //  private final static AuthorizationStrategy consumerAuth = new ConsumerAuthStrategy();

  public static AuthorizationStrategy create(IudxRole role, Api api) {
    if (role == null) {
      throw new IllegalArgumentException(role + "invalid role.");
    }
    switch (role) {
      case CONSUMER:
        return ConsumerAuthStrategy.getInstance(api);
      default:
        throw new IllegalArgumentException(role + "invalid role.");
    }
  }
}
