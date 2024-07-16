package com.chinhbean.bookinghotel.repositories;

import com.chinhbean.bookinghotel.entities.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface IFeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByHotelId(Long hotelId, Pageable pageable);

    Page<Feedback> findAllByUserId(Long userId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM Feedback f WHERE f.id = :id")
    void deleteById(@NonNull @Param("id") Long id);
}
