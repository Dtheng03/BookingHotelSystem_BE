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

    @Query("SELECT h FROM Hotel h " +
            "JOIN h.location hl " +
            "JOIN h.roomTypes rt " +
            "WHERE hl.province = :province " +
            "AND h.status = 'ACTIVE' " +
            "OR SUM(rt.numberOfRoom) >= :numberOfRoom " +
            "AND SUM(rt.capacityPerRoom * rt.numberOfRoom) >= :numPeople " +
            "GROUP BY h.id, hl.id")
    List<Hotel> findPotentialHotels(
            @Param("province") String province,
            @Param("numPeople") Integer numPeople,
            @Param("numberOfRoom") Integer numberOfRoom,
            Pageable pageable);




    @Query("SELECT h FROM Hotel h " +
            "LEFT JOIN FETCH h.conveniences c " +
            "LEFT JOIN FETCH h.roomTypes rt " +
            "LEFT JOIN FETCH rt.type t " +
            "LEFT JOIN FETCH rt.roomConveniences rc " +
            "LEFT JOIN FETCH rt.roomImages " +
            "LEFT JOIN FETCH h.location l " +
            "WHERE (h.rating = :rating OR :rating IS NULL) AND " +
            "(c.freeBreakfast = :freeBreakfast OR :freeBreakfast IS NULL) AND " +
            "(c.pickUpDropOff = :pickUpDropOff OR :pickUpDropOff IS NULL) AND " +
            "(c.restaurant = :restaurant OR :restaurant IS NULL) AND " +
            "(c.bar = :bar OR :bar IS NULL) AND " +
            "(c.pool = :pool OR :pool IS NULL) AND " +
            "(c.freeInternet = :freeInternet OR :freeInternet IS NULL) AND " +
            "(c.reception24h = :reception24h OR :reception24h IS NULL) AND " +
            "(c.laundry = :laundry OR :laundry IS NULL) AND " +
            "h.status = 'ACTIVE'")
    Page<Hotel> filterHotelWithConvenience(Integer rating,
                                           Boolean freeBreakfast,
                                           Boolean pickUpDropOff,
                                           Boolean restaurant,
                                           Boolean bar,
                                           Boolean pool,
                                           Boolean freeInternet,
                                           Boolean reception24h,
                                           Boolean laundry,
                                           Pageable pageable);

}
