package com.chinhbean.bookinghotel.utils;

import com.chinhbean.bookinghotel.entities.Booking;
import com.chinhbean.bookinghotel.entities.BookingDetails;
import com.chinhbean.bookinghotel.enums.BookingStatus;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExcelFileExporter {

    public static void exportBookingsListToExcel(List<Booking> bookings, HttpServletResponse response, LocalDate startDate, LocalDate endDate) throws IOException, DataNotFoundException {
        List<Booking> filteredBookings = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.PAID)
                .filter(booking -> {
                    LocalDate bookingDate = booking.getBookingDate().toLocalDate();
                    return !bookingDate.isBefore(startDate) && !bookingDate.isAfter(endDate);
                })
                .sorted(Comparator.comparing(Booking::getBookingDate).reversed())
                .toList();

        if (filteredBookings.isEmpty()) {
            throw new DataNotFoundException("No bookings found for the specified criteria.");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Bookings");
        Row headerRow = sheet.createRow(0);

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerCellStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle rightAlignedStyle = workbook.createCellStyle();
        rightAlignedStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Creating header
        String[] columns = {"Booking ID", "Customer Name", "Hotel Name", "Booking Date", "Room Type", "Check-In Date", "Check-Out Date", "Price Per Room", "Number Of Rooms", "Transaction Code", "Total Price"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerCellStyle);
        }

        // Date formatter
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Filling data
        int rowNum = 1;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Booking booking : filteredBookings) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(rightAlignedStyle);
                switch (i) {
                    case 0 -> cell.setCellValue(booking.getBookingId());
                    case 1 -> cell.setCellValue(booking.getUser().getFullName());
                    case 2 -> cell.setCellValue(booking.getHotel().getHotelName());
                    case 3 -> cell.setCellValue(booking.getBookingDate().format(dateTimeFormatter));
                    case 4 -> cell.setCellValue(booking.getBookingDetails().stream()
                            .map(detail -> detail.getRoomType().getRoomTypeName())
                            .distinct()
                            .collect(Collectors.joining(", ")));
                    case 5 -> cell.setCellValue(booking.getCheckInDate().format(dateFormatter));
                    case 6 -> cell.setCellValue(booking.getCheckOutDate().format(dateFormatter));
                    case 7 -> {
                        BigDecimal averagePricePerRoom = booking.getBookingDetails().stream()
                                .map(detail -> detail.getTotalMoney().divide(BigDecimal.valueOf(detail.getNumberOfRooms()), 0, RoundingMode.HALF_UP))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(booking.getBookingDetails().size()), 0, RoundingMode.HALF_UP);
                        cell.setCellValue(averagePricePerRoom.doubleValue() + " VND");
                    }
                    case 8 -> cell.setCellValue(booking.getBookingDetails().stream()
                            .mapToInt(BookingDetails::getNumberOfRooms)
                            .sum());
                    case 9 -> cell.setCellValue(booking.getPaymentTransaction() != null ?
                            booking.getPaymentTransaction().getTransactionCode() : "N/A");
                    case 10 -> {
                        BigDecimal bookingTotalPrice = booking.getTotalPrice().setScale(0, RoundingMode.HALF_UP);
                        cell.setCellValue(bookingTotalPrice.doubleValue() + " VND");
                        totalAmount = totalAmount.add(bookingTotalPrice);
                    }
                }
            }
        }

        Row totalRow = sheet.createRow(rowNum);
        CellStyle totalRowStyle = workbook.createCellStyle();
        totalRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalRowStyle.setAlignment(HorizontalAlignment.RIGHT);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        totalRowStyle.setFont(boldFont);

        Cell totalLabelCell = totalRow.createCell(9);
        totalLabelCell.setCellValue("Total Amount:");
        totalLabelCell.setCellStyle(totalRowStyle);

        Cell totalValueCell = totalRow.createCell(10);
        totalValueCell.setCellValue(totalAmount.setScale(0, RoundingMode.HALF_UP).doubleValue() + " VND");
        totalValueCell.setCellStyle(totalRowStyle);

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Set the content type and write to response
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"bookings.xlsx\"");

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
        } finally {
            workbook.close();
        }
    }
}
