package com.booking.booking.client;

import com.booking.booking.dto.request.CreateMomoRequest;
import com.booking.booking.dto.response.CreateMomoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "momo", url = "${momo.end-point}")
public interface MomoApi {

  @PostMapping("/create")
  CreateMomoResponse createMomoQR(@RequestBody CreateMomoRequest request);

}
