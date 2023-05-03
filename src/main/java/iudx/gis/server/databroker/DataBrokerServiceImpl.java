package iudx.gis.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.gis.server.common.Response;
import iudx.gis.server.common.ResponseUrn;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerServiceImpl implements DataBrokerService {

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  private final RabbitMQClient client;

  public DataBrokerServiceImpl(RabbitMQClient webClient) {
    this.client = webClient;
  }

  @Override
  public DataBrokerService publishMessage(
      JsonObject body,
      String toExchange,
      String routingKey,
      Handler<AsyncResult<JsonObject>> handler) {
    Future<Void> rabbitMqClientStartFuture;

    Buffer buffer = Buffer.buffer(body.toString());

    if (!client.isConnected()) {
      rabbitMqClientStartFuture = client.start();
    } else {
      rabbitMqClientStartFuture = Future.succeededFuture();
    }

    rabbitMqClientStartFuture
        .compose(rabbitStartupFuture -> client.basicPublish(toExchange, routingKey, buffer))
        .onSuccess(
            successHandler -> {
              handler.handle(Future.succeededFuture());
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.QUEUE_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              handler.handle(Future.failedFuture(response.toJson().toString()));
            });
    return this;
  }
}
