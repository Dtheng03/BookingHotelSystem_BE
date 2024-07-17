package com.chinhbean.bookinghotel.repositories;

import com.chinhbean.bookinghotel.entities.ServicePackage;
import com.chinhbean.bookinghotel.enums.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface IServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    @Query("SELECT s FROM ServicePackage s LEFT JOIN FETCH s.paymentTransaction pt WHERE s.id = :packageId")
    Optional<ServicePackage> findPackageWithPaymentTransactionById(Long packageId);

}
