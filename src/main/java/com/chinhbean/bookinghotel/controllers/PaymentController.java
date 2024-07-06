package com.chinhbean.bookinghotel.controllers;

import com.chinhbean.bookinghotel.dtos.PaymentDTO;
import com.chinhbean.bookinghotel.entities.Booking;
import com.chinhbean.bookinghotel.entities.User;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import com.chinhbean.bookinghotel.repositories.IUserRepository;
import com.chinhbean.bookinghotel.responses.payment.PaymentResponse;
import com.chinhbean.bookinghotel.services.booking.IBookingService;
import com.chinhbean.bookinghotel.services.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final IUserRepository userRepository;
    private final IBookingService bookingService;

    @GetMapping("/vn-pay")
    public PaymentResponse<PaymentDTO.VNPayResponse> pay(
            @RequestParam(required = false) String bookingId,
            @RequestParam(required = false) String packageId,
            @RequestParam String amount,
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) String phoneGuest,
            @RequestParam(required = false) String nameGuest,
            @RequestParam(required = false) String emailGuest,
            HttpServletRequest request) {

        request.setAttribute("amount", amount);
        request.setAttribute("bankCode", bankCode);
        request.setAttribute("phoneGuest", phoneGuest);
        request.setAttribute("nameGuest", nameGuest);
        request.setAttribute("emailGuest", emailGuest);

        Optional<User> user = userRepository.findByEmail(emailGuest);

        if (bookingId != null) {
            request.setAttribute("bookingId", bookingId);
            return new PaymentResponse<>(HttpStatus.OK, "Success", paymentService.createVnPayPaymentForBooking(request));
        } else if (packageId != null) {
            request.setAttribute("packageId", packageId);
            if (user.isPresent()) {
                request.setAttribute("userEmail", user.get().getEmail());
            }
            return new PaymentResponse<>(HttpStatus.OK, "Success", paymentService.createVnPayPaymentForPackage(request));
        } else {
            throw new IllegalArgumentException("Either bookingId or packageId must be provided");
        }
    }

    @GetMapping("/vn-pay-callback")
    public void payCallbackHandler(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String status = request.getParameter("vnp_ResponseCode");
        String paymentId = request.getParameter("vnp_TxnRef");
        String bookingId = null;
        String packageId = null;
        String orderInfo = request.getParameter("vnp_OrderInfo");
        String email = null;
        if (orderInfo != null) {
            Pattern pattern = Pattern.compile("dich vu: (\\d+) cho user ([^\\s]+)");
            Matcher matcher = pattern.matcher(orderInfo);
            if (matcher.find()) {
                packageId = matcher.group(1);
                email = matcher.group(2);
            }
        }

        if (orderInfo != null && orderInfo.contains("don hang:")) {
            bookingId = orderInfo.substring(orderInfo.indexOf("don hang:") + 9).trim();
        }


        if ("00".equals(status)) {
            // Successful payment
            if (bookingId != null) {
                paymentService.updatePaymentTransactionStatusForBooking(bookingId, true);
                try {
                    Booking booking = bookingService.getBookingById(Long.parseLong(bookingId));
                    bookingService.sendMailNotificationForBookingPayment(booking);
                    response.sendRedirect("http://localhost:3000/payment-return/success");
                } catch (DataNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else if (packageId != null) {
                paymentService.updatePaymentTransactionStatusForPackage(packageId, email, true);
                response.sendRedirect("http://localhost:3000/payment-return/success");
            } else {
                // Handle unexpected case where neither bookingId nor packageId is present
                response.sendRedirect("http://localhost:3000/payment-return/failed");
            }
        } else {
            // Payment failed
            if (bookingId != null) {
                paymentService.updatePaymentTransactionStatusForBooking(bookingId, false);
            } else if (packageId != null) {
                paymentService.updatePaymentTransactionStatusForPackage(packageId, email, false);
            }
            response.sendRedirect("http://localhost:3000/payment-return/failed");
        }
    }

}
