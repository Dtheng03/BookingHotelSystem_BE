package com.chinhbean.bookinghotel.services.pack;

import com.chinhbean.bookinghotel.dtos.DataMailDTO;
import com.chinhbean.bookinghotel.entities.*;
import com.chinhbean.bookinghotel.enums.PackageStatus;
import com.chinhbean.bookinghotel.exceptions.PermissionDenyException;
import com.chinhbean.bookinghotel.repositories.IUserRepository;
import com.chinhbean.bookinghotel.repositories.PaymentTransactionRepository;
import com.chinhbean.bookinghotel.repositories.ServicePackageRepository;
import com.chinhbean.bookinghotel.services.sendmails.IMailService;
import com.chinhbean.bookinghotel.utils.MailTemplate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PackageService implements IPackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final IUserRepository userRepository;
    private final IMailService mailService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Transactional
    @Override
    public List<ServicePackage> getAllPackages() {
        return servicePackageRepository.findAll();
    }

    @Transactional
    @Override
    public ServicePackage getPackageById(Long id) {
        return servicePackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package with ID: " + id + " does not exist."));
    }

    @Transactional
    @Override
    public ServicePackage createPackage(ServicePackage servicePackage) {
        validatePackage(servicePackage);
        return servicePackageRepository.save(servicePackage);
    }

    @Transactional
    @Override

    public ServicePackage updatePackage(Long id, ServicePackage updatedPackage) {
        ServicePackage existingPackage = getPackageById(id);
        existingPackage.setName(updatedPackage.getName());
        existingPackage.setDescription(updatedPackage.getDescription());
        existingPackage.setPrice(updatedPackage.getPrice());
        existingPackage.setDuration(updatedPackage.getDuration());
        return servicePackageRepository.save(existingPackage);
    }

    @Transactional
    @Override
    public void deletePackage(Long id) {
        ServicePackage existingPackage = getPackageById(id);
        servicePackageRepository.delete(existingPackage);
    }

    private void validatePackage(ServicePackage servicePackage) {
        if (servicePackage.getName() == null || servicePackage.getName().isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be empty");
        }

        if (servicePackage.getPrice() == null || servicePackage.getPrice() <= 0) {
            throw new IllegalArgumentException("Package price must be greater than 0");
        }

        if (servicePackage.getDuration() == null) {
            throw new IllegalArgumentException("Package duration must be greater than 0");
        }

        if (servicePackage.getDuration() > 365) {
            throw new IllegalArgumentException("Package duration cannot exceed 365 months");
        }

        if (servicePackage.getDuration() < 30) {
            throw new IllegalArgumentException("Package duration must be at least 30 month");
        }
    }

    @Transactional
    @Override

    public ServicePackage registerPackage(Long packageId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long userId = currentUser.getId();

        //check user and get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        //check package and get package
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));

        //check date > now
        LocalDate now = LocalDate.now();

        //case 30

        if (servicePackage.getDuration() == 30) {
            user.setServicePackage(servicePackage);
            user.setPackageStartDate(now);
            user.setPackageEndDate(now.plusDays(30));
            user.setStatus(PackageStatus.PENDING);
            userRepository.save(user);
        } else {
            //case other (365)
            user.setServicePackage(servicePackage);
            user.setPackageStartDate(now);
            user.setPackageEndDate(now.plusDays(365));
            user.setStatus(PackageStatus.PENDING);
            userRepository.save(user);
        }

        scheduler.schedule(() -> updatePackageIfPending(userId), 300, TimeUnit.SECONDS);

        return servicePackage;
    }

    @Async
    public CompletableFuture<Void> updatePackageIfPending(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && PackageStatus.PENDING.equals(user.getStatus())) {
            user.setServicePackage(null);
            user.setPackageStartDate(null);
            user.setPackageEndDate(null);
            user.setStatus(PackageStatus.INACTIVE);
            userRepository.save(user);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    @Override
    public boolean checkAndHandlePackageExpiration() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        currentUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate now = LocalDate.now();

        if (currentUser.getPackageEndDate() != null && now.isAfter(currentUser.getPackageEndDate())) {
            currentUser.setServicePackage(null);
            currentUser.setPackageStartDate(null);
            currentUser.setPackageEndDate(null);
            userRepository.save(currentUser);
            return true;
        }
        return false;
    }

    @Override
    public void sendMailNotificationForPackagePayment(ServicePackage servicePackage, String email) {
        try {
            DataMailDTO dataMail = new DataMailDTO();

            Optional<PaymentTransaction> paymentTransactionOpt = paymentTransactionRepository.findByEmailGuest(email);
            if (paymentTransactionOpt.isPresent()) {
                PaymentTransaction paymentTransaction = paymentTransactionOpt.get();

                dataMail.setTo(paymentTransaction.getEmailGuest());
                dataMail.setSubject(MailTemplate.SEND_MAIL_SUBJECT.PACKAGE_PAYMENT_SUCCESS);

                NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
                Map<String, Object> props = new HashMap<>();
                props.put("fullName", paymentTransaction.getNameGuest());
                props.put("packageId", servicePackage.getId());
                props.put("packagePrice", currencyFormatter.format(servicePackage.getPrice()));
                props.put("packageDuration", servicePackage.getDuration());
                props.put("description", servicePackage.getDescription());
                dataMail.setProps(props);
                mailService.sendHtmlMail(dataMail, MailTemplate.SEND_MAIL_TEMPLATE.PACKAGE_PAYMENT_SUCCESS_TEMPLATE);
                System.out.println("Email successfully sent to " + paymentTransaction.getEmailGuest());
            } else {
                System.out.println("No payment transaction found for email: " + email);
            }
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }


    @Override
    public ServicePackage findPackageWithPaymentTransactionById(Long packageId) {
        return servicePackageRepository.findPackageWithPaymentTransactionById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));
    }

    @Transactional
    public void updatePackageStatus(Long userId, PackageStatus newStatus) throws PermissionDenyException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getServicePackage() == null) {
            throw new IllegalArgumentException("User does not have an package");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        ServicePackage servicePackage = user.getServicePackage();
        if (servicePackage == null) {
            throw new IllegalArgumentException("User does not have an active package");
        }
        if (Role.ADMIN.equals(currentUser.getRole().getRoleName())) {

            switch (newStatus) {
                case ACTIVE:
                    user.setStatus(newStatus);
                    break;
                case INACTIVE:
                    user.setStatus(newStatus);
                    break;
                case PENDING:
                    user.setStatus(newStatus);
                    break;
                case EXPIRED:
                    user.setStatus(newStatus);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid booking status");
            }
        } else {
            throw new PermissionDenyException("You do not have permission to update the package status.");
        }
        user.setStatus(newStatus);
        userRepository.save(user);
    }



}
