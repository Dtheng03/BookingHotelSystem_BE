package com.chinhbean.bookinghotel.repositories;

import com.chinhbean.bookinghotel.entities.Hotel;
import com.chinhbean.bookinghotel.enums.HotelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IHotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {
    Page<Hotel> findHotelsByPartnerId(Long partnerId, Pageable pageable);

    Page<Hotel> findAllByStatus(HotelStatus hotelStatus, Pageable pageable);

    @Query("SELECT DISTINCT h FROM Hotel h " +
            "JOIN h.location hl " +
            "JOIN h.roomTypes rt " +
            "WHERE hl.province = :province AND h.status = 'ACTIVE' " +
            "GROUP BY h.id, hl.id " +
            "HAVING SUM(rt.numberOfRoom) >= :numberOfRoom " +
            "AND SUM(rt.capacityPerRoom * rt.numberOfRoom) >= :numPeople")
    List<Hotel> findPotentialHotels(
            @Param("province") String province,
            @Param("numPeople") Integer numPeople,
            @Param("numberOfRoom") Integer numberOfRoom,
            Pageable pageable);


    @Query("SELECT DISTINCT h FROM Hotel h " +
            "JOIN h.conveniences c " +
            "LEFT JOIN FETCH h.roomTypes rt " +
            "LEFT JOIN FETCH rt.type t " +
            "LEFT JOIN FETCH rt.roomConveniences rc " +
            "LEFT JOIN FETCH rt.roomImages " +
            "LEFT JOIN FETCH h.location l " +
            "WHERE ((:rating IS NULL OR h.rating = :rating) OR " +
            "(:freeBreakfast IS NULL OR c.freeBreakfast = :freeBreakfast) OR " +
            "(:pickUpDropOff IS NULL OR c.pickUpDropOff = :pickUpDropOff) OR " +
            "(:restaurant IS NULL OR c.restaurant = :restaurant) OR " +
            "(:bar IS NULL OR c.bar = :bar) OR " +
            "(:pool IS NULL OR c.pool = :pool) OR " +
            "(:freeInternet IS NULL OR c.freeInternet = :freeInternet) OR " +
            "(:reception24h IS NULL OR c.reception24h = :reception24h) OR " +
            "(:laundry IS NULL OR c.laundry = :laundry)) AND " +
            "h.status = 'ACTIVE'")
    Page<Hotel> filterHotelWithConvenience(@Param("rating") Integer rating,
                                           @Param("freeBreakfast") Boolean freeBreakfast,
                                           @Param("pickUpDropOff") Boolean pickUpDropOff,
                                           @Param("restaurant") Boolean restaurant,
                                           @Param("bar") Boolean bar,
                                           @Param("pool") Boolean pool,
                                           @Param("freeInternet") Boolean freeInternet,
                                           @Param("reception24h") Boolean reception24h,
                                           @Param("laundry") Boolean laundry,
                                           Pageable pageable);

}
