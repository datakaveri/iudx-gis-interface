package iudx.gis.server.apiserver.handlers;

import static iudx.gis.server.apiserver.util.Constants.*;

import iudx.gis.server.apiserver.exceptions.DxRuntimeException;
import iudx.gis.server.apiserver.response.RestResponse;
import iudx.gis.server.apiserver.util.HttpStatusCode;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ValidationFailureHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ValidationFailureHandler.class);

  @Override
  public void handle(RoutingContext context) {
    Throwable failure = context.failure();

    if (failure instanceof DxRuntimeException) {
      DxRuntimeException exception = (DxRuntimeException) failure;
      LOGGER.error(exception.getUrn().getUrn() + " : " + exception.getMessage());
      HttpStatusCode code = HttpStatusCode.getByValue(exception.getStatusCode());

      JsonObject response = new RestResponse.Builder()
          .withType(exception.getUrn().getUrn())
          .withTitle(code.getDescription())
          .withMessage(exception.getLocalizedMessage())
          .build()
          .toJson();

      context.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(exception.getStatusCode())
          .end(response.toString());
    }

    if (failure instanceof RuntimeException) {

      String validationErrorMessage = MSG_BAD_QUERY;
      context.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(validationFailureResponse(validationErrorMessage).toString());

    }
    context.next();
    return;
  }

  private JsonObject validationFailureResponse(String message) {
    return new JsonObject()
        .put(JSON_TYPE, HttpStatus.SC_BAD_REQUEST)
        .put(JSON_TITLE, "Bad Request")
        .put(JSON_DETAIL, message);
  }

}
