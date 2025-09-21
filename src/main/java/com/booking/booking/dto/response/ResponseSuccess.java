package com.booking.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

public class ResponseSuccess extends ResponseEntity {

  public ResponseSuccess(HttpStatus status, String message) {
    super(new Payload(status.value(), message), HttpStatus.OK);
  }

  public ResponseSuccess(HttpStatus status, String message, Object data) {
    super(new Payload(status.value(), message, data), status);
  }

  public ResponseSuccess(Payload body, HttpStatus status) {
    super(body, status);
  }

  public ResponseSuccess(MultiValueMap<String, String> headers, HttpStatus status) {
    super(headers, status);
  }

  public ResponseSuccess(Payload payload, MultiValueMap<String, String> headers, int rawStatus) {
    super(payload, headers, rawStatus);
  }

  public ResponseSuccess(Payload payload, MultiValueMap<String, String> headers,
      HttpStatus status) {
    super(payload, headers, status);
  }

  public static class Payload {

    private final int status;
    private final String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;

    public Payload(int status, String message) {
      this.status = status;
      this.message = message;
      this.time = LocalDateTime.now();
    }

    public Payload(int status, String message, Object data) {
      this.status = status;
      this.message = message;
      this.data = data;
      this.time = LocalDateTime.now();
    }

    public int getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public Object getData() {
      return data;
    }

    public LocalDateTime getTime() {
      return time;
    }
  }
}
