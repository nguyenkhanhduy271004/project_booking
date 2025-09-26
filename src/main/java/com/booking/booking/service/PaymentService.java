package com.booking.booking.service;

import com.booking.booking.client.MomoApi;
import com.booking.booking.common.BookingStatus;
import com.booking.booking.config.VnpayConfig;
import com.booking.booking.dto.PaymentDTO;
import com.booking.booking.dto.request.CreateMomoRequest;
import com.booking.booking.dto.response.CreateMomoResponse;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Booking;
import com.booking.booking.repository.BookingRepository;
import com.booking.booking.util.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-SERVICE")
public class PaymentService {

    @Value("${momo.partner-code}")
    private String PARTNER_CODE;
    @Value("${momo.access-key}")
    private String ACCESS_KEY;
    @Value("${momo.secret-key}")
    private String SECRET_KEY;
    @Value("${momo.return-url}")
    private String REDIRECT_URL;
    @Value("${momo.ipn-url}")
    private String IPN_URL;
    @Value("${momo.request-type}")
    private String REQUEST_TYPE;

    private final MomoApi momoApi;
    private final VnpayConfig vnPayConfig;
    private final EmailService emailService;
    private final BookingRepository bookingRepository;

    public CreateMomoResponse createQR(long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking with id: " + bookingId + " not found"));
        BigDecimal price = booking.getTotalPrice();
        long amount = price.longValue();
        String orderId = booking.getBookingCode() + "-" + UUID.randomUUID().toString().substring(0, 6);
        String requestId = UUID.randomUUID().toString();
        String orderInfo = "Thanh toán hóa đơn: " + orderId;
        String extraData = Base64.getEncoder()
                .encodeToString("Không có khuyến mãi".getBytes(StandardCharsets.UTF_8));

        String rawSignature = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                ACCESS_KEY, amount, extraData, IPN_URL, orderId, orderInfo, PARTNER_CODE, REDIRECT_URL,
                requestId, REQUEST_TYPE);

        String signature = hmacSHA256(SECRET_KEY, rawSignature);

        CreateMomoRequest request = CreateMomoRequest.builder()
                .partnerCode(PARTNER_CODE)
                .requestType(REQUEST_TYPE)
                .ipnUrl(IPN_URL)
                .redirectUrl(REDIRECT_URL)
                .orderId(orderId)
                .orderInfo(orderInfo)
                .requestId(requestId)
                .extraData(extraData)
                .amount(amount)
                .signature(signature)
                .lang("vi")
                .build();

        CreateMomoResponse response = momoApi.createMomoQR(request);
        log.info("Momo createQR response: {}", response);

        return response;
    }

    public PaymentDTO.VNPayResponse createVnPayPayment(String bookingId, String bankCode, HttpServletRequest req) {
        Booking booking = bookingRepository.findByBookingCode(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with id: " + bookingId + " not found"));

        BigDecimal price = booking.getTotalPrice();
        long amount = price.multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        vnpParamsMap.put("vnp_TxnRef", booking.getBookingCode());
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toán đơn hàng:" + booking.getBookingCode());
        vnpParamsMap.put("vnp_IpAddr", VnpayUtil.getIpAddress(req));

        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }

        String queryUrl = VnpayUtil.getPaymentURL(vnpParamsMap, true);
        String hashData = VnpayUtil.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VnpayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        return PaymentDTO.VNPayResponse.builder()
                .code("ok")
                .message("success")
                .paymentUrl(paymentUrl)
                .build();
    }


    @Transactional
    public void handleMomoCallback(Map<String, String> params) {
        String raw = String.format(
                "amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&"
                        + "partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                params.get("amount"),
                params.get("extraData"),
                params.get("message"),
                params.get("orderId"),
                params.get("orderInfo"),
                params.get("orderType"),
                params.get("partnerCode"),
                params.get("payType"),
                params.get("requestId"),
                params.get("responseTime"),
                params.get("resultCode"),
                params.get("transId")
        );
        String sig = hmacSHA256(SECRET_KEY, raw);
        if (!sig.equals(params.get("signature"))) {
            throw new BadRequestException("Invalid MoMo signature");
        }

        String orderId = params.get("orderId");
        Booking booking = bookingRepository.findByBookingCode(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + orderId));

        long paid = Long.parseLong(params.get("amount"));
        if (paid != booking.getTotalPrice().longValue()) {
            throw new BadRequestException("MoMo amount mismatch");
        }

        // Idempotent:
        if (booking.getStatus() == BookingStatus.CONFIRMED) return;

        if ("0".equals(params.get("resultCode"))) {
            booking.setStatus(BookingStatus.CONFIRMED);
            // send email ...
        } else {
            booking.setStatus(BookingStatus.EXPIRED);
        }
        bookingRepository.save(booking);
    }


    @Transactional
    public void handleVnpayCallback(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null) throw new BadRequestException("Missing vnp_SecureHash");

        Map<String, String> signed = new HashMap<>(params);
        signed.remove("vnp_SecureHash");
        signed.remove("vnp_SecureHashType");

        String hashData = VnpayUtil.getPaymentURL(signed, false);
        String calc = VnpayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        if (!calc.equalsIgnoreCase(secureHash)) {
            throw new BadRequestException("Invalid VNPay signature");
        }

        String responseCode = params.get("vnp_ResponseCode");

        String bookingCode = params.get("vnp_TxnRef");

        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));

        long expected = booking.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();
        long actual = Long.parseLong(params.get("vnp_Amount"));
        if (actual != expected) throw new BadRequestException("VNPay amount mismatch");

        // Idempotent:
        if (booking.getStatus() == BookingStatus.CONFIRMED) return;

        if ("00".equals(responseCode)) {
            booking.setStatus(BookingStatus.CONFIRMED);
            // send email ...
        } else {
            booking.setStatus(BookingStatus.EXPIRED);
        }
        bookingRepository.save(booking);
    }

    public void handleMomoCallback(String orderId) {
        String shortCode = extractShortBookingCode(orderId);
        log.info("Booking code: {}", shortCode);

        Booking booking = bookingRepository.findByBookingCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + shortCode));

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    private String extractShortBookingCode(String orderId) {
        if (orderId != null && orderId.contains("-")) {
            int lastDashIndex = orderId.lastIndexOf("-");
            return orderId.substring(0, lastDashIndex);
        }
        return orderId;
    }



    private String hmacSHA256(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while generating HMAC SHA256 signature", e);
        }
    }
}
