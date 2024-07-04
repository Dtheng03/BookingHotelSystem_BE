package com.chinhbean.bookinghotel.services.feedback;

import com.chinhbean.bookinghotel.components.LocalizationUtils;
import com.chinhbean.bookinghotel.dtos.FeedbackDTO;
import com.chinhbean.bookinghotel.entities.Feedback;
import com.chinhbean.bookinghotel.entities.Hotel;
import com.chinhbean.bookinghotel.entities.User;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import com.chinhbean.bookinghotel.repositories.IFeedbackRepository;
import com.chinhbean.bookinghotel.repositories.IHotelRepository;
import com.chinhbean.bookinghotel.responses.feedback.FeedbackResponse;
import com.chinhbean.bookinghotel.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService implements IFeedbackService {
    private final IFeedbackRepository feedbackRepository;
    private final LocalizationUtils localizationUtils;
    private final IHotelRepository hotelRepository;
    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);

    @Override
    public Page<FeedbackResponse> findAllFeedbacksByHotelId(Long hotelId, int page, int size) {
        logger.info("Getting feedbacks for the hotel with ID: {}", hotelId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbacks = feedbackRepository.findAllByHotelId(hotelId, pageable);
        if (feedbacks.isEmpty()) {
            logger.warn("No feedbacks found for the hotel with ID: {}", hotelId);
            return Page.empty();
        }
        logger.info("Successfully retrieved all feedbacks for the hotel with ID: {}", hotelId);
        return feedbacks.map(FeedbackResponse::fromFeedback);
    }

    @Override
    public FeedbackResponse findFeedbackById(Long feedbackId) throws DataNotFoundException {
        logger.info("Fetching details for feedback with ID: {}", feedbackId);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> {
                    logger.error("Feedback with ID: {} does not exist.", feedbackId);
                    return new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.FEEDBACK_DOES_NOT_EXISTS));
                });

        logger.info("Successfully retrieved details for feedback with ID: {}", feedbackId);
        return FeedbackResponse.fromFeedback(feedback);
    }

    @Override
    public FeedbackResponse createFeedback(FeedbackDTO feedbackDTO) throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Hotel hotel = hotelRepository.findById(feedbackDTO.getHotelId()).orElseThrow(() ->
                new DataNotFoundException("Hotel not found with ID: " + feedbackDTO.getHotelId()));
        logger.info("Creating feedback for the hotel with ID: {}", feedbackDTO.getHotelId());
        Feedback feedback = new Feedback();
        feedback.setHotel(hotel);
        feedback.setUser(currentUser);
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComment(feedbackDTO.getComment());
        Feedback savedFeedback = feedbackRepository.save(feedback);
        logger.info("Successfully created feedback for the hotel with ID: {}", savedFeedback.getHotel().getId());
        return FeedbackResponse.fromFeedback(savedFeedback);
    }

    @Override
    public void deleteFeedback(Long feedbackId) throws DataNotFoundException {
        logger.info("Deleting feedback with ID: {}", feedbackId);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new DataNotFoundException("Feedback not found with ID: " + feedbackId));
        feedbackRepository.delete(feedback);
        logger.info("Successfully deleted feedback with ID: {}", feedbackId);
    }

    @Override
    public FeedbackResponse updateFeedback(Long feedbackId, FeedbackDTO feedbackDTO) throws DataNotFoundException {
        logger.info("Updating feedback with ID: {}", feedbackId);
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new DataNotFoundException("Feedback not found with ID: " + feedbackId));

        if (feedbackDTO.getRating() != null) {
            feedback.setRating(feedbackDTO.getRating());
        }
        if (feedbackDTO.getComment() != null) {
            feedback.setComment(feedbackDTO.getComment());
        }
        Feedback updatedFeedback = feedbackRepository.save(feedback);

        logger.info("Successfully updated feedback with ID: {}", updatedFeedback.getId());
        return FeedbackResponse.fromFeedback(updatedFeedback);
    }

    @Override
    public Page<FeedbackResponse> findAllFeedbacksByUserId(int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        logger.info("Getting feedbacks for the user with ID: {}", currentUser.getId());
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbacks = feedbackRepository.findAllByUserId(currentUser.getId(), pageable);
        if (feedbacks.isEmpty()) {
            logger.warn("No feedbacks found for the user with ID: {}", currentUser.getId());
            return Page.empty();
        }
        logger.info("Successfully retrieved all feedbacks for the user with ID: {}", currentUser.getId());
        return feedbacks.map(FeedbackResponse::fromFeedback);
    }
}
