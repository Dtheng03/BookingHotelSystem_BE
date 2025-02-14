package com.chinhbean.bookinghotel.services.booking;

import com.chinhbean.bookinghotel.dtos.BookingDTO;
import com.chinhbean.bookinghotel.dtos.BookingDetailDTO;
import com.chinhbean.bookinghotel.dtos.DataMailDTO;
import com.chinhbean.bookinghotel.entities.*;
import com.chinhbean.bookinghotel.enums.BookingStatus;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import com.chinhbean.bookinghotel.exceptions.PermissionDenyException;
import com.chinhbean.bookinghotel.repositories.*;
import com.chinhbean.bookinghotel.responses.booking.BookingResponse;
import com.chinhbean.bookinghotel.services.sendmails.IMailService;
import com.chinhbean.bookinghotel.utils.ExcelFileExporter;
import com.chinhbean.bookinghotel.utils.MailTemplate;
import com.chinhbean.bookinghotel.utils.MessageKeys;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@EnableAsync
public class BookingService implements IBookingService {

    private final IBookingRepository bookingRepository;
    private final IUserRepository IUserRepository;
    private final IRoomTypeRepository roomTypeRepository;
    private final IBookingDetailRepository bookingDetailRepository;
    private final IHotelRepository hotelRepository;
    private final IMailService mailService;
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Transactional
    @Override
    public BookingResponse createBooking(BookingDTO bookingDTO) throws PermissionDenyException {
        User user;
        Booking booking;

        if (bookingDTO.getUserId() != null) {
            user = IUserRepository.findById(bookingDTO.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User with ID: " + bookingDTO.getUserId() + " does not exist."));

        } else {
            user = IUserRepository.findByFullName("guest")
                    .orElseThrow(() -> new IllegalArgumentException("Guest user does not exist."));
        }
        Hotel hotel = hotelRepository.findById(bookingDTO.getHotelId())
                .orElseThrow(() -> new IllegalArgumentException("Hotel with ID: " + bookingDTO.getHotelId() + " does not exist."));

        booking = getBooking(bookingDTO, user, hotel);
        booking.setExpirationDate(LocalDateTime.now().plusSeconds(300));

        // Save booking first
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingDetails> bookingDetails = new ArrayList<>();
        for (BookingDetailDTO bookingDetailDTO : bookingDTO.getBookingDetails()) {
            BookingDetails bookingDetail = new BookingDetails();
            bookingDetail.setBooking(savedBooking);

            Long roomTypeId = bookingDetailDTO.getRoomTypeId();
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Room type with ID: " + roomTypeId + " does not exist."));

            if (bookingDetailDTO.getNumberOfRooms() < 1) {

                throw new IllegalArgumentException("Number of rooms must be greater than 0");
            }
            // Decrease room quantity
            int updatedRows = roomTypeRepository.decrementRoomQuantity(roomTypeId, bookingDetailDTO.getNumberOfRooms());
            if (updatedRows == 0) {
                throw new IllegalStateException("Not enough rooms with ID: " + roomTypeId);
            }

            bookingDetail.setRoomType(roomType);
            bookingDetail.setPrice(bookingDetailDTO.getPrice());
            bookingDetail.setNumberOfRooms(bookingDetailDTO.getNumberOfRooms());
            bookingDetail.setTotalMoney(bookingDetailDTO.getTotalMoney());
            bookingDetails.add(bookingDetail);
        }
        savedBooking.setBookingDetails(bookingDetails);
        bookingDetailRepository.saveAll(bookingDetails);

        // Initialize lazy-loaded collections
        savedBooking.getBookingDetails().forEach(detail -> {
            RoomType roomType = detail.getRoomType();
            int roomImagesSize = roomType.getRoomImages().size();
            int roomConveniencesSize = roomType.getRoomConveniences().size();
            Long typeId = roomType.getType().getId();
            logger.info("RoomType ID: {} has {} images and {} conveniences.", typeId, roomImagesSize, roomConveniencesSize);
        });

        // Schedule a task to delete booking after 300 seconds if still PENDING
        scheduler.schedule(() -> deleteBookingIfPending(savedBooking.getBookingId()), 300, TimeUnit.SECONDS);

        // Convert savedBooking to BookingResponse
        return BookingResponse.fromBooking(savedBooking);
    }


    @Async
    public CompletableFuture<Void> deleteBookingIfPending(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null && BookingStatus.PENDING.equals(booking.getStatus())) {
            List<BookingDetails> bookingDetails = bookingDetailRepository.findByBookingId(bookingId);
            for (BookingDetails bookingDetail : bookingDetails) {
                roomTypeRepository.incrementRoomQuantity(bookingDetail.getRoomType().getId(), bookingDetail.getNumberOfRooms());
            }
            bookingRepository.delete(booking);
            logger.info("Deleted expired booking with ID: {}", bookingId);
        }
        return CompletableFuture.completedFuture(null);
    }

    private static Booking getBooking(BookingDTO bookingDTO, User user, Hotel hotel) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setEmail(bookingDTO.getEmail());
        booking.setPhoneNumber(bookingDTO.getPhoneNumber());
        booking.setFullName(bookingDTO.getFullName());
        booking.setTotalPrice(bookingDTO.getTotalPrice());
        booking.setCheckInDate(bookingDTO.getCheckInDate());
        booking.setCheckOutDate(bookingDTO.getCheckOutDate());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCouponId(bookingDTO.getCouponId());
        booking.setNote(bookingDTO.getNote());
        booking.setPaymentMethod(bookingDTO.getPaymentMethod());
        booking.setExpirationDate(LocalDateTime.now().plusSeconds(300));
        booking.setBookingDate(LocalDateTime.now());
        return booking;
    }


    @Transactional
    @Override
    public Page<BookingResponse> getListBooking(int page, int size) throws DataNotFoundException {
        logger.info("Fetching all bookings from the database.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Pageable pageable = PageRequest.of(page, size);

        Page<Booking> bookings;

        if (currentUser.getRole().getId() == 1) {
            bookings = bookingRepository.findAll(pageable);
        } else {
            bookings = bookingRepository.findAllByUserId(currentUser.getId(), pageable);

        }

        if (bookings.isEmpty()) {
            logger.warn("No bookings found in the database.");
            throw new DataNotFoundException(MessageKeys.NO_BOOKINGS_FOUND);
        }
        logger.info("Successfully retrieved all bookings.");
        return bookings.map(BookingResponse::fromBooking);
    }

    @Transactional
    @Override
    public Page<BookingResponse> getBookingsByHotel(Long hotelId, int page, int size) throws DataNotFoundException {
        logger.info("Fetching bookings for hotel with ID: {}", hotelId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Pageable pageable = PageRequest.of(page, size);

        Page<Booking> bookings;
        bookings = bookingRepository.findByHotel_IdAndHotel_Partner_Id(hotelId, currentUser.getId(), pageable);

        if (bookings.isEmpty()) {
            logger.warn("No bookings found for hotel with ID: {}", hotelId);
            throw new DataNotFoundException(MessageKeys.NO_BOOKINGS_FOUND);
        }
        logger.info("Successfully retrieved bookings for hotel with ID: {}", hotelId);

        bookings.forEach(booking -> Hibernate.initialize(booking.getBookingDetails()));

        List<BookingResponse> bookingResponses = bookings.stream()
                .map(BookingResponse::fromBooking)
                .collect(Collectors.toList());
        return new PageImpl<>(bookingResponses, pageable, bookings.getTotalElements());
    }

    @Override
    public void sendMailNotificationForBookingPayment(Booking booking) {
        try {
            DataMailDTO dataMail = new DataMailDTO();
            dataMail.setTo(booking.getPaymentTransaction().getEmailGuest());
            dataMail.setSubject(MailTemplate.SEND_MAIL_SUBJECT.BOOKING_PAYMENT_SUCCESS);

            NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            Map<String, Object> props = new HashMap<>();
            props.put("fullName", booking.getPaymentTransaction().getNameGuest());
            props.put("checkInDate", booking.getCheckInDate());
            props.put("checkOutDate", booking.getCheckOutDate());
            props.put("totalPrice", currencyFormatter.format(booking.getTotalPrice()));
            props.put("paymentMethod", booking.getPaymentMethod());
            props.put("note", booking.getNote());
            props.put("hotelName", booking.getHotel().getHotelName());
            props.put("bookingId", booking.getBookingId());

            // Format the booking date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedBookingDate = booking.getBookingDate().format(formatter);
            props.put("bookingDate", formattedBookingDate);

            // Add room details
            if (!booking.getBookingDetails().isEmpty()) {
                BookingDetails bookingDetail = booking.getBookingDetails().get(0); // Assuming there's at least one booking detail
                props.put("roomTypeName", bookingDetail.getRoomType().getRoomTypeName());
                props.put("numberOfRooms", bookingDetail.getNumberOfRooms());
                props.put("pricePerRoom", currencyFormatter.format(bookingDetail.getPrice()));
            }

            dataMail.setProps(props);
            mailService.sendHtmlMail(dataMail, MailTemplate.SEND_MAIL_TEMPLATE.BOOKING_PAYMENT_SUCCESS_TEMPLATE);
            logger.info("Successfully sent booking payment success email to: {}", booking.getPaymentTransaction().getEmailGuest());
        } catch (Exception exp) {
            logger.error("Failed to send booking payment success email", exp);
        }
    }


    @Override
    public Booking getBookingById(Long bookingId) throws DataNotFoundException {
        return bookingRepository.findWithDetailsAndPaymentTransactionById(bookingId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.NO_BOOKINGS_FOUND));
    }

    @Transactional
    @Override
    public Booking updateBooking(Long bookingId, BookingDTO bookingDTO) throws DataNotFoundException {
        logger.info("Updating booking with ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.NO_BOOKINGS_FOUND));
        if (bookingDTO.getTotalPrice() != null) {
            booking.setTotalPrice(bookingDTO.getTotalPrice());
        }
        if (bookingDTO.getCheckInDate() != null) {
            booking.setCheckInDate(bookingDTO.getCheckInDate());
        }
        if (bookingDTO.getCheckOutDate() != null) {
            booking.setCheckOutDate(bookingDTO.getCheckOutDate());
        }
        if (bookingDTO.getCouponId() != null) {
            booking.setCouponId(bookingDTO.getCouponId());
        }
        if (bookingDTO.getNote() != null) {
            booking.setNote(bookingDTO.getNote());
        }
        if (bookingDTO.getPaymentMethod() != null) {
            booking.setPaymentMethod(bookingDTO.getPaymentMethod());
        }

        if (bookingDTO.getBookingDetails() != null) {
            List<BookingDetails> bookingDetails = bookingDTO.getBookingDetails().stream()
                    .map(this::convertToEntity)
                    .collect(Collectors.toList());
            booking.setBookingDetails(bookingDetails);
        }

        logger.info("Booking with ID: {} updated successfully.", bookingId);
        return bookingRepository.save(booking);
    }

    @Transactional
    @Override
    public void updateStatus(Long bookingId, BookingStatus newStatus) throws DataNotFoundException, PermissionDenyException {
        logger.info("Updating status for booking with ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.NO_BOOKINGS_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Check user role and update status accordingly
        if (Role.ADMIN.equals(currentUser.getRole().getRoleName()) ||
                Role.PARTNER.equals(currentUser.getRole().getRoleName())) {

            switch (newStatus) {
                case PENDING, CONFIRMED, PAID, CHECKED_IN, CANCELLED:
                    booking.setStatus(newStatus);
                    break;
                case CHECKED_OUT:
                    booking.setStatus(newStatus);
                    List<BookingDetails> bookingDetails = bookingDetailRepository.findByBookingId(bookingId);
                    for (BookingDetails bookingDetail : bookingDetails) {
                        roomTypeRepository.incrementRoomQuantity(bookingDetail.getRoomType().getId(), bookingDetail.getNumberOfRooms());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid booking status");
            }
        } else {
            throw new PermissionDenyException("You do not have permission to update the booking status.");
        }

        bookingRepository.save(booking);
        logger.info("Status for booking with ID: {} updated successfully to {}.", bookingId, newStatus);
    }


    private BookingDetails convertToEntity(BookingDetailDTO detailDTO) {
        BookingDetails detail = new BookingDetails();
        RoomType roomType = roomTypeRepository.findById(detailDTO.getRoomTypeId()).orElse(null);
        detail.setRoomType(roomType);
        detail.setPrice(detailDTO.getPrice());
        detail.setNumberOfRooms(detailDTO.getNumberOfRooms());
        detail.setTotalMoney(detailDTO.getTotalMoney());
        return detail;
    }

    @Transactional
    @Override
    public BookingResponse getBookingDetail(Long bookingId) throws DataNotFoundException {
        logger.info("Fetching details for booking with ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    logger.error("Booking with ID: {} does not exist.", bookingId);
                    return new DataNotFoundException(MessageKeys.NO_BOOKINGS_FOUND);
                });
        logger.info("Successfully retrieved details for booking with ID: {}", bookingId);
        return BookingResponse.fromBooking(booking);
    }

    @Transactional
    @Override
    public void exportBookingsToExcel(Long partnerId, HttpServletResponse response, Integer year, Integer month, Integer day) throws IOException, DataNotFoundException {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (year != null && month != null && day != null) {
            startDate = LocalDateTime.of(year, month, day, 0, 0);
            endDate = startDate.plusDays(1).minusSeconds(1);
        } else if (year != null && month != null) {
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusSeconds(1);
        } else if (year != null) {
            startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            endDate = startDate.plusYears(1).minusSeconds(1);
        } else if (month != null) {
            Year currentYear = Year.now();
            startDate = LocalDateTime.of(currentYear.getValue(), month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusSeconds(1);
        } else if (day != null) {
            LocalDate today = LocalDate.now();
            startDate = LocalDateTime.of(today.getYear(), today.getMonth(), day, 0, 0);
            endDate = startDate.plusDays(1).minusSeconds(1);
        }

        logger.info("Finding bookings with parameters: partnerId={}, startDate={}, endDate={}", partnerId, startDate, endDate);

        // Find bookings based on the constructed date range
        List<Booking> bookings = bookingRepository.findByPartnerIdAndDateRange(partnerId, startDate, endDate);

        if (bookings.isEmpty()) {
            throw new DataNotFoundException("No bookings found for the specified criteria.");
        }

        // Export bookings to Excel
        ExcelFileExporter.exportBookingsListToExcel(bookings, response,
                startDate != null ? startDate.toLocalDate() : null,
                endDate != null ? endDate.toLocalDate() : null);
    }

}
