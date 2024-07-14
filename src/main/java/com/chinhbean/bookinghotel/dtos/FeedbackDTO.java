package com.chinhbean.bookinghotel.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackDTO {
    private Long hotelId;

    private Double rating;

    private String comment;
}
