package br.edu.unirv.portos.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Callback;
import java.util.function.Consumer;

public final class Ui {
    private Ui() { }
    public static Label label(String text, String style) {
        Label label = new Label(text);
        if (style != null && !style.isBlank()) label.getStyleClass().add(style);
        return label;
    }
    public static Label fieldLabel(String text) { return label(text, "field-label"); }
    public static VBox field(String title, Node control) {
        VBox box = new VBox(6, fieldLabel(title), control);
        VBox.setVgrow(control, Priority.NEVER);
        return box;
    }
    public static TextField textField(String prompt) {
        TextField f = new TextField(); f.setPromptText(prompt); f.getStyleClass().add("input"); return f;
    }
    public static Button button(String text, String style, Runnable action) {
        Button b = new Button(text); if (style != null && !style.isBlank()) b.getStyleClass().addAll(style.trim().split("\\s+"));
        b.setOnAction(e -> action.run()); return b;
    }
    public static VBox card(String style) {
        VBox b = new VBox(10); b.getStyleClass().add("surface-card");
        if (style != null && !style.isBlank()) b.getStyleClass().addAll(style.trim().split("\\s+"));
        b.setPadding(new Insets(16)); return b;
    }
    public static HBox sectionTitle(String title, String subtitle) {
        VBox texts = new VBox(3, label(title, "section-title"), label(subtitle, "muted-text"));
        HBox row = new HBox(texts); row.setAlignment(Pos.CENTER_LEFT); return row;
    }
    public static <T> TableColumn<T, Void> actionColumn(Consumer<T> edit, Consumer<T> delete) {
        return actionColumn("Editar", edit, delete);
    }
    public static <T> TableColumn<T, Void> actionColumn(String editLabel, Consumer<T> edit, Consumer<T> delete) {
        TableColumn<T, Void> column = new TableColumn<>("Ações");
        column.setMinWidth(124); column.setMaxWidth(142);
        column.setCellFactory(new Callback<>() {
            @Override public TableCell<T, Void> call(TableColumn<T, Void> param) {
                return new TableCell<>() {
                    private final Button editButton = new Button(editLabel);
                    private final Button deleteButton = new Button("Excluir");
                    private final HBox box = new HBox(5, editButton, deleteButton);
                    {
                        editButton.getStyleClass().add("table-action");
                        deleteButton.getStyleClass().addAll("table-action", "table-danger");
                        box.setAlignment(Pos.CENTER);
                        editButton.setOnAction(e -> { T item = getTableView().getItems().get(getIndex()); edit.accept(item); });
                        deleteButton.setOnAction(e -> { T item = getTableView().getItems().get(getIndex()); delete.accept(item); });
                    }
                    @Override protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty); setGraphic(empty ? null : box);
                    }
                };
            }
        });
        return column;
    }
    public static void configureTable(TableView<?> table) {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(label("Nenhum registro encontrado.", "muted-text"));
        table.setFixedCellSize(38);
    }
    public static Label badge(String text, String color) {
        Label l = label(text, "badge");
        l.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + ";");
        return l;
    }
    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle("Não foi possível concluir"); alert.setHeaderText("Verifique os dados informados"); alert.setContentText(message); alert.showAndWait();
    }
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }
    public static String money(double value) { return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,.0f", value); }
    public static String decimal(double value) { return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", value); }
}
