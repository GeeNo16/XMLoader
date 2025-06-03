package org.example;

import javafx.scene.paint.Color;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.util.*;

public class SqliteToExcelExporter {
    private final Map<String, String> tableColumnMap;

    private final Logger logger;

    public SqliteToExcelExporter(Map<String, String> tableColumnMap, Logger logger) {
        this.tableColumnMap = tableColumnMap;
        this.logger = logger;
    }

    public void exportToExcel(String folderPath, String outputExcelPath) throws Exception {
        File folder = new File(folderPath);
        File[] dbFiles = folder.listFiles((dir, name) -> name.endsWith(".db"));

        if (dbFiles == null || dbFiles.length == 0) {
            logger.log("Нет .db файлов в папке: " + folderPath, Color.RED);
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");


            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            int colNum = 0;

            Cell fileNameHeader = headerRow.createCell(colNum++);
            fileNameHeader.setCellValue("Файл");

            List<String> keys = new ArrayList<>(tableColumnMap.keySet());
            for (String key : keys) {
                Cell cell = headerRow.createCell(colNum++);
                cell.setCellValue(tableColumnMap.get(key));
            }

            int count = 1;
            for (File dbFile : dbFiles) {
                String message = String.format("[%d/%d] Обработка файла: %s", count, dbFiles.length, dbFile.getName());
                logger.log(message, Color.BLACK);
                count++;

                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
                    Row dataRow = sheet.createRow(rowNum++);
                    colNum = 0;

                    dataRow.createCell(colNum++).setCellValue(dbFile.getName());

                    for (String tablePath : keys) {
                        String value = extractValues(conn, tablePath);
                        if (value.length() > 32000) dataRow.createCell(colNum++).setCellValue(value.substring(0, 32000));
                        else dataRow.createCell(colNum++).setCellValue(value);
                    }
                    logger.log(" → Успешно: " + dbFile.getName() + "\n", Color.GREEN);
                } catch (SQLException e) {
                    System.err.println("Ошибка подключения к базе: " + dbFile.getName() + " — " + e.getMessage());
                    logger.log(" → Ошибка: " + e.getMessage() + "\n", Color.RED);
                }
                if (!dbFile.delete()) logger.log("Ошибка удаления файла", Color.RED);
            }

            try (FileOutputStream fileOut = new FileOutputStream(outputExcelPath)) {
                workbook.write(fileOut);
            }
        }
        if (!folder.delete()) logger.log("Ошибка удаления папки", Color.RED);
        logger.log("Готово!", Color.BLACK);
    }



    private String extractValues(Connection conn, String tablePath) {
        String sql = "SELECT text_content FROM \"" + tablePath + "\" WHERE text_content IS NOT NULL";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> results = new ArrayList<>();
            int count = 0;
            String previous = "";
            while (rs.next()) {
                String val = rs.getString("text_content");
                if (previous.equals(val)) {
                    count++;
                    String newLast = String.format("%s X %s", val, count);
                    results.removeLast();
                    results.add(newLast);
                    continue;
                }
                if (val != null && !val.isBlank()) {
                    previous = val;
                    results.add(val.trim());
                }
            }
            return String.join(",", results);
        } catch (SQLException e) {
            return "";
        }
    }

}
