package com.chinhbean.bookinghotel.controllers;

import com.chinhbean.bookinghotel.services.dashboard.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("api/v1/dashboards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final IDashboardService dashboardService;
    @GetMapping("/total-revenue")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public BigDecimal getTotalRevenue() {
        return dashboardService.getTotalRevenueFromPaidBookings();
    }


//    @GetMapping("/total-revenue-by-package")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
//    public Map<Long, BigDecimal> getTotalRevenueByPackage() {
//        return dashboardService.getTotalRevenueByPackage();
//    }
    @GetMapping("/total-revenue-by-package")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public Map<Long, BigDecimal> getTotalRevenueByPackage() {
        return dashboardService.getTotalRevenueByPackage();
    }

}
