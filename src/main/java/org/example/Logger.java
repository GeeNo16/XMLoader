package org.example;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Objects;

public class Logger {
    public VBox container;
    private final TextFlow textFlow;
    public final ScrollPane scrollPane;

    public Logger() {
        textFlow = new TextFlow();
        textFlow.setLineSpacing(4);
        textFlow.getStyleClass().add("logger-textflow");

        scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.getStyleClass().add("logger-scrollpane");

        container = new VBox(scrollPane);
        container.getStyleClass().add("logger-container");

        container.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());

        log("""
                Добро пожаловать в XMLoader
                
                Эта программа экспортирует данные из ваших XML в XLSX
                
                а) Для начала работы, выберите папку, где хранятся XML
                б) Выберите нужные вам колонки или используйте Формат
                в) Ищите готовый XLSX файл в той же папке

                Желаю приятного пользования!
                ----------------------------------------------------------------------
                """, Color.web("#2E4053"));
    }


    public void log(String message, Color color) {
        if (message != null && !message.isEmpty()) {
            Platform.runLater(() -> {
                Text text = new Text(message + "\n");
                text.setFill(color);
                text.setStyle("-fx-font-size: 14px;");

                textFlow.getChildren().add(text);
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
            });
        }
    }
}
