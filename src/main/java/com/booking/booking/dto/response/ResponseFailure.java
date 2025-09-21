package com.booking.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseFailure extends ResponseEntity<ResponseFailure.FailurePayload> {

  public ResponseFailure(HttpStatus status, String message) {
    super(new FailurePayload(status.value(), message), status);
  }

  public ResponseFailure(HttpStatus status, String message, Object errors) {
    super(new FailurePayload(status.value(), message, errors), status);
  }

  public static class FailurePayload {
    private final int status;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("errors")
    private final Object errors;

    public FailurePayload(int status, String message) {
      this.status = status;
      this.message = message;
      this.errors = null;
    }

    public FailurePayload(int status, String message, Object errors) {
      this.status = status;
      this.message = message;
      this.errors = errors;
    }

    public int getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public Object getErrors() {
      return errors;
    }
  }
}
