package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDateTime;

public class App extends Application {
    private final TreeView<String> tableTreeView = new TreeView<>(new CheckBoxTreeItem<>("root"));

    private File selectedFolder;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Logger logger = new Logger();
        Label title = new Label("XMLoader");
        Button folderButton = new Button("Выбрать папку");
        Button exportButton = new Button("Экспорт колонок в Excel");
        exportButton.setDisable(true);
        Button preset1Button = new Button("Экспорт в Формате 1");
        preset1Button.setDisable(true);
        TextField chosenFolder = new TextField();
        Separator separator = new Separator(Orientation.VERTICAL);

        folderButton.setOnAction(e -> chooseFolder(stage, chosenFolder, logger, exportButton, preset1Button));
        exportButton.setOnAction(e -> runExportScriptAsync(0, logger, exportButton, preset1Button));
        preset1Button.setOnAction(e -> runExportScriptAsync(1, logger, exportButton, preset1Button));
        chosenFolder.setPrefColumnCount(45);
        title.getStyleClass().add("app-title");

        tableTreeView.setShowRoot(false);
        tableTreeView.setCellFactory(CheckBoxTreeCell.forTreeView());

        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setGridLinesVisible(false);
        layout.setPadding(new Insets(20));
        layout.add(title, 0, 0, 2, 1);
        layout.add(separator, 2, 0, 1, 5);
        layout.add(folderButton, 0, 1);
        layout.add(chosenFolder, 0, 2, 2, 1);
        layout.add(tableTreeView, 0, 3, 2, 1);
        layout.add(exportButton, 0, 4);
        layout.add(preset1Button, 1, 4);
        layout.add(logger.scrollPane, 3, 0, 1, 5);


        Scene scene = new Scene(layout, 1000, 700);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> runClose(chosenFolder, logger));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());
        stage.setTitle("XMLoader");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png")));
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
    }

    private void chooseFolder(Stage stage, TextField chosenFolder, Logger logger, Button btn1, Button btn2) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Выберите папку с XML-файлами");
        selectedFolder = dirChooser.showDialog(stage);
        if (selectedFolder != null) chosenFolder.setText(selectedFolder.getAbsolutePath());
        runParseScriptAsync(logger, btn1, btn2);
    }

    private void runClose(TextField chosenFolder, Logger logger) {
        String folder = chosenFolder.getText() + "/databases";
        if (!new File(folder).exists()) return;
        XmlToSqliteParser parser = new XmlToSqliteParser(logger);
        System.out.println("Done");
        parser.clear(folder);
    }

    private void runParseScriptAsync(Logger logger, Button btn1, Button btn2) {
        if (selectedFolder == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                XmlToSqliteParser parser = new XmlToSqliteParser(logger);
                parser.parseXmlFolder(selectedFolder.getAbsolutePath());

                Platform.runLater(() -> {
                    CheckBoxTreeItem<String> root = TableSelector.buildTree(parser.getNamespace().stream().toList());
                    tableTreeView.setRoot(root);
                });

                return null;
            }

            @Override
            protected void succeeded() {
                btn1.setDisable(false);
                btn2.setDisable(false);
            }

            @Override
            protected void failed() {
                btn1.setDisable(false);
                btn2.setDisable(false);
            }
        };

        new Thread(task).start();
    }


    private void runExportScriptAsync(int preset, Logger logger, Button btn1, Button btn2) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd;HH-mm-ss");
                String now = LocalDateTime.now().format(formatter);
                btn1.setDisable(true);
                btn2.setDisable(true);
                CheckBoxTreeItem<String> root = (CheckBoxTreeItem<String>) tableTreeView.getRoot();
                HashMap<String, String> columns = new HashMap<>();

                if (preset == 0) {
                    List<String> selected = TableSelector.getSelectedTables(root);
                    if (selected.isEmpty()) {
                        logger.log("Нужно выбрать как минимум одну колонку", Color.RED);
                        return null;
                    }
                    for (String s : selected) {
                        columns.put(s, s);
                    }
                } else if (preset == 1) {
                    columns.putAll(Storage.preset1);
                }

                SqliteToExcelExporter exporter = new SqliteToExcelExporter(columns, logger);
                String inputPath = selectedFolder.getAbsolutePath() + "/databases";
                String outputPath = selectedFolder.getAbsolutePath() + "/output" + now + ".xlsx";

                try {
                    exporter.exportToExcel(inputPath, outputPath, preset);
                } catch (Exception e) {
                    logger.log("Ошибка экспорта: " + e.getMessage(), Color.RED);
                }

                return null;
            }

            @Override
            protected void succeeded() {
                btn1.setDisable(false);
                btn2.setDisable(false);
            }

            @Override
            protected void failed() {
                btn1.setDisable(false);
                btn2.setDisable(false);
            }
        };

        new Thread(task).start();
    }
}