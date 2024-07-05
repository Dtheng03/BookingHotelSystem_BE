package com.chinhbean.bookinghotel.repositories;

import com.chinhbean.bookinghotel.entities.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IFeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByHotelId(Long hotelId, Pageable pageable);

    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);
}
