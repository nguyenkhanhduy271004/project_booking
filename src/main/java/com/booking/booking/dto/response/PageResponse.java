package com.booking.booking.dto.response;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageResponse<T> implements Serializable {

  private int pageNo;
  private int pageSize;
  private long totalPage;
  private long totalElements;
  private T items;
}
