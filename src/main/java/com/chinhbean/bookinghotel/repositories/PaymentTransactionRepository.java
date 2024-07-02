package com.chinhbean.bookinghotel.repositories;

import com.chinhbean.bookinghotel.entities.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
}

