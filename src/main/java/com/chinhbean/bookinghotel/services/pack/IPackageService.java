package com.chinhbean.bookinghotel.services.pack;

import com.chinhbean.bookinghotel.entities.ServicePackage;
import com.chinhbean.bookinghotel.enums.PackageStatus;
import com.chinhbean.bookinghotel.exceptions.PermissionDenyException;

import java.util.List;

public interface IPackageService {
    List<ServicePackage> getAllPackages();

    ServicePackage getPackageById(Long id);

    ServicePackage createPackage(ServicePackage servicePackage);

    ServicePackage updatePackage(Long id, ServicePackage servicePackage);

    void deletePackage(Long id);

    ServicePackage registerPackage(Long packageId);

    boolean checkAndHandlePackageExpiration();

    void sendMailNotificationForPackagePayment(ServicePackage servicePackage, String email);

    ServicePackage findPackageWithPaymentTransactionById(Long packageId);

    void updatePackageStatus(Long userId, PackageStatus newStatus) throws PermissionDenyException;
}
