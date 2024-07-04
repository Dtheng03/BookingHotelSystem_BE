package com.chinhbean.bookinghotel.utils;

import com.chinhbean.bookinghotel.entities.Booking;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ExcelFileExporter {

    public static void exportBookingsListToExcel(List<Booking> bookings, HttpServletResponse response) throws IOException {
        // Resolve the home directory and create the desired path
        String projectDirectory = System.getProperty("user.dir");
        Path directoryPath = Paths.get(projectDirectory, "src", "main", "resources", "exports");
        Files.createDirectories(directoryPath); // Ensure the directory exists

        // Define the file path within the created directory
        Path filePath = directoryPath.resolve("bookings.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Bookings");
        Row headerRow = sheet.createRow(0);
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Creating header
        String[] columns = {"Booking ID", "Customer Name", "Hotel Name", "Booking Date", "Status"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerCellStyle);
        }

        // Filling data
        int rowNum = 1;
        for (Booking booking : bookings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(booking.getBookingId());
            row.createCell(1).setCellValue(booking.getUser().getName());
            row.createCell(2).setCellValue(booking.getHotel().getHotelName());
            row.createCell(3).setCellValue(booking.getBookingDate().toString());
            row.createCell(4).setCellValue(booking.getStatus().toString());
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Save the workbook to the server's filesystem
        try (FileOutputStream fileOut = new FileOutputStream(filePath.toString())) {
            workbook.write(fileOut);
        }

        // Set the content type to indicate a response in Excel format
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=bookings.xlsx");

        // Write the workbook to the servlet output stream
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
        } finally {
            workbook.close();
        }
    }
}
