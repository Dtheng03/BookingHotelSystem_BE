    package com.chinhbean.bookinghotel.enums;

    public enum BookingStatus {
        //PENDING: Đặt phòng đã được tạo nhưng chưa được xác nhận hoặc thanh toán.

        PENDING
        //PAID: Đặt phòng đã được thanh toán.

        ,PAID,
        //CHECKED_IN: Khách hàng đã nhận phòng.

        CHECKED_IN,
        //CHECKED_OUT: Khách hàng đã trả phòng.

        CHECKED_OUT,
        //CANCELLED: Đặt phòng đã bị hủy bởi khách hàng hoặc nhà cung cấp dịch vụ.

        CANCELLED


    }
