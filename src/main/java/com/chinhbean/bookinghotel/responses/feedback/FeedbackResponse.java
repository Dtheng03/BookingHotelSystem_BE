package com.chinhbean.bookinghotel.responses.feedback;

import com.chinhbean.bookinghotel.entities.Feedback;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FeedbackResponse {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("hotelId")
    private Long hotelId;

    @JsonProperty("rating")
    private Double rating;

    @JsonProperty("comment")
    private String comment;

    public static FeedbackResponse fromFeedback(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .fullName(feedback.getUser().getFullName())
                .hotelId(feedback.getHotel().getId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .build();
    }
}
