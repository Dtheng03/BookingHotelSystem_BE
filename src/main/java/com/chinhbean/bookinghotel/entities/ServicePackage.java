package com.chinhbean.bookinghotel.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "packages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServicePackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private Double price;

    @Column(name = "duration")
    private Integer duration;

    @OneToMany(mappedBy = "servicePackage", fetch = FetchType.LAZY)
    @JsonBackReference("service-package-payment")
    private List<PaymentTransaction> paymentTransaction;

}
