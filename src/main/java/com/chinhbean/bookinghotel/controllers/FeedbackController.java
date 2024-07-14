package com.chinhbean.bookinghotel.controllers;

import com.chinhbean.bookinghotel.dtos.FeedbackDTO;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import com.chinhbean.bookinghotel.responses.ResponseObject;
import com.chinhbean.bookinghotel.responses.feedback.FeedbackResponse;
import com.chinhbean.bookinghotel.services.feedback.IFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/feedbacks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeedbackController {
    private final IFeedbackService feedbackService;

    @GetMapping("/get-all-feedback/{hotelId}")
    public ResponseEntity<ResponseObject> getAllFeedback(@PathVariable Long hotelId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {

        Page<FeedbackResponse> feedback = feedbackService.findAllFeedbacksByHotelId(hotelId, page, size);
        if (feedback.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message("feedbacks not found")
                    .data(null)
                    .build());
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .data(feedback)
                    .message("Retrieved feedbacks successfully")
                    .build());
        }
    }

    @GetMapping("/get-feedback/{feedbackId}")
    public ResponseEntity<ResponseObject> getFeedback(@PathVariable Long feedbackId) {
        try {
            FeedbackResponse feedback = feedbackService.findFeedbackById(feedbackId);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .data(feedback)
                    .message("Retrieved feedback successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/create-feedback")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CUSTOMER')")
    public ResponseEntity<ResponseObject> createFeedback(@RequestBody FeedbackDTO feedback) {
        try {
            FeedbackResponse createdFeedback = feedbackService.createFeedback(feedback);
            return ResponseEntity.status(HttpStatus.CREATED).body(ResponseObject.builder()
                    .status(HttpStatus.CREATED)
                    .data(createdFeedback)
                    .message("Feedback created successfully")
                    .build());
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @DeleteMapping("/delete-feedback/{feedbackId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CUSTOMER')")
    public ResponseEntity<ResponseObject> deleteFeedback(@PathVariable Long feedbackId) {
        try {
            feedbackService.deleteFeedback(feedbackId);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Feedback deleted successfully")
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PutMapping("/update-feedback/{feedbackId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CUSTOMER')")
    public ResponseEntity<ResponseObject> updateFeedback(@PathVariable Long feedbackId, @RequestBody FeedbackDTO feedback) {
        try {
            FeedbackResponse updatedFeedback = feedbackService.updateFeedback(feedbackId, feedback);
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .data(updatedFeedback)
                    .message("Feedback updated successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/get-all-feedbacks-by-user-id")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CUSTOMER','ROLE_PARTNER')")
    public ResponseEntity<ResponseObject> getAllFeedbacksByUserId(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {

        Page<FeedbackResponse> feedbacks = feedbackService.findAllFeedbacksByUserId(page, size);
        if (feedbacks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message("Feedbacks not found")
                    .data(null)
                    .build());
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .data(feedbacks)
                    .message("Retrieved feedbacks successfully")
                    .build());
        }
    }
}
