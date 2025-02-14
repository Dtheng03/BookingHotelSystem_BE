package com.chinhbean.bookinghotel.utils;

public class MailTemplate {
    public final static class SEND_MAIL_SUBJECT {
        public final static String USER_REGISTER = "ĐĂNG KÍ THÀNH CÔNG!";

        public final static String OTP_SEND = "MÃ OTP XÁC THỰC";

        public final static String NEW_PASSWORD = "NEW LOGIN INFORMATION";

        public final static String BOOKING_PAYMENT_SUCCESS = "BOOKING PAYMENT SUCCESSFUL!";

        public final static String PACKAGE_PAYMENT_SUCCESS = "PACKAGE PAYMENT SUCCESSFUL!";
    }

    public final static class SEND_MAIL_TEMPLATE {
        public final static String USER_REGISTER = "register";

        public final static String OTP_SEND_TEMPLATE = "otp-sent";

        public final static String NEW_PASSWORD = "new-password";
        public final static String BOOKING_PAYMENT_SUCCESS_TEMPLATE = "booking";

        public final static String PACKAGE_PAYMENT_SUCCESS_TEMPLATE = "package-payment";
    }
}
