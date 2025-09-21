package com.booking.booking.controller;

import com.booking.booking.constant.MomoParameter;
import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/momo")
    @PreAuthorize("isAuthenticated()")
    public ResponseSuccess createQRMomo(@RequestParam Long bookingId) {
        return new ResponseSuccess(HttpStatus.OK, "Create Momo QR successfully!",
                paymentService.createQR(bookingId));
    }

    @GetMapping("/vnpay")
    @PreAuthorize("isAuthenticated()")
    public ResponseSuccess pay(HttpServletRequest request) {
        return new ResponseSuccess(HttpStatus.OK, "Success", paymentService.createVnPayPayment(request));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<String> vnpayReturnHandler(HttpServletRequest request) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");

        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));

        try {
            paymentService.handleVnpayCallback(params);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý thanh toán cho đơn hàng " + txnRef);
        }

        if ("00".equals(responseCode)) {
            return ResponseEntity.ok("Thanh toán thành công cho đơn hàng " + txnRef);
        } else {
            return ResponseEntity.badRequest().body("Thanh toán thất bại cho đơn hàng " + txnRef);
        }
    }


    @PostMapping("/momo-ipn")
    public ResponseSuccess momoIpnHandler(@RequestParam Map<String, String> request) {
        try {
            paymentService.handleMomoCallback(request);

            Integer responseCode = Integer.valueOf(request.get(MomoParameter.RESULT_CODE));
            return new ResponseSuccess(HttpStatus.OK,
                    responseCode == 0 ? "Payment successful" : "Payment failed");
        } catch (Exception e) {
            return new ResponseSuccess(HttpStatus.INTERNAL_SERVER_ERROR, "Payment processing error", null);
        }
    }
}
