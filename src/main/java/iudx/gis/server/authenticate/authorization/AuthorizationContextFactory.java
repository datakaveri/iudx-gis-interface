package iudx.gis.server.authenticate.authorization;

public class AuthorizationContextFactory {

  private final static AuthorizationStrategy consumerAuth = new ConsumerAuthStrategy();
  private final static AuthorizationStrategy providerAuth = new ProviderAuthStrategy();
  private final static AuthorizationStrategy delegateAuth = new DelegateAuthStrategy();
  private final static AuthorizationStrategy adminAuth = new AdminAuthStrategy();

  public static AuthorizationStrategy create(IudxRole role) {
    switch (role) {
      case CONSUMER: {
        return consumerAuth;
      }
      case PROVIDER: {
        return providerAuth;
      }
      case DELEGATE: {
        return delegateAuth;
      }
      case ADMIN: {
        return adminAuth;
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}
