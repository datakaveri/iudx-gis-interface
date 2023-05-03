package iudx.gis.server.metering;

import static iudx.gis.server.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.gis.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private MeteringService metering;
  private DataBrokerService dataBrokerService;
  private JsonObject config;

  @Override
  public void start() {

    config = config();
    dataBrokerService = DataBrokerService.createProxy(vertx, DATABROKER_SERVICE_ADDRESS);
    metering = new MeteringServiceImpl(dataBrokerService, config);
    binder = new ServiceBinder(vertx);
    consumer =
        binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, metering);
    LOGGER.info("Metering Verticle Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
