package iudx.gis.server.cache;

import static iudx.gis.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.gis.server.common.Constants.PG_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.gis.server.database.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(CacheVerticle.class);
  private static final String SERVICE_ADDRESS = CACHE_SERVICE_ADDRESS;
  private ServiceBinder binder;
  private CacheService cacheService;
  private PostgresService pgService;

  @Override
  public void start() throws Exception {

    pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);

    cacheService = new CacheServiceImpl(vertx, pgService);

    binder = new ServiceBinder(vertx);
    binder.setAddress(SERVICE_ADDRESS).register(CacheService.class, cacheService);

    LOGGER.info("Cache Verticle deployed.");
  }
}
