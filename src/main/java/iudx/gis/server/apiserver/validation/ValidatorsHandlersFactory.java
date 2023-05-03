package iudx.gis.server.apiserver.validation;

import static iudx.gis.server.apiserver.util.Constants.NGSILDQUERY_ID;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import iudx.gis.server.apiserver.util.RequestType;
import iudx.gis.server.apiserver.validation.types.IdTypeValidator;
import iudx.gis.server.apiserver.validation.types.JsonSchemaTypeValidator;
import iudx.gis.server.apiserver.validation.types.StringTypeValidator;
import iudx.gis.server.apiserver.validation.types.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidatorsHandlersFactory {

  private static final Logger LOGGER = LogManager.getLogger(ValidatorsHandlersFactory.class);
  private static Map<String, String> jsonSchemaMap = new HashMap<>();

  public List<Validator> build(
      final Vertx vertx,
      RequestType type,
      final MultiMap parameters,
      final MultiMap headers,
      final JsonObject body) {
    List<Validator> validator = new ArrayList<>();
    LOGGER.debug("getValidation4Context() started for :" + type);
    switch (type) {
      case ENTITY_PATH:
        getEntityPathValidations(parameters, validator);
        break;
      case ENTITY_QUERY:
        getEntityQueryValidations(parameters, validator);
        break;
      case ADMIN_CRUD_PATH:
        getAdminCrudPathValidations(vertx, body, validator);
        break;
      case ADMIN_CRUD_PATH_DELETE:
        getAdminCrudPathDeleteValidations(parameters, validator);
        break;
      default:
        break;
    }
    return validator;
  }

  private void getAdminCrudPathDeleteValidations(MultiMap parameters, List<Validator> validator) {
    validator.add(new IdTypeValidator(parameters.get("id"), true));
  }

  private void getAdminCrudPathValidations(
      Vertx vertx, JsonObject body, List<Validator> validator) {
    validator.addAll(getRequestSchemaValidator(vertx, body, RequestType.ADMIN_CRUD_PATH));
  }

  private void getEntityQueryValidations(MultiMap parameters, List<Validator> validator) {
    validator.add(new IdTypeValidator(parameters.get(NGSILDQUERY_ID), true));
  }

  private void getEntityPathValidations(MultiMap parameters, List<Validator> validator) {
    validator.add(new StringTypeValidator(parameters.get("domain"), true));
    validator.add(new StringTypeValidator(parameters.get("userSha"), true));
    validator.add(new StringTypeValidator(parameters.get("resourceServer"), true));
    validator.add(new StringTypeValidator(parameters.get("resourceGroup"), true));
    validator.add(new StringTypeValidator(parameters.get("resourceName"), true));
  }

  private List<Validator> getRequestSchemaValidator(
      Vertx vertx, JsonObject body, RequestType requestType) {
    List<Validator> validators = new ArrayList<>();
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);
    String jsonSchema = null;

    try {
      jsonSchema = loadJson(requestType.getFilename());
      Schema schema = schemaParser.parse(new JsonObject(jsonSchema));
      validators.add(new JsonSchemaTypeValidator(body, schema));
    } catch (Exception ex) {
      LOGGER.error(ex);
      //      throw new DxRuntimeException(HttpStatusCode.BAD_REQUEST.getValue(),
      // SCHEMA_READ_ERROR);
    }
    return validators;
  }

  private String loadJson(String filename) {
    String jsonStr = null;
    if (jsonSchemaMap.containsKey(filename)) {
      jsonStr = jsonSchemaMap.get(filename);
    } else {
      try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename)) {
        jsonStr = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
        jsonSchemaMap.put(filename, jsonStr);
      } catch (IOException e) {
        LOGGER.error(e);
        //        throw new DxRuntimeException(HttpStatusCode.BAD_REQUEST.getValue(),
        // SCHEMA_READ_ERROR);
      }
    }
    return jsonStr;
  }
}
