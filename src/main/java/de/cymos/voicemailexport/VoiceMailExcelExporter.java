package de.cymos.voicemailexport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class VoiceMailExcelExporter {

    private static final String[] HEADERS = {"Anrufer", "Datum", "Nachricht"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private static final Logger logger = LogManager.getLogger(VoiceMailExcelExporter.class);

    public static void exportToExcel(List<VoiceMail> voiceMails, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("VoiceMails");

        // Define styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle lightRowStyle = createRowStyle(workbook, IndexedColors.GREY_25_PERCENT);
        CellStyle darkRowStyle = createRowStyle(workbook, IndexedColors.GREY_40_PERCENT);

        // Create header
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill data rows
        for (int i = 0; i < voiceMails.size(); i++) {
            VoiceMail vm = voiceMails.get(i);
            logger.debug("Writing voice mail from {} to excel file.", VoiceMailExport.dateTimeFormat.format(vm.dateSent()));

            Row row = sheet.createRow(i + 1);
            CellStyle rowStyle = (i % 2 == 0) ? lightRowStyle : darkRowStyle;

            Cell callerCell = row.createCell(0);
            callerCell.setCellValue(vm.caller());
            callerCell.setCellStyle(rowStyle);

            Cell dateCell = row.createCell(1);
            dateCell.setCellValue(DATE_FORMAT.format(vm.callDate()));
            dateCell.setCellStyle(rowStyle);

            Cell messageCell = row.createCell(2);
            messageCell.setCellValue(vm.transcript());
            messageCell.setCellStyle(rowStyle);
        }

        // Adjust column widths
        sheet.autoSizeColumn(0); // Anrufer
        sheet.autoSizeColumn(1); // Datum
        sheet.setColumnWidth(2, 21952); // Nachricht column (~100 600 pixels wide)

        // Write to file
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            workbook.write(out);
        }
        workbook.close();
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);

        // Thin borders on all sides
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private static CellStyle createRowStyle(Workbook workbook, IndexedColors bgColor) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);

        // Thin borders on all sides
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

}
