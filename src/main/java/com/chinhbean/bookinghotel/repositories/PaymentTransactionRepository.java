package com.chinhbean.bookinghotel.repositories;

import com.chinhbean.bookinghotel.entities.PaymentTransaction;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM PaymentTransaction pt WHERE pt.emailGuest = :emailGuest")
    void deleteByEmailGuest(String emailGuest);

}

