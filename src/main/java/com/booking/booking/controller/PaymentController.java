package com.booking.booking.controller;

import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.dto.response.CreateMomoResponse;
import com.booking.booking.dto.PaymentDTO;
import com.booking.booking.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-CONTROLLER")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    private Environment env;

    @GetMapping("/momo")
    @PreAuthorize("isAuthenticated()")
    public ResponseSuccess createQRMomo(@RequestParam Long bookingId) {
        return new ResponseSuccess(HttpStatus.OK, "Create Momo QR successfully!",
                paymentService.createQR(bookingId));
    }

    @GetMapping("/vnpay")
    @PreAuthorize("isAuthenticated()")
    public ResponseSuccess pay(@RequestParam String bookingCode,
                               @RequestParam(required = false) String bankCode,
                               HttpServletRequest request) {
        return new ResponseSuccess(HttpStatus.OK, "Success", paymentService.createVnPayPayment(bookingCode, bankCode, request));
    }

    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseSuccess processPayment(
            @RequestParam Long bookingId,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String bankCode,
            HttpServletRequest request) {
        
        try {
            Map<String, Object> response = new HashMap<>();
            
            switch (paymentMethod.toUpperCase()) {
                case "MOMO":
                    CreateMomoResponse momoResult = paymentService.createQR(bookingId);
                    response.put("paymentMethod", "MOMO");
                    response.put("paymentUrl", momoResult.getPayUrl());
                    response.put("qrCode", momoResult.getQrCodeUrl());
                    response.put("orderId", momoResult.getOrderId());
                    response.put("resultCode", momoResult.getResultCode());
                    response.put("message", momoResult.getMessage());
                    break;
                    
                case "VNPAY":
                    PaymentDTO.VNPayResponse vnpayResult = paymentService.createVnPayPayment(bookingId.toString(), bankCode, request);
                    response.put("paymentMethod", "VNPAY");
                    response.put("paymentUrl", vnpayResult.paymentUrl);
                    response.put("code", vnpayResult.code);
                    response.put("message", vnpayResult.message);
                    response.put("orderId", bookingId.toString());
                    break;
                    
                case "CASH":
                    // Thanh toán tiền mặt - không cần redirect
                    response.put("paymentMethod", "CASH");
                    response.put("message", "Booking confirmed. Payment will be made at the hotel.");
                    response.put("bookingId", bookingId);
                    break;
                    
                default:
                    return new ResponseSuccess(HttpStatus.BAD_REQUEST, "Invalid PaymentMethod");
            }
            
            response.put("bookingId", bookingId);
            response.put("success", true);
            
            return new ResponseSuccess(HttpStatus.OK, "Get momo qr success", response);
            
        } catch (Exception e) {
            log.error("Error processing payment for booking {}: {}", bookingId, e.getMessage(), e);
            return new ResponseSuccess(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    @PostMapping("/retry")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> retryPayment(
            @RequestParam Long bookingId,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String bankCode,
            HttpServletRequest request) {
        
        log.info("Retry payment for booking {} with method {}", bookingId, paymentMethod);
        
        return processPayment(bookingId, paymentMethod, bankCode, request);
    }


    @GetMapping("/vnpay-return")
    public RedirectView vnpayReturnHandler(HttpServletRequest request) {

        String baseUrl = env.getProperty("BASE_URL_FE", "http://localhost:5173");

        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");

        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));

        try {
            paymentService.handleVnpayCallback(params);
        } catch (Exception e) {
            return new RedirectView(baseUrl + "/payment/result?status=failure&orderId=" + txnRef);
        }

        if ("00".equals(responseCode)) {
            return new RedirectView(baseUrl + "/payment/result?status=success&orderId=" + txnRef);
        } else {
            return new RedirectView(baseUrl + "/payment/result?status=failure&orderId=" + txnRef);
        }
    }


    @PostMapping("/momo-ipn")
    public ResponseEntity<Map<String, Object>> momoIpnHandler(@RequestParam Map<String, String> request) {
        try {
            paymentService.handleMomoCallback(request);
            return ResponseEntity.ok(Map.of("resultCode", 0, "message", "Confirm Success"));
        } catch (Exception e) {

            return ResponseEntity.ok(Map.of("resultCode", 1, "message", "Confirm Failed"));
        }
    }

    @GetMapping("/momo-return")
    public RedirectView momoReturnHandler(HttpServletRequest request) {
        String baseUrl = env.getProperty("BASE_URL_FE", "http://localhost:5173");

        String orderId = request.getParameter("orderId");
        String resultCode = request.getParameter("resultCode");

        if ("0".equals(resultCode)) {
            paymentService.handleMomoCallback(orderId);
            return new RedirectView(baseUrl + "/payment/result?status=success&orderId=" + orderId);
        } else {
            return new RedirectView(baseUrl + "/payment/result?status=failure&orderId=" + orderId);
        }
    }

}
