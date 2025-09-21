package com.booking.booking.controller;

import com.booking.booking.dto.request.EvaluateRequest;
import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.service.EvaluateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluates")
@RequiredArgsConstructor
@Slf4j(topic = "EVALUATE-CONTROLLER")
public class EvaluateController {

    private final EvaluateService evaluateService;

    @PostMapping
    @PreAuthorize("hasAuthority('GUEST')")
    public ResponseSuccess createEvaluate(@RequestBody EvaluateRequest evaluateRequest) {

        log.info("Evaluate request: {}", evaluateRequest);

        evaluateService.createEvaluate(evaluateRequest);

        return new ResponseSuccess(HttpStatus.OK, "Evaluate created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GUEST')")
    public ResponseSuccess updateEvaluate(@PathVariable Long id,
                                          @RequestBody EvaluateRequest evaluateRequest) {
        log.info("Evaluate request: {}", evaluateRequest);

        evaluateService.updateEvaluate(id, evaluateRequest);

        return new ResponseSuccess(HttpStatus.OK, "Evaluate updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GUEST')")
    public ResponseSuccess deleteEvaluate(@PathVariable Long id) {

        evaluateService.deleteEvaluate(id);

        return new ResponseSuccess(HttpStatus.NO_CONTENT, "Evaluate deleted successfully");
    }

    @GetMapping("/room/{id}")
    public ResponseSuccess getEvaluateByRoomId(@PathVariable Long id) {

        return new ResponseSuccess(HttpStatus.OK, "Get evaluate by room id successfully",
                evaluateService.getEvaluatesByRoomId(id));
    }


}
