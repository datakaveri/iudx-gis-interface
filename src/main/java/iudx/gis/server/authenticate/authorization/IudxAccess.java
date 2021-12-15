package iudx.gis.server.authenticate.authorization;

import java.util.stream.Stream;

public enum IudxAccess {

  API("api"),
  SUBSCRIPTION("sub"),
  INGESTION("ingestion"),
  MANAGEMENT("management");

  private final String access;

  IudxAccess(String access) {
    this.access = access;
  }

  public String getAccess() {
    return this.access;
  }

  public static IudxAccess fromAccess(final String access) {
    return Stream.of(values())
        .filter(v -> v.access.equalsIgnoreCase(access))
        .findAny()
        .orElse(null);
  }

}
