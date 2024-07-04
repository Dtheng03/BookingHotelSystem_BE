package com.chinhbean.bookinghotel.controllers;

import com.chinhbean.bookinghotel.dtos.PaymentDTO;
import com.chinhbean.bookinghotel.responses.payment.PaymentResponse;
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

@RestController
@RequestMapping("api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

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

        if (bookingId != null) {
            request.setAttribute("bookingId", bookingId);
            return new PaymentResponse<>(HttpStatus.OK, "Success", paymentService.createVnPayPaymentForBooking(request));
        } else if (packageId != null) {
            request.setAttribute("packageId", packageId);
            return new PaymentResponse<>(HttpStatus.OK, "Success", paymentService.createVnPayPaymentForPackage(request));
        } else {
            throw new IllegalArgumentException("Either bookingId or packageId must be provided");
        }
    }

    @GetMapping("/vn-pay-callback")
    public void payCallbackHandler(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String status = request.getParameter("vnp_ResponseCode");
        String paymentId = request.getParameter("vnp_TxnRef");
        String bookingId = request.getParameter("vnp_TxnRef");
        String packageId = request.getParameter("packageId"); // Assuming you pass packageId in VNPayConfig

        if ("00".equals(status)) {
            // Successful payment
            if (bookingId != null) {
                paymentService.updatePaymentTransactionStatus(bookingId, true);
                response.sendRedirect("http://localhost:3000/payment-return/success-booking");
            } else if (packageId != null) {
                paymentService.updatePaymentTransactionStatus(packageId, true);
                response.sendRedirect("http://localhost:3000/payment-return/success-package");
            } else {
                // Handle unexpected case where neither bookingId nor packageId is present
                response.sendRedirect("http://localhost:3000/payment-return/failed");
            }
        } else {
            // Payment failed
            paymentService.updatePaymentTransactionStatus(paymentId, false);
            response.sendRedirect("http://localhost:3000/payment-return/failed");
        }
    }
}
