package iudx.gis.server.apiserver.validation.types;

import static iudx.gis.server.apiserver.response.ResponseUrn.*;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import iudx.gis.server.apiserver.exceptions.DxRuntimeException;
import iudx.gis.server.apiserver.util.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JsonSchemaTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(JsonSchemaTypeValidator.class);

  private final JsonObject value;
  private final Schema schema;

  public JsonSchemaTypeValidator(final JsonObject value, final Schema schema) {
    this.value = value;
    this.schema = schema;
  }

  @Override
  public boolean isValid() {
    try {
      schema.validateSync(value);
    } catch (Exception e) {
      LOGGER.error("Validation error :" + e.getMessage());
      throw new DxRuntimeException(failureCode(), INVALID_PAYLOAD_FORMAT, e.getLocalizedMessage());
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_PAYLOAD_FORMAT.getMessage();
  }

}
