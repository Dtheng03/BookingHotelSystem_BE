package com.chinhbean.bookinghotel.services.pack;

import com.chinhbean.bookinghotel.entities.ServicePackage;
import com.chinhbean.bookinghotel.entities.User;
import com.chinhbean.bookinghotel.repositories.IUserRepository;
import com.chinhbean.bookinghotel.repositories.ServicePackageRepository;
import com.chinhbean.bookinghotel.services.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageService implements IPackageService {

    private final ServicePackageRepository servicePackageRepository;
    private final IUserRepository userRepository;

    public List<ServicePackage> getAllPackages() {
        return servicePackageRepository.findAll();
    }

    public ServicePackage getPackageById(Long id) {
        return servicePackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Package with ID: " + id + " does not exist."));
    }

    public ServicePackage createPackage(ServicePackage servicePackage) {
        validatePackage(servicePackage);
        return servicePackageRepository.save(servicePackage);
    }

    public ServicePackage updatePackage(Long id, ServicePackage updatedPackage) {
        ServicePackage existingPackage = getPackageById(id);
        existingPackage.setName(updatedPackage.getName());
        existingPackage.setDescription(updatedPackage.getDescription());
        existingPackage.setPrice(updatedPackage.getPrice());
            existingPackage.setDuration(updatedPackage.getDuration());
        return servicePackageRepository.save(existingPackage);
    }

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


        if (servicePackage.getDuration() > 12) {
            throw new IllegalArgumentException("Package duration cannot exceed 12 months");
        }

        if (servicePackage.getDuration() < 1) {
            throw new IllegalArgumentException("Package duration must be at least 1 month");
        }

    }

    public void registerPackage(Long packageId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long userId = currentUser.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));

        LocalDate now = LocalDate.now();
        if(servicePackage.getDuration() == 30){
            if(now.isAfter(user.getPackageEndDate())){
                user.setServicePackage(servicePackage);
                user.setPackageStartDate(now);
                user.setPackageEndDate(now.plusDays(30)); // Assuming package lasts for 30 days
                userRepository.save(user);
            }
        } else {
            user.setServicePackage(servicePackage);
            user.setPackageStartDate(now);
            user.setPackageEndDate(now.plusDays(365)); // Assuming package lasts for 30 days
            userRepository.save(user);
        }
    }

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
}
