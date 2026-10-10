package com.loanmust.loan.reports;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Renders a {@link ReportTable} to .xlsx with typed number and date cells. */
@Component
public class ExcelExporter {

    public byte[] toXlsx(ReportTable table) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = table.title() == null ? "Report" : table.title().replaceAll("[\\\\/?*\\[\\]:]", " ");
            Sheet sheet = wb.createSheet(sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName);

            CreationHelper helper = wb.getCreationHelper();
            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle head = wb.createCellStyle();
            head.setFont(bold);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(helper.createDataFormat().getFormat("dd-mmm-yyyy"));
            CellStyle dateTime = wb.createCellStyle();
            dateTime.setDataFormat(helper.createDataFormat().getFormat("dd-mmm-yyyy hh:mm"));

            Row hr = sheet.createRow(0);
            for (int c = 0; c < table.headers().size(); c++) {
                Cell cell = hr.createCell(c);
                cell.setCellValue(table.headers().get(c));
                cell.setCellStyle(head);
            }
            int r = 1;
            for (java.util.List<Object> rowData : table.rows()) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < rowData.size(); c++) {
                    Object v = rowData.get(c);
                    Cell cell = row.createCell(c);
                    if (v == null) {
                        cell.setBlank();
                    } else if (v instanceof BigDecimal bd) {
                        cell.setCellValue(bd.doubleValue());
                        cell.setCellStyle(money);
                    } else if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else if (v instanceof LocalDateTime dt) {
                        cell.setCellValue(dt);
                        cell.setCellStyle(dateTime);
                    } else if (v instanceof LocalDate d) {
                        cell.setCellValue(d);
                        cell.setCellStyle(date);
                    } else {
                        cell.setCellValue(String.valueOf(v));
                    }
                }
            }
            for (int c = 0; c < table.headers().size(); c++) {
                sheet.setColumnWidth(c, Math.min(60, Math.max(12, table.headers().get(c).length() + 4)) * 256);
            }
            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ResponseEntity<ByteArrayResource> download(ReportTable table, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(toXlsx(table)));
    }
}
