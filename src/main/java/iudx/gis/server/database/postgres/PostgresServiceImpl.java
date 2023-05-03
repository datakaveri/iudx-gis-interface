package iudx.gis.server.database.postgres;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import iudx.gis.server.common.Response;
import iudx.gis.server.common.ResponseUrn;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PostgresServiceImpl implements PostgresService {

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  private final PgPool client;


  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public PostgresService executeQuery(final String query,
      Handler<AsyncResult<JsonObject>> handler) {

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(connection -> connection.query(query)
            .collecting(rowCollector)
            .execute()
            .map(row -> row.value()))
        .onSuccess(successHandler -> {
          JsonArray result = new JsonArray(successHandler);
          JsonObject responseJson = new JsonObject()
              .put("type", ResponseUrn.SUCCESS_URN.getUrn())
              .put("title", ResponseUrn.SUCCESS_URN.getMessage())
              .put("result", result);
          handler.handle(Future.succeededFuture(responseJson));
        })
        .onFailure(failureHandler -> {
          LOGGER.debug(failureHandler);
          Response response = new Response.Builder()
              .withUrn(ResponseUrn.DB_ERROR_URN.getUrn())
              .withStatus(HttpStatus.SC_BAD_REQUEST)
              .withDetail(failureHandler.getLocalizedMessage()).build();
          handler.handle(Future.failedFuture(response.toString()));
        });
    return this;
  }

}
