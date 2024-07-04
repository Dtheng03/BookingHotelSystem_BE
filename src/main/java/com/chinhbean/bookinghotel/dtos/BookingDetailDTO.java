package com.chinhbean.bookinghotel.dtos;

import com.chinhbean.bookinghotel.entities.BookingDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BookingDetailDTO {
    @JsonProperty("room-type-id")
    private Long roomTypeId;
    private Float price;
    @JsonProperty("number-of-rooms")
    private Integer numberOfRooms;
    @JsonProperty("total-money")
    private BigDecimal totalMoney;
    @JsonProperty("room-name")
    private String roomName;



    public static BookingDetailDTO fromBookingDetail(BookingDetails bookingDetail) {
        return BookingDetailDTO.builder()
                .roomTypeId(bookingDetail.getRoomType().getId())
                .roomName(bookingDetail.getRoomType().getRoomTypeName())
                .price(bookingDetail.getPrice())
                .numberOfRooms(bookingDetail.getNumberOfRooms())
                .totalMoney(bookingDetail.getTotalMoney())
                .build();
    }
}
