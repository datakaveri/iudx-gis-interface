package iudx.gis.server.apiserver.validation.types;

import iudx.gis.server.apiserver.exceptions.DxRuntimeException;
import iudx.gis.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

import static iudx.gis.server.apiserver.util.Constants.*;

public class IdTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(IdTypeValidator.class);

  private Integer maxLength = VALIDATION_ID_MAX_LEN;
  private static final Pattern regexIDPattern = Pattern.compile(
      "^[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z.]{4,100}/{1}[a-zA-Z-_.]{4,100}/{1}[a-zA-Z0-9-_.]{4,100}$");

  private String value;
  private boolean required;

  public IdTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isvalidIUDXId(String value) {
    return regexIDPattern.matcher(value).matches();
  }


  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    String errorMessage = "";
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
    if (value!=null && value.length() > maxLength) {
      errorMessage = "Validation error : Value exceed max character limit";
    }
    if (!isvalidIUDXId(value)) {
      errorMessage = "Validation error : Invalid id";
    }
    if (errorMessage.isBlank()) {
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
    return "Invalid id.";
  }
}
