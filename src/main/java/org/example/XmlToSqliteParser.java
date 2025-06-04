package org.example;

import javafx.scene.paint.Color;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class XmlToSqliteParser {
    private Connection connection;
    private final Set<String> createdTables = new HashSet<>();
    private final Map<String, Integer> lastInsertedIds = new HashMap<>();
    private final HashSet<String> namespace = new HashSet<>();

    private final Logger logger;

    public XmlToSqliteParser(Logger logger) {
        this.logger = logger;
    }

    public void parseXmlFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            logger.log("Папка не найдена: " + folderPath, Color.RED);
            return;
        }

        File[] xmlFiles = folder.listFiles((e, name) -> name.toLowerCase().endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) {
            logger.log("XML-файлы не найдены в папке: " + folderPath, Color.RED);
            return;
        }

        File dbDir = new File(folder, "databases");
        if (!dbDir.exists()) {
            boolean created = dbDir.mkdir();
            if (!created) {
                logger.log("Не удалось создать папку для баз данных: " + dbDir.getAbsolutePath(), Color.RED);
                return;
            }
        }

        logger.log("Найдено " + xmlFiles.length + " XML-файлов. Начинается обработка...\n", Color.BLACK);

        for (int i = 0; i < xmlFiles.length; i++) {
            File xmlFile = xmlFiles[i];
            String xmlName = xmlFile.getName();
            String dbFileName = xmlName.replace(".xml", ".db");
            File dbFile = new File(dbDir, dbFileName);

            String message = String.format("[%d/%d] Обработка файла: %s", i + 1, xmlFiles.length, xmlName);
            logger.log(message, Color.BLACK);

            try {
                parseSingleXmlToDatabase(xmlFile.getAbsolutePath(), dbFile.getAbsolutePath());
                logger.log(" → Успешно: " + dbFileName + "\n", Color.GREEN);
            } catch (Exception e) {
                logger.log(" → Ошибка: " + e.getMessage() + "\n", Color.RED);
            }
        }

        logger.log("Обработка завершена", Color.BLACK);
    }

    public void parseSingleXmlToDatabase(String xmlFilePath, String dbPath) throws Exception {
        File xmlFile = new File(xmlFilePath);
        if (!xmlFile.exists()) throw new FileNotFoundException("Файл не найден: " + xmlFilePath);

        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                try {
                    DatabaseMetaData metaData = connection.getMetaData();

                    ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

                    while (tables.next()) {
                        String tableName = tables.getString("TABLE_NAME");
                        namespace.add(tableName);
                    }

                } catch (SQLException e) {
                    logger.log("Ошибка при работе с базой данных: " + e.getMessage(), Color.RED);
                }
            } catch (SQLException e) {
                logger.log("Ошибка при закрытии соединения: " + e.getMessage(), Color.RED);
            }
            return;
        }

        createdTables.clear();
        lastInsertedIds.clear();
        initializeDatabase(dbPath);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();

            processNode(document.getDocumentElement(), null, "");
            connection.commit();
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    private void initializeDatabase(String dbPath) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Драйвер SQLite не найден", e);
        }
    }

    private void processNode(Node node, String parentPath, String currentPath) throws SQLException {
        if (node.getNodeType() != Node.ELEMENT_NODE) return;

        String nodeName = normalizeName(node.getNodeName());
        String fullPath = currentPath.isEmpty() ? nodeName : currentPath + "." + nodeName;

        if (!createdTables.contains(fullPath)) {
            createTable(fullPath, parentPath);
            createdTables.add(fullPath);
        }

        int insertedId = insertNode(fullPath, parentPath, node);
        lastInsertedIds.put(fullPath, insertedId);

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                processNode(child, fullPath, fullPath);
            }
        }
    }

    private void createTable(String tableName, String parentTable) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS \"").append(tableName).append("\" (")
                .append("id INTEGER PRIMARY KEY AUTOINCREMENT");

        if (parentTable != null) {
            sql.append(", parent_id INTEGER");
        }

        sql.append(", text_content TEXT");
        sql.append(")");
        executeSQL(sql.toString());
        namespace.add(tableName);
    }

    private int insertNode(String tableName, String parentTable, Node node) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (parentTable != null) {
            columns.add("parent_id");
            placeholders.add("?");
            values.add(lastInsertedIds.getOrDefault(parentTable, null));
        }

        String text = node.getTextContent().trim();
        if (!text.isEmpty()) {
            columns.add("text_content");
            placeholders.add("?");
            values.add(text);
        }

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String colName = "attr_" + normalizeName(attr.getNodeName());
            addColumnIfNotExists(tableName, colName);
            columns.add(colName);
            placeholders.add("?");
            values.add(attr.getNodeValue());
        }

        String sql = "INSERT INTO \"" + tableName + "\" (" +
                String.join(", ", columns) + ") VALUES (" +
                String.join(", ", placeholders) + ")";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private void addColumnIfNotExists(String table, String column) throws SQLException {
        try {
            executeSQL("ALTER TABLE \"" + table + "\" ADD COLUMN " + column + " TEXT");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column name")) {
                throw e;
            }
        }
    }

    private void executeSQL(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private String normalizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }

    public HashSet<String> getNamespace() {
        return namespace;
    }

    public void clear(String dbFolderPath) {
        File folder = new File(dbFolderPath);
        File[] dbFiles = folder.listFiles((dir, name) -> name.endsWith(".db"));
        System.out.println("Done");
        if (dbFiles == null) return;
        for (File dbFile : dbFiles) {
            if (!dbFile.delete()) logger.log("Ошибка удаления файла: " + dbFile.getAbsolutePath(), Color.RED);
        }
        if (!folder.delete()) logger.log("Ошибка удаления папки: " + folder.getAbsolutePath(), Color.RED);
    }
}