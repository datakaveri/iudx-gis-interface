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

    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE,
            failureMessage());
      }
    }
    if (value != null && value.length() > 100) {
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, failureMessage());
    }
    return true;
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
