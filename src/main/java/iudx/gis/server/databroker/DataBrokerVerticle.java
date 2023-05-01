package iudx.gis.server.databroker;

import static iudx.gis.server.common.Constants.DATABROKER_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.gis.server.cache.CacheService;
import iudx.gis.server.common.VHosts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerVerticle.class);

  private RabbitMQOptions config;
  private RabbitMQClient client;
  private DataBrokerService databroker;
  private String dataBrokerIP;
  private int dataBrokerPort;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private boolean automaticRecoveryEnabled;
  private String virtualHost;

  private ServiceBinder binder;

  @Override
  public void start() throws Exception {

    dataBrokerIP = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");
    automaticRecoveryEnabled = config().getBoolean("automaticRecoveryEnabled");
    virtualHost = config().getString(VHosts.IUDX_INTERNAL.value);

    /* Configure the RabbitMQ Data Broker client with input from config files. */

    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIP);
    config.setPort(dataBrokerPort);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
    config.setVirtualHost(virtualHost);

    client = RabbitMQClient.create(vertx, config);
    databroker = new DataBrokerServiceImpl(client);
    binder = new ServiceBinder(vertx);
    binder.setAddress(DATABROKER_SERVICE_ADDRESS).register(DataBrokerService.class, databroker);

    LOGGER.info("Data-broker verticle started.");
  }
}
