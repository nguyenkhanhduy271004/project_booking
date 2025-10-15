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
import com.booking.booking.util.BookingUtil;
import com.booking.booking.util.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-SERVICE")
public class PaymentService {

    @Value("${momo.partner-code}")
    private String partnerCode;
    @Value("${momo.access-key}")
    private String accessKey;
    @Value("${momo.secret-key}")
    private String secretKey;
    @Value("${momo.return-url}")
    private String redirectUrl;
    @Value("${momo.ipn-url}")
    private String ipnUrl;
    @Value("${momo.request-type}")
    private String requestType;

    private final MomoApi momoApi;
    private final BookingUtil bookingUtil;
    private final VnpayConfig vnPayConfig;
    private final EmailService emailService;
    private final BookingRepository bookingRepository;

    public CreateMomoResponse createQR(long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);
        booking.setStatus(BookingStatus.PAYING);
        booking.setPaymentExpiredAt(Instant.now().plus(Duration.ofMinutes(15)));

        bookingUtil.handleBookingWithStatus(booking, BookingStatus.PAYING);
        bookingRepository.save(booking);

        BigDecimal price = booking.getTotalPrice();

        String orderId = booking.getBookingCode()
                .replaceAll("[^a-zA-Z0-9_.:-]", "");

        String requestId = UUID.randomUUID().toString();
        String orderInfo = "Thanh toán hóa đơn: " + orderId;
        String extraData = Base64.getEncoder().encodeToString("Không có khuyến mãi".getBytes(StandardCharsets.UTF_8));

        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey, price.longValue(), extraData, ipnUrl, orderId, orderInfo, partnerCode, redirectUrl,
                requestId, requestType
        );

        String signature = hmacSHA256(secretKey, rawSignature);

        CreateMomoRequest request = CreateMomoRequest.builder()
                .partnerCode(partnerCode)
                .requestType(requestType)
                .ipnUrl(ipnUrl)
                .redirectUrl(redirectUrl)
                .orderId(orderId)
                .orderInfo(orderInfo)
                .requestId(requestId)
                .extraData(extraData)
                .amount(price.longValue())
                .signature(signature)
                .lang("vi")
                .build();

        CreateMomoResponse response = momoApi.createMomoQR(request);
        log.info("Momo createQR response: {}", response);

        return response;
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

        String sig = hmacSHA256(secretKey, raw);
        if (!sig.equals(params.get("signature"))) {
            throw new BadRequestException("Invalid MoMo signature");
        }

        String orderId = extractShortBookingCode(params.get("orderId"));
        Booking booking = findBookingByCodeOrThrow(orderId);

        BigDecimal paid = new BigDecimal(params.get("amount"));
        if (paid.compareTo(booking.getTotalPrice()) != 0) {
            throw new BadRequestException("MoMo amount mismatch");
        }

        boolean success = "0".equals(params.get("resultCode"));
        confirmOrCancelBooking(booking, success);
    }

    public void handleMomoCallback(String orderId) {
        String shortCode = extractShortBookingCode(orderId);
        log.info("Booking code: {}", shortCode);
        Booking booking = bookingRepository.findByBookingCode(shortCode).orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + shortCode));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingUtil.handleBookingWithStatus(booking, BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    public PaymentDTO.VNPayResponse createVnPayPayment(String bookingCode, String bankCode, HttpServletRequest req) {
        Booking booking = findBookingByCodeOrThrow(bookingCode);
        booking.setStatus(BookingStatus.PAYING);
        booking.setPaymentExpiredAt(Instant.now().plus(Duration.ofMinutes(15)));

        bookingUtil.handleBookingWithStatus(booking, BookingStatus.PAYING);
        bookingRepository.save(booking);

        BigDecimal price = booking.getTotalPrice().multiply(BigDecimal.valueOf(100));

        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();
        vnpParamsMap.put("vnp_Amount", price.toBigInteger().toString());
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

        String bookingCode = params.get("vnp_TxnRef");
        Booking booking = findBookingByCodeOrThrow(bookingCode);

        BigDecimal expected = booking.getTotalPrice().multiply(BigDecimal.valueOf(100));
        BigDecimal actual = new BigDecimal(params.get("vnp_Amount"));
        if (actual.compareTo(expected) != 0) {
            throw new BadRequestException("VNPay amount mismatch");
        }

        boolean success = "00".equals(params.get("vnp_ResponseCode"));
        confirmOrCancelBooking(booking, success);
    }

    private void confirmOrCancelBooking(Booking booking, boolean success) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) return;

        if (success) {
            booking.setStatus(BookingStatus.CONFIRMED);
//            emailService.sendPaymentSuccessEmail(booking);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingUtil.handleBookingWithStatus(booking, BookingStatus.CANCELLED);
        }
        bookingRepository.save(booking);
    }

    private Booking findBookingOrThrow(long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with id: " + bookingId + " not found"));
    }

    private Booking findBookingByCodeOrThrow(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));
    }

    private String extractShortBookingCode(String orderId) {
        if (orderId != null && orderId.contains("-")) {
            return orderId.substring(0, orderId.lastIndexOf("-"));
        }
        return orderId;
    }

    private String hmacSHA256(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while generating HMAC SHA256 signature", e);
        }
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void checkExpiredPayments() {
        List<Booking> bookings = bookingRepository
                .findByStatusAndPaymentExpiredAtBefore(BookingStatus.PAYING, Instant.now());

        if (bookings.isEmpty()) {
            return;
        }

        bookings.forEach(booking -> {
            booking.setStatus(BookingStatus.PENDING);
            log.info("Booking ID {} has expired payment. Resetting status to PENDING", booking.getId());
        });

        bookingRepository.saveAll(bookings);
    }

}
