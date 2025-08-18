package com.booking.booking.service;

import com.booking.booking.client.MomoApi;
import com.booking.booking.common.BookingStatus;
import com.booking.booking.config.VnpayConfig;
import com.booking.booking.controller.request.CreateMomoRequest;
import com.booking.booking.controller.response.CreateMomoResponse;
import com.booking.booking.dto.PaymentDTO;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Booking;
import com.booking.booking.repository.BookingRepository;
import com.booking.booking.util.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    String orderId = booking.getBookingCode();
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

  public PaymentDTO.VNPayResponse createVnPayPayment(HttpServletRequest request) {
    long bookingId = Long.parseLong(request.getParameter("bookingId"));

    Booking booking = bookingRepository.findById(bookingId).orElseThrow(
        () -> new ResourceNotFoundException("Booking with id: " + bookingId + " not found"));

    BigDecimal price = booking.getTotalPrice();
    long amount = price.multiply(BigDecimal.valueOf(100)).longValue();

    String bankCode = request.getParameter("bankCode");
    Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();
    vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
    if (bankCode != null && !bankCode.isEmpty()) {
      vnpParamsMap.put("vnp_BankCode", bankCode);
    }
    vnpParamsMap.put("vnp_TxnRef", booking.getBookingCode());
    vnpParamsMap.put("vnp_OrderInfo", "Thanh toán đơn hàng:" + booking.getBookingCode());
    vnpParamsMap.put("vnp_IpAddr", VnpayUtil.getIpAddress(request));
    String queryUrl = VnpayUtil.getPaymentURL(vnpParamsMap, true);
    String hashData = VnpayUtil.getPaymentURL(vnpParamsMap, false);
    String vnpSecureHash = VnpayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
    queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
    String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
    return PaymentDTO.VNPayResponse.builder()
        .code("ok")
        .message("success")
        .paymentUrl(paymentUrl).build();
  }

  @Transactional
  public void handleMomoCallback(Map<String, String> params) {
    String orderId = params.get("orderId");
    String resultCode = params.get("resultCode");

    log.info("Received MoMo callback for order: {} with result code: {}", orderId, resultCode);

    Booking booking = bookingRepository.findByBookingCode(orderId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Booking not found with code: " + orderId));

    if ("0".equals(resultCode)) {
      booking.setStatus(BookingStatus.CONFIRMED);
      log.info("Payment successful for booking: {}", orderId);

      try {
        emailService.sendBookingConfirmation(booking.getGuest().getEmail(), booking);
      } catch (Exception e) {
        log.error("Failed to send confirmation email for booking: {}", orderId, e);
      }
    } else {
      booking.setStatus(BookingStatus.EXPIRED);
      log.warn("Payment failed for booking: {} with result code: {}", orderId, resultCode);
    }

    bookingRepository.save(booking);
  }

  @Transactional
  public void handleVnpayCallback(Map<String, String> params) {
    String orderInfo = params.get("vnp_OrderInfo");
    String responseCode = params.get("vnp_ResponseCode");

    String bookingCode = extractBookingCodeFromOrderInfo(orderInfo);

    log.info("Received VNPay callback for booking: {} with response code: {}", bookingCode,
        responseCode);

    Booking booking = bookingRepository.findByBookingCode(bookingCode)
        .orElseThrow(
            () -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));

    if ("00".equals(responseCode)) {
      booking.setStatus(BookingStatus.CONFIRMED);
      log.info("Payment successful for booking: {}", bookingCode);

      try {
        emailService.sendBookingConfirmation(booking.getGuest().getEmail(), booking);
      } catch (Exception e) {
        log.error("Failed to send confirmation email for booking: {}", bookingCode, e);
      }
    } else {
      booking.setStatus(BookingStatus.EXPIRED);
      log.warn("Payment failed for booking: {} with response code: {}", bookingCode, responseCode);
    }

    bookingRepository.save(booking);
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

  private String extractBookingCodeFromOrderInfo(String orderInfo) {
    if (orderInfo != null && orderInfo.contains("BK-")) {
      int startIndex = orderInfo.indexOf("BK-");
      int endIndex = startIndex + 11;
      if (endIndex <= orderInfo.length()) {
        return orderInfo.substring(startIndex, endIndex);
      }
    }
    throw new IllegalArgumentException("Invalid order info format: " + orderInfo);
  }
}
