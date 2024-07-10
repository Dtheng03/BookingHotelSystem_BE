package com.chinhbean.bookinghotel.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "hotel_location")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class HotelLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    @NotNull(message = "Latitude is required")
    private Double latitude;

    @Column(nullable = false)
    @NotNull(message = "Longitude is required")
    private Double longitude;

}
