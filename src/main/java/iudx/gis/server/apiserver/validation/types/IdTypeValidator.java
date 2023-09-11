package iudx.gis.server.apiserver.validation.types;

import static iudx.gis.server.apiserver.util.Constants.*;

import iudx.gis.server.apiserver.exceptions.DxRuntimeException;
import iudx.gis.server.apiserver.response.ResponseUrn;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IdTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(IdTypeValidator.class);
  private static final Pattern regexIDPattern =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
  private Integer maxLength = VALIDATION_ID_MAX_LEN;
  private String value;
  private boolean required;

  public IdTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isValidIudxId(String value) {
    return regexIDPattern.matcher(value).matches();
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    String errorMessage = "";
    if (required && (value == null || value.isBlank())) {
      errorMessage = "Validation error : null or blank value for required mandatory field";
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, failureMessage());

    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        errorMessage = "Validation error :  blank value for passed";
        throw new DxRuntimeException(
            failureCode(), ResponseUrn.INVALID_ATTR_VALUE, failureMessage(value));
      }
    }
    if (value != null && value.length() > maxLength) {
      errorMessage = "Validation error : Value exceed max character limit";
      throw new DxRuntimeException(
          failureCode(), ResponseUrn.INVALID_ATTR_VALUE, failureMessage(value));
    }
    if (!isValidIudxId(value)) {
      errorMessage = "Validation error : Invalid id";
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, errorMessage);
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid id.";
  }
}
