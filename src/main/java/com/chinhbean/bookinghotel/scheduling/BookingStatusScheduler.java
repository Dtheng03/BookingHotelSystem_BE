package com.chinhbean.bookinghotel.scheduling;

import com.chinhbean.bookinghotel.entities.Booking;
import com.chinhbean.bookinghotel.entities.BookingDetails;
import com.chinhbean.bookinghotel.enums.BookingStatus;
import com.chinhbean.bookinghotel.repositories.IBookingRepository;
import com.chinhbean.bookinghotel.repositories.IRoomTypeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingStatusScheduler {

    private final IBookingRepository bookingRepository;
    private final IRoomTypeRepository roomTypeRepository;

    public BookingStatusScheduler(IBookingRepository bookingRepository, IRoomTypeRepository roomTypeRepository) {
        this.bookingRepository = bookingRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void updateBookingAndRoomStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findAll();

        for (Booking booking : bookings) {
            updateBookingStatus(booking, now);
        }
    }

    private void updateBookingStatus(Booking booking, LocalDateTime now) {
        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.CONFIRMED) {
            if (now.toLocalDate().equals(booking.getCheckInDate())) {
                booking.setStatus(BookingStatus.CHECKED_IN);
                bookingRepository.save(booking);
            } else if (now.toLocalDate().equals(booking.getCheckOutDate())) {
                booking.setStatus(BookingStatus.CHECKED_OUT);
                bookingRepository.save(booking);
                increaseRoomQuantityForBookingDetails(booking.getBookingDetails());
            }
        }
    }

    private void increaseRoomQuantityForBookingDetails(List<BookingDetails> bookingDetails) {
        for (BookingDetails detail : bookingDetails) {
            detail.getRoomType().setNumberOfRoom(detail.getRoomType().getNumberOfRoom() + detail.getNumberOfRooms());
            roomTypeRepository.save(detail.getRoomType());
        }
    }
}