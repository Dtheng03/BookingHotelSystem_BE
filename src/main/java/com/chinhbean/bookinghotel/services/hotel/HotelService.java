package com.chinhbean.bookinghotel.services.hotel;

import com.chinhbean.bookinghotel.components.JwtTokenUtils;
import com.chinhbean.bookinghotel.dtos.ConvenienceDTO;
import com.chinhbean.bookinghotel.dtos.HotelDTO;
import com.chinhbean.bookinghotel.dtos.HotelLocationDTO;
import com.chinhbean.bookinghotel.entities.*;
import com.chinhbean.bookinghotel.enums.HotelStatus;
import com.chinhbean.bookinghotel.enums.PackageStatus;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import com.chinhbean.bookinghotel.exceptions.PermissionDenyException;
import com.chinhbean.bookinghotel.repositories.IBookingRepository;
import com.chinhbean.bookinghotel.repositories.IConvenienceRepository;
import com.chinhbean.bookinghotel.repositories.IHotelRepository;
import com.chinhbean.bookinghotel.responses.hotel.HotelResponse;
import com.chinhbean.bookinghotel.utils.MessageKeys;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelService implements IHotelService {

    private final IHotelRepository hotelRepository;
    private final IConvenienceRepository convenienceRepository;
    private final JwtTokenUtils jwtTokenUtils;
    private final UserDetailsService userDetailsService;
    private final IBookingRepository bookingRepository;
    private static final Logger logger = LoggerFactory.getLogger(HotelService.class);

    @Transactional
    @Override
    public Page<HotelResponse> getAllHotels(Integer page, Integer size) {
        logger.info("Fetching all ACTIVE hotels from the database.");
        Pageable pageable = PageRequest.of(page, size);
        Page<Hotel> hotels = hotelRepository.findAllByStatus(HotelStatus.ACTIVE, pageable);
        if (hotels.isEmpty()) {
            logger.warn("No ACTIVE hotels found in the database.");
            return Page.empty();
        }
        logger.info("Successfully retrieved all ACTIVE hotels.");
        return hotels.map(HotelResponse::fromHotel);
    }

    @Transactional
    @Override
    public Page<HotelResponse> getAdminHotels(Integer page, Integer size) {
        logger.info("Fetching all hotels from the database.");
        Pageable pageable = PageRequest.of(page, size);
        Page<Hotel> hotels = hotelRepository.findAll(pageable);
        if (hotels.isEmpty()) {
            logger.warn("No hotels found in the database.");
            return Page.empty();
        }
        logger.info("Successfully retrieved all hotels.");
        return hotels.map(HotelResponse::fromHotel);
    }

    @Transactional
    @Override
    public Page<HotelResponse> getPartnerHotels(Integer page, Integer size, User userDetails) throws PermissionDenyException {
        PackageStatus packageStatus = getPackageStatus(userDetails);
        checkPackageStatus(packageStatus);
        logger.info("Getting hotels for partner with ID: {}", userDetails.getId());
        Pageable pageable = PageRequest.of(page, size);
        Page<Hotel> hotels = hotelRepository.findHotelsByPartnerId(userDetails.getId(), pageable);
        if (hotels.isEmpty()) {
            logger.warn("No hotels found for the partner with ID: {}", userDetails.getId());
            return Page.empty();
        }
        logger.info("Successfully retrieved all hotels for the partner with ID: {}", userDetails.getId());
        return hotels.map(HotelResponse::fromHotel);
    }


    @Transactional
    @Override
    public HotelResponse getHotelDetail(Long hotelId, HttpServletRequest request) throws DataNotFoundException, PermissionDenyException {
        logger.info("Fetching details for hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> {
                    logger.error("Hotel with ID: {} does not exist.", hotelId);
                    return new DataNotFoundException(MessageKeys.HOTEL_DOES_NOT_EXISTS);
                });

        User currentUser = null;
        try {
            currentUser = getCurrentUser(request);
        } catch (Exception e) {
            logger.info("Unauthenticated access to hotel details with ID: {}", hotelId);
        }

        if (currentUser != null && currentUser.getRole().getRoleName().equalsIgnoreCase("PARTNER")) {
            PackageStatus packageStatus = getPackageStatus(currentUser);
            checkPackageStatus(packageStatus);
        }

        if (!HotelStatus.ACTIVE.equals(hotel.getStatus())) {
            if (currentUser == null ||
                    (!currentUser.getId().equals(hotel.getPartner().getId()) &&
                            currentUser.getRole().getId() != 1)) {
                logger.warn("Attempt to access inactive hotel with ID: {} by unauthorized user", hotelId);
                throw new DataNotFoundException(MessageKeys.HOTEL_DOES_NOT_EXISTS);
            }
        } else {
            if (currentUser != null &&
                    currentUser.getRole().getRoleName().equalsIgnoreCase("PARTNER") &&
                    !currentUser.getId().equals(hotel.getPartner().getId()) && currentUser.getRole().getId() != 1) {
                logger.warn("Partner attempted to access another partner's hotel. Hotel ID: {}, User ID: {}", hotelId, currentUser.getId());
                throw new PermissionDenyException(MessageKeys.PARTNER_CANNOT_VIEW_OTHER_HOTELS);
            }
        }

        logger.info("Successfully retrieved details for hotel with ID: {}", hotelId);
        return HotelResponse.fromHotel(hotel);
    }

    @Transactional
    @Override
    public HotelResponse createHotel(HotelDTO hotelDTO) throws PermissionDenyException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser != null && currentUser.getRole().getRoleName().equalsIgnoreCase("PARTNER")) {
            PackageStatus packageStatus = getPackageStatus(currentUser);
            checkPackageStatus(packageStatus);
        }
        logger.info("Creating a new hotel with name: {}", hotelDTO.getHotelName());
        Hotel hotel = convertToEntity(hotelDTO);
        hotel.setPartner(currentUser);
        Set<Convenience> newConveniences = hotel.getConveniences().stream()
                .filter(convenience -> convenience.getId() == null)
                .collect(Collectors.toSet());
        convenienceRepository.saveAll(newConveniences);
        Hotel savedHotel = hotelRepository.save(hotel);
        logger.info("Hotel created successfully with ID: {}", savedHotel.getId());
        return HotelResponse.fromHotel(savedHotel);
    }

    private Hotel convertToEntity(HotelDTO hotelDTO) {
        HotelLocation location = new HotelLocation();
        location.setAddress(hotelDTO.getLocation().getAddress());
        location.setProvince(hotelDTO.getLocation().getProvince());
        location.setLatitude(hotelDTO.getLocation().getLatitude());
        location.setLongitude(hotelDTO.getLocation().getLongitude());

        Set<Convenience> conveniences = hotelDTO.getConveniences().stream()
                .map(this::createNewConvenience)
                .collect(Collectors.toSet());
        Hotel hotel = Hotel.builder()
                .hotelName(hotelDTO.getHotelName())
                .rating(hotelDTO.getRating())
                .description(hotelDTO.getDescription())
                .brand(hotelDTO.getBrand())
                .status(HotelStatus.PENDING)
                .conveniences(conveniences)
                .location(location)
                .build();
        location.setHotel(hotel);
        return hotel;
    }

    private Convenience convertToConvenienceEntity(ConvenienceDTO dto) {
        return convenienceRepository.findByFreeBreakfastAndPickUpDropOffAndRestaurantAndBarAndPoolAndFreeInternetAndReception24hAndLaundry(
                        dto.getFreeBreakfast(),
                        dto.getPickUpDropOff(),
                        dto.getRestaurant(),
                        dto.getBar(),
                        dto.getPool(),
                        dto.getFreeInternet(),
                        dto.getReception24h(),
                        dto.getLaundry())
                .orElseGet(() -> createNewConvenience(dto));
    }

    private Convenience createNewConvenience(ConvenienceDTO dto) {
        Convenience convenience = new Convenience();
        convenience.setFreeBreakfast(dto.getFreeBreakfast());
        convenience.setPickUpDropOff(dto.getPickUpDropOff());
        convenience.setRestaurant(dto.getRestaurant());
        convenience.setBar(dto.getBar());
        convenience.setPool(dto.getPool());
        convenience.setFreeInternet(dto.getFreeInternet());
        convenience.setReception24h(dto.getReception24h());
        convenience.setLaundry(dto.getLaundry());
        return convenience;
    }

    @Transactional
    @Override
    public HotelResponse updateHotel(Long hotelId, HotelDTO updateDTO) throws DataNotFoundException, PermissionDenyException {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.HOTEL_DOES_NOT_EXISTS));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser != null && currentUser.getRole().getRoleName().equalsIgnoreCase("PARTNER")) {
            PackageStatus packageStatus = getPackageStatus(currentUser);
            checkPackageStatus(packageStatus);
        }

        if (hotel.getStatus() == HotelStatus.PENDING) {
            throw new PermissionDenyException(MessageKeys.HOTEL_IS_PENDING);
        }
        assert currentUser != null;
        if (!currentUser.getId().equals(hotel.getPartner().getId())) {
            throw new PermissionDenyException(MessageKeys.USER_DOES_NOT_HAVE_PERMISSION_TO_UPDATE_HOTEL);
        }
        if (updateDTO.getHotelName() != null) {
            hotel.setHotelName(updateDTO.getHotelName());
        }
        if (updateDTO.getRating() != null) {
            hotel.setRating(updateDTO.getRating());
        }
        if (updateDTO.getDescription() != null) {
            hotel.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getBrand() != null) {
            hotel.setBrand(updateDTO.getBrand());
        }
        if (updateDTO.getLocation() != null) {
            HotelLocationDTO locationDTO = updateDTO.getLocation();
            HotelLocation location = hotel.getLocation();
            location.setAddress(locationDTO.getAddress());
            location.setProvince(locationDTO.getProvince());
        }

        if (updateDTO.getConveniences() != null) {
            Set<Convenience> conveniences = updateDTO.getConveniences().stream()
                    .map(this::convertToConvenienceEntity)
                    .collect(Collectors.toSet());
            hotel.setConveniences(conveniences);
        }

        Hotel updatedHotel = hotelRepository.save(hotel);
        return HotelResponse.fromHotel(updatedHotel);
    }

    @Transactional
    @Override
    public void updateStatus(Long hotelId, HotelStatus newStatus) throws DataNotFoundException, PermissionDenyException {
        Hotel hotel = getHotelById(hotelId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser != null && currentUser.getRole().getRoleName().equalsIgnoreCase("PARTNER")) {
            PackageStatus packageStatus = getPackageStatus(currentUser);
            checkPackageStatus(packageStatus);
        }
        assert currentUser != null;
        if (Role.ADMIN.equals(currentUser.getRole().getRoleName())) {
            hotel.setStatus(newStatus);
        } else if (Role.PARTNER.equals(currentUser.getRole().getRoleName())) {
            if (newStatus == HotelStatus.ACTIVE || newStatus == HotelStatus.INACTIVE || newStatus == HotelStatus.CLOSED) {
                hotel.setStatus(newStatus);
            } else {
                throw new PermissionDenyException(MessageKeys.USER_CANNOT_CHANGE_STATUS);
            }
        } else {
            throw new PermissionDenyException(MessageKeys.USER_DOES_NOT_HAVE_PERMISSION_TO_CHANGE_STATUS);
        }
        hotelRepository.save(hotel);
    }

    @Override
    public Hotel getHotelById(Long hotelId) throws DataNotFoundException {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.NO_HOTELS_FOUND));
    }

    @Override
    public Page<HotelResponse> findHotelsByProvinceAndDatesAndCapacity(String province, Integer numPeople, LocalDate checkInDate, LocalDate checkOutDate, Integer numberOfRoom, Integer page, Integer size) {
        // Validate input parameters
        validateInputParameters(province, numPeople, checkInDate, checkOutDate, numberOfRoom, page, size);

        Pageable pageable = PageRequest.of(page, size);

        List<Hotel> potentialHotels = hotelRepository.findPotentialHotels(province, numPeople, numberOfRoom, pageable);

        List<Hotel> availableHotels = potentialHotels.stream()
                .filter(hotel -> hasEnoughAvailableRooms(hotel, checkInDate, checkOutDate, numPeople, numberOfRoom))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), availableHotels.size());
        Page<Hotel> hotelPage = new PageImpl<>(availableHotels.subList(start, end), pageable, availableHotels.size());
        return hotelPage.map(HotelResponse::fromHotel);
    }

    private void validateInputParameters(String province, Integer numPeople, LocalDate checkInDate, LocalDate checkOutDate, Integer numberOfRoom, Integer page, Integer size) {
        if (StringUtils.isBlank(province)) {
            throw new IllegalArgumentException("Province must not be blank");
        }
        if (numPeople == null || numPeople <= 0) {
            throw new IllegalArgumentException("Number of people must be positive");
        }
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates must not be null");
        }
        if (checkInDate.isAfter(checkOutDate)) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        if (numberOfRoom == null || numberOfRoom <= 0) {
            throw new IllegalArgumentException("Number of rooms must be positive");
        }
        if (page == null || page < 0 || size == null || size <= 0) {
            throw new IllegalArgumentException("Invalid page or size");
        }
    }

    private boolean hasEnoughAvailableRooms(Hotel hotel, LocalDate checkInDate, LocalDate checkOutDate, Integer numPeople, Integer numberOfRoom) {
        List<RoomType> availableRoomTypes = hotel.getRoomTypes().stream()
                .filter(rt -> isRoomTypeAvailable(rt, checkInDate, checkOutDate, numberOfRoom))
                .toList();

        int totalAvailableRooms = availableRoomTypes.stream().mapToInt(RoomType::getNumberOfRoom).sum();
        int totalCapacity = availableRoomTypes.stream().mapToInt(rt -> rt.getCapacityPerRoom() * rt.getNumberOfRoom()).sum();

        return totalAvailableRooms >= numberOfRoom && totalCapacity >= numPeople;
    }

    private boolean isRoomTypeAvailable(RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate, Integer numberOfRoom) {
        long bookedRooms = bookingRepository.countBookedRooms(roomType.getId(), checkInDate, checkOutDate);
        return (roomType.getNumberOfRoom() - bookedRooms) >= numberOfRoom;
    }

    @Override
    public Page<HotelResponse> filterHotelsByConveniencesAndRating(Integer rating, Boolean freeBreakfast, Boolean pickUpDropOff, Boolean restaurant, Boolean bar, Boolean pool, Boolean freeInternet, Boolean reception24h, Boolean laundry, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Hotel> hotels = hotelRepository.filterHotelWithConvenience(rating, freeBreakfast, pickUpDropOff, restaurant, bar, pool, freeInternet, reception24h, laundry, pageable);
        return hotels.map(HotelResponse::fromHotel);
    }

    @Override
    public void deleteHotel(Long hotelId) throws DataNotFoundException {
        if (!hotelRepository.existsById(hotelId)) {
            throw new DataNotFoundException(MessageKeys.HOTEL_DOES_NOT_EXISTS);
        }
        hotelRepository.deleteById(hotelId);
    }

    private User getCurrentUser(HttpServletRequest request) throws PermissionDenyException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new PermissionDenyException(MessageKeys.AUTH_TOKEN_MISSING_OR_INVALID);
        }
        final String token = authHeader.substring(7);
        final Map<String, String> identifiers = jwtTokenUtils.extractIdentifier(token);
        if (identifiers == null || (identifiers.get("email") == null && identifiers.get("phoneNumber") == null)) {
            throw new PermissionDenyException(MessageKeys.TOKEN_NO_IDENTIFIER);
        }
        String emailOrPhone = identifiers.get("email") != null ? identifiers.get("email") : identifiers.get("phoneNumber");
        try {
            return (User) userDetailsService.loadUserByUsername(emailOrPhone);
        } catch (UsernameNotFoundException e) {
            throw new PermissionDenyException(MessageKeys.USER_NOT_FOUND);
        }
    }

    private void checkPackageStatus(PackageStatus packageStatus) throws PermissionDenyException {
        if (packageStatus != PackageStatus.ACTIVE) {
            throw new PermissionDenyException("Invalid package status: " + packageStatus);
        }
    }

    private PackageStatus getPackageStatus(User user) {
        PackageStatus packageStatus = user.getStatus();
        if (packageStatus == null) {
            throw new IllegalStateException("Package status not found for the user with ID: " + user.getId());
        }
        return packageStatus;
    }
}