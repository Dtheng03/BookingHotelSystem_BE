package com.chinhbean.bookinghotel.repositories;

import com.chinhbean.bookinghotel.entities.Booking;
import com.chinhbean.bookinghotel.entities.Hotel;
import com.chinhbean.bookinghotel.entities.User;
import com.chinhbean.bookinghotel.enums.BookingStatus;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IBookingRepository extends JpaRepository<Booking, Long> {
//    @Query("SELECT b FROM Booking b JOIN b.bookingDetails bd JOIN bd.roomType rt JOIN rt.hotel h WHERE h.partner.id = :partnerId")
//    Page<Booking> findAllByPartnerId(@Param("partnerId") Long partnerId, Pageable pageable);

    Page<Booking> findAllByUserId(Long userId, Pageable pageable);

    Set<Booking> findAllByStatusAndExpirationDateBefore(BookingStatus status, LocalDateTime dateTime);

    //    @Query("SELECT b FROM Booking b WHERE b.hotel.partner.id = :partnerId")
//    Page<Booking> findAllByPartnerId(@Param("partnerId") Long partnerId, Pageable pageable);
    @Query("SELECT b FROM Booking b WHERE b.hotel.id = :hotelId AND b.hotel.partner.id = :partnerId")
    Page<Booking> findByHotel_IdAndHotel_Partner_Id(Long hotelId, Long partnerId, Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH b.paymentTransaction pt " +
            "WHERE b.bookingId = :bookingId")
    Optional<Booking> findWithDetailsAndPaymentTransactionById(Long bookingId) throws DataNotFoundException;

    @Query("SELECT b FROM Booking b WHERE (:partnerId IS NULL OR b.hotel.partner.id = :partnerId) AND " +
            "(COALESCE(:startDate, NULL) IS NULL AND COALESCE(:endDate, NULL) IS NULL OR " +
            "b.bookingDate >= COALESCE(:startDate, b.bookingDate) AND b.bookingDate < COALESCE(:endDate, b.bookingDate))")
    List<Booking> findByPartnerIdAndDateRange(
            @Param("partnerId") Long partnerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(bd) FROM BookingDetails bd " +
            "JOIN bd.booking b " +
            "WHERE bd.roomType.id = :roomTypeId " +
            "AND b.checkOutDate > :checkIn " +
            "AND b.checkInDate < :checkOut")
    long countBookedRooms(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    List<Booking> findByUserAndHotelAndStatus(User user, Hotel hotel, BookingStatus status);
}
