package com.chinhbean.bookinghotel.services.feedback;

import com.chinhbean.bookinghotel.dtos.FeedbackDTO;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import com.chinhbean.bookinghotel.responses.feedback.FeedbackResponse;
import org.springframework.data.domain.Page;

public interface IFeedbackService {
    Page<FeedbackResponse> findAllFeedbacksByHotelId(Long hotelId, int page, int size);

    FeedbackResponse findFeedbackById(Long feedbackId) throws DataNotFoundException;

    FeedbackResponse createFeedback(FeedbackDTO feedbackDTO) throws DataNotFoundException;

    void deleteFeedback(Long feedbackId) throws DataNotFoundException;

    FeedbackResponse updateFeedback(Long feedbackId, FeedbackDTO feedbackDTO) throws DataNotFoundException;

    Page<FeedbackResponse> findAllFeedbacksByUserId(int page, int size);
}
