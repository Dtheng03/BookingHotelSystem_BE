package com.chinhbean.bookinghotel.utils;

import com.chinhbean.bookinghotel.entities.Booking;
import com.chinhbean.bookinghotel.entities.BookingDetails;
import com.chinhbean.bookinghotel.enums.BookingStatus;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ExcelFileExporter {

    public static void exportBookingsListToExcel(List<Booking> bookings, HttpServletResponse response) throws IOException {

        List<Booking> activeBookings = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.PAID)
                .toList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Bookings");
        Row headerRow = sheet.createRow(0);
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

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
        for (Booking booking : activeBookings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(booking.getBookingId());
            row.createCell(1).setCellValue(booking.getUser().getFullName());
            row.createCell(2).setCellValue(booking.getHotel().getHotelName());
            row.createCell(3).setCellValue(booking.getBookingDate().format(dateTimeFormatter));
            String roomTypes = booking.getBookingDetails().stream()
                    .map(detail -> detail.getRoomType().getRoomTypeName())
                    .distinct()
                    .collect(Collectors.joining(", "));
            row.createCell(4).setCellValue(roomTypes);
            row.createCell(5).setCellValue(booking.getCheckInDate().format(dateFormatter));
            row.createCell(6).setCellValue(booking.getCheckOutDate().format(dateFormatter));
            BigDecimal averagePricePerRoom = booking.getBookingDetails().stream()
                    .map(detail -> detail.getTotalMoney().divide(BigDecimal.valueOf(detail.getNumberOfRooms()), 0, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(booking.getBookingDetails().size()), 0, RoundingMode.HALF_UP);
            int totalNumberOfRooms = booking.getBookingDetails().stream()
                    .mapToInt(BookingDetails::getNumberOfRooms)
                    .sum();
            row.createCell(7).setCellValue(averagePricePerRoom + " VND");
            row.createCell(8).setCellValue(totalNumberOfRooms);
            BigDecimal bookingTotalPrice = booking.getTotalPrice().setScale(0, RoundingMode.HALF_UP);
            row.createCell(9).setCellValue(bookingTotalPrice + " VND");
            totalAmount = totalAmount.add(bookingTotalPrice);
        }

        Row totalRow = sheet.createRow(rowNum);
        CellStyle totalRowStyle = workbook.createCellStyle();
        totalRowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        totalRowStyle.setFont(boldFont);

        Cell totalLabelCell = totalRow.createCell(8);
        totalLabelCell.setCellValue("Total Amount:");
        totalLabelCell.setCellStyle(totalRowStyle);

        Cell totalValueCell = totalRow.createCell(9);
        totalValueCell.setCellValue(totalAmount.setScale(0, RoundingMode.HALF_UP) + " VND");
        totalValueCell.setCellStyle(totalRowStyle);

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Set the content type to indicate a response in Excel format
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"bookings.xlsx\"");

        // Write the workbook to the servlet output stream
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
        } finally {
            workbook.close();
        }
    }
}
