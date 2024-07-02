package com.chinhbean.bookinghotel.services.payment;

import com.chinhbean.bookinghotel.configurations.VNPAYConfig;
import com.chinhbean.bookinghotel.dtos.PaymentDTO;
import com.chinhbean.bookinghotel.entities.Booking;
import com.chinhbean.bookinghotel.entities.PaymentTransaction;
import com.chinhbean.bookinghotel.enums.BookingStatus;
import com.chinhbean.bookinghotel.repositories.IBookingRepository;
import com.chinhbean.bookinghotel.repositories.PaymentTransactionRepository;
import com.chinhbean.bookinghotel.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final VNPAYConfig vnPayConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final IBookingRepository bookingRepository;

    @Transactional
    public PaymentDTO.VNPayResponse createVnPayPayment(HttpServletRequest request) {
        long amount = Integer.parseInt(request.getAttribute("amount").toString()) * 100L;
        String bankCode = (String) request.getAttribute("bankCode");
        Long bookingId = Long.parseLong(request.getAttribute("bookingId").toString());
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking with ID: " + bookingId + " does not exist."));

        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig(bookingId.toString());
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }
        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
        // build query url
        String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
        queryUrl += "&vnp_SecureHash=" + VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        // Lưu thông tin giao dịch vào bảng payment_transaction
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setBooking(booking);
        paymentTransaction.setPhoneGuest((String) request.getAttribute("phoneGuest"));
        paymentTransaction.setNameGuest((String) request.getAttribute("nameGuest"));
        paymentTransaction.setEmailGuest((String) request.getAttribute("emailGuest"));
        paymentTransaction.setCreateDate(LocalDateTime.now());
        paymentTransactionRepository.save(paymentTransaction);

        return PaymentDTO.VNPayResponse.builder()
                .code("ok")
                .message("success")
                .paymentUrl(paymentUrl).build();
    }

    @Transactional
    public void updatePaymentTransactionStatus(String bookingId, boolean isSuccess) {
        Long id = Long.parseLong(bookingId);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking with ID: " + bookingId + " does not exist."));

        if (isSuccess) {
            booking.setStatus(BookingStatus.PAID);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
        }
        bookingRepository.save(booking);
    }
}
