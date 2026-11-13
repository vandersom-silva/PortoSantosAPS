package br.edu.unirv.portos.view;

import br.edu.unirv.portos.controller.PortController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainView extends BorderPane {
    private final PortController controller;
    private final VBox sidebar = new VBox(8);
    private final BorderPane workspace = new BorderPane();
    private final Label pageTitle = Ui.label("Lineup marítimo", "top-title");
    private final Label pageSubtitle = Ui.label("Visão operacional do Porto de Santos", "muted-text");
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private LineupView lineupView;

    public MainView(PortController controller) {
        this.controller = controller;
        getStyleClass().add("app-root");
        setLeft(buildSidebar());
        setCenter(workspace);
        show("lineup");
    }
    private Node buildSidebar() {
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(22, 14, 18, 14));
        sidebar.setPrefWidth(238); sidebar.setMinWidth(238);
        VBox brand = new VBox(3, Ui.label("GÊMEO DIGITAL", "brand-kicker"), Ui.label("PORTO DE SANTOS", "brand-title"), Ui.label("APS  •  FIPS  |  v1.0", "brand-subtitle"));
        brand.setPadding(new Insets(4, 8, 22, 8));
        sidebar.getChildren().addAll(brand, Ui.label("OPERAÇÃO MARÍTIMA", "nav-section"));
        addNav("lineup", "◫", "Lineup", true);
        addNav("berths", "▤", "Berços", false);
        addNav("cargo", "▦", "Cargas", false);
        addNav("vessels", "◇", "Navios", false);
        addNav("tides", "≈", "Marés", false);
        addNav("schedules", "＋", "Atracações", false);
        addNav("history", "≡", "Histórico", false);
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox footer = new VBox(7,
                Ui.label("AMBIENTE DE AVALIAÇÃO", "nav-section"),
                Ui.badge("DADOS MOCADOS • EM MEMÓRIA", "#4ade80"),
                Ui.label("Módulo marítimo RF001–RF012", "sidebar-footnote"));
        footer.setPadding(new Insets(15, 8, 4, 8));
        sidebar.getChildren().addAll(spacer, footer);
        return sidebar;
    }
    private void addNav(String key, String icon, String text, boolean active) {
        Button b = new Button(icon + "   " + text);
        b.getStyleClass().add("nav-button");
        if (active) b.getStyleClass().add("nav-active");
        b.setMaxWidth(Double.MAX_VALUE); b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> show(key));
        navButtons.put(key, b); sidebar.getChildren().add(b);
    }
    private void show(String key) {
        navButtons.forEach((k,b) -> { b.getStyleClass().remove("nav-active"); if(k.equals(key)) b.getStyleClass().add("nav-active"); });
        Node screen;
        String title;
        String subtitle;
        switch (key) {
            case "berths" -> { screen = new BerthsView(controller); title = "Berços de atracação"; subtitle = "Cadastro, edição, exclusão e auditoria dos 60 berços"; }
            case "cargo" -> { screen = new CargoView(controller); title = "Catálogo de cargas"; subtitle = "Commodities, perfis portuários e produtividade operacional"; }
            case "vessels" -> { screen = new VesselsView(controller); title = "Navios mercantes"; subtitle = "Cadastro e consulta da frota registrada"; }
            case "tides" -> { screen = new TidesView(controller); title = "Tábuas de marés"; subtitle = "Monitoramento astronômico em intervalos de 30 minutos"; }
            case "schedules" -> { screen = new SchedulesView(controller); title = "Programação de atracações"; subtitle = "Validação de calado, maré baixa e conflito de berço"; }
            case "history" -> { screen = new HistoryView(controller); title = "Histórico de atracações"; subtitle = "Auditoria cronológica e consulta das escalas do mês"; }
            default -> { if (lineupView == null) lineupView = new LineupView(controller); screen = lineupView; title = "Lineup marítimo"; subtitle = "Visão panorâmica, indicadores globais e ocupação dos berços"; }
        }
        pageTitle.setText(title); pageSubtitle.setText(subtitle);
        VBox header = new VBox(4, pageTitle, pageSubtitle);
        header.getStyleClass().add("page-header"); header.setPadding(new Insets(20, 26, 16, 26));
        ScrollPane scroll = new ScrollPane(screen);
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("workspace-scroll");
        workspace.setTop(header); workspace.setCenter(scroll);
    }
}
