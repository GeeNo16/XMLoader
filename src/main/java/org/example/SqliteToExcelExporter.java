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

    public void exportToExcel(String folderPath, String outputExcelPath, int preset) throws Exception {
        List<String> targetTags = new ArrayList<>(List.of(
                "extract_about_property_land.restrict_records.restrict_record.right_holders.right_holder.legal_entity.entity.resident.name",
                "extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.period.period_info.end_date",
                "extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.period.period_info.start_date"
        ));
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
            keys.addAll(Storage.restrictions.keySet());
            Map<Integer, List<String>> filesRestrictions = new HashMap<>();
            Map<String, Integer> nameColumn = new HashMap<>();
            List<Integer> targetColumns = new ArrayList<>();
            for (String key : keys) {
                Cell cell = headerRow.createCell(colNum++);
                if (Storage.restrictions.containsKey(key)){
                    cell.setCellValue(Storage.restrictions.get(key));
                    nameColumn.put(Storage.restrictions.get(key).trim().toLowerCase(), colNum - 1);
                }
                if (targetTags.contains(key)) {
                    targetColumns.add(colNum - 1);
                    cell.setCellValue(Storage.preset1.get(key));
                }
                else cell.setCellValue(tableColumnMap.get(key));
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
                        if (Objects.equals(tablePath, "extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.restriction_encumbrance_type.value")){
                            if (!value.isEmpty()) {
                                if (value.contains(",")) {
                                    List<String> rests = Arrays.stream(value.split(",")).toList();
                                    filesRestrictions.put(rowNum - 1, rests);
                                } else filesRestrictions.put(rowNum - 1, Collections.singletonList(value));
                            }
                        }
                    }
                    logger.log(" → Успешно: " + dbFile.getName() + "\n", Color.GREEN);
                } catch (SQLException e) {
                    logger.log(" → Ошибка: " + e.getMessage() + "\n", Color.RED);
                }
            }

            if (!filesRestrictions.isEmpty()) {
                for (Map.Entry<Integer, List<String>> entry : filesRestrictions.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        for (String value : entry.getValue()) {
                            String rework;
                            if (value.contains("X")) rework = value.split(" X ")[0].trim().toLowerCase();
                            else rework = value.trim().toLowerCase();
                            if (sheet.getRow(entry.getKey()) != null){
                                Row row = sheet.getRow(entry.getKey());
                                if (nameColumn.get(rework) != null) row.createCell(nameColumn.get(rework)).setCellValue(value);
                            }
                        }
                    }
                }
            }

            if (preset == 1) {
                boolean flag = false;
                for (Row row : sheet) {
                    if (flag) {
                        String fileName = row.getCell(0).getStringCellValue().replace(".db", ".xml");
                        String path = folderPath.replace("databases", fileName);
                        Map<String, String> fits = DubolomParser.parseDubolom(path);
                        if (!fits.isEmpty()) {
                            for (int n : targetColumns) {
                                String cellData = row.getCell(n).getStringCellValue();
                                List<String> info = getStrings(cellData);
                                List<String> filtered = new ArrayList<>();
                                for (String part : info) {
                                    String code = fits.get(part);
                                    if (Objects.equals(code, "022006000000")) {
                                        filtered.add(part);
                                    }
                                }
                                if (filtered.size() > 1) {
                                    row.getCell(n).setCellValue(String.join(",", filtered));
                                } else if (filtered.size() == 1) {
                                    row.getCell(n).setCellValue(filtered.getFirst());
                                } else {
                                    row.getCell(n).setCellValue("");
                                }
                            }
                        }
                    } else flag = true;
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(outputExcelPath)) {
                workbook.write(fileOut);
            }
        }
        logger.log("Готово!", Color.BLACK);
    }

    private static List<String> getStrings(String cellData) {
        List<String> info = new ArrayList<>();
        if (cellData.contains(",")) {
            String[] prepare = cellData.split(",");
            for (String s : prepare) {
                if (s.contains("X")) info.add(s.split(" X ")[0].trim());
                else info.add(s.trim());
            }
        }
        else {
            if (cellData.contains("X")) info.add(cellData.split(" X ")[0].trim());
            else info.add(cellData.trim());
        }
        return info;
    }

    private String extractValues(Connection conn, String tablePath) {
        String sql = "SELECT text_content FROM \"" + tablePath + "\" WHERE text_content IS NOT NULL";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> results = new ArrayList<>();
            int count = 1;
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
            return String.join(", ", results);
        } catch (SQLException e) {
            return "";
        }
    }

}
