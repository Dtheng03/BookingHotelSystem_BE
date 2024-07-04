package com.chinhbean.bookinghotel.entities;

import jakarta.persistence.*;
import lombok.*;

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
    private Integer duration; // Thay đổi thành Integer để biểu thị số tháng

}
