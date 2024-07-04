package com.chinhbean.bookinghotel.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id")
    @JsonBackReference
    private Booking booking;

    @OneToOne
    @JoinColumn(name = "package_id")
    @JsonBackReference
    private ServicePackage servicePackage;

    @Column(nullable = false)
    private String phoneGuest;

    @Column(nullable = false)
    private String nameGuest;

    @Column(nullable = false)
    private String emailGuest;

    @Column(nullable = false)
    private LocalDateTime createDate;

}
