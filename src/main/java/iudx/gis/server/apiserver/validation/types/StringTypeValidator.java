package iudx.gis.server.apiserver.validation.types;

import iudx.gis.server.apiserver.exceptions.DxRuntimeException;
import iudx.gis.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StringTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(StringTypeValidator.class);

  private String value;
  private boolean required;

  public StringTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    String errorMessage = "";
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      errorMessage = "Validation error : null or blank value for required mandatory field";
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        errorMessage = "Validation error :  blank value for passed";
      }
    }
    if (value!=null && value.length() > 100) {
      errorMessage = "Validation error : length >100 not allowed";
    }
    if (errorMessage.isEmpty()) {
      return true;
    }
    throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_PAYLOAD_FORMAT, errorMessage);
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid string";
  }
}
