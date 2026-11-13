package br.edu.unirv.portos.view;

import br.edu.unirv.portos.controller.PortController;
import br.edu.unirv.portos.model.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.Cursor;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.Tooltip;
import java.util.*;
import java.util.stream.Collectors;

public final class LineupView extends VBox {
    private static final double TRACK_WIDTH = 840;
    private static final int PAGE_SIZE = 5;
    private final PortController controller;
    private final EnumSet<CargoProfile> selectedProfiles = EnumSet.allOf(CargoProfile.class);
    private final VBox berthCards = new VBox(10);
    private final HBox kpis = new HBox(12);
    private final Label selectedBanner = Ui.label("Selecione uma barra de navio para consultar carga e descarga.", "selection-banner");
    private final Label pageInfo = Ui.label("Página 1 de 12", "muted-text");
    private final HBox pagination = new HBox(5);
    private int currentPage = 1;
    private List<Berth> visibleBerths = List.of();
    private Set<Integer> criticalDays = Set.of();
    private Label tideBadge;
    private Canvas timelineAxis;
    private final Map<Canvas, Berth> trackBerths = new IdentityHashMap<>();
    private ShipSchedule draggingSchedule;
    private Canvas dragSourceTrack;
    private Canvas dragTargetTrack;
    private int dragPreviewStartDay = 1;
    private double dragPreviewX;
    private double dragPointerOffsetDays;
    private boolean dragPreviewValid;
    private boolean dragMoved;
    private String dragPreviewMessage = "";

    public LineupView(PortController controller) {
        this.controller = controller;
        setSpacing(14); setPadding(new Insets(2, 26, 26, 26));
        getChildren().addAll(buildMonthStrip(), buildKpis(), buildFilters(), buildTimeline(), selectedBanner, buildPagination());
        controller.berths().addListener((ListChangeListener<Berth>) c -> refresh());
        controller.schedules().addListener((ListChangeListener<ShipSchedule>) c -> refresh());
        controller.vessels().addListener((ListChangeListener<RegisteredVessel>) c -> refresh());
        controller.tides("2026-11").addListener((ListChangeListener<TideMeasurement>) c -> refresh());
        refresh();
    }
    private HBox buildMonthStrip() {
        HBox row = new HBox(10, Ui.badge("LINEUP ATIVO", "#0284c7"), Ui.label("Novembro de 2026", "month-title"), new Region(), Ui.label("Régua mensal • 30 dias", "muted-text"));
        HBox.setHgrow(row.getChildren().get(2), Priority.ALWAYS); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    private HBox buildKpis() {
        kpis.setAlignment(Pos.CENTER_LEFT); kpis.setFillHeight(true); return kpis;
    }
    private VBox buildFilters() {
        VBox box = Ui.card("filter-card");
        box.getChildren().addAll(Ui.label("Filtros por perfil de carga", "section-title"), Ui.label("Desmarque um perfil para ocultar os berços correspondentes e recalcular os indicadores.", "muted-text"));
        FlowPane checks = new FlowPane(8, 8); checks.setPrefWrapLength(900);
        for (CargoProfile profile : CargoProfile.values()) {
            CheckBox cb = new CheckBox(profile.getLabel()); cb.setSelected(true); cb.getStyleClass().add("filter-check");
            cb.selectedProperty().addListener((obs, old, value) -> { if(value) selectedProfiles.add(profile); else selectedProfiles.remove(profile); currentPage=1; refresh(); });
            checks.getChildren().add(cb);
        }
        box.getChildren().add(checks);
        return box;
    }
    private VBox buildTimeline() {
        VBox box = new VBox(8); box.getStyleClass().add("timeline-section");
        tideBadge = Ui.badge("Maré baixa: —", "#dc2626");
        HBox title = new HBox(10, Ui.label("Berços e janelas de atracação • arraste os navios para mover", "section-title"), new Region(), tideBadge);
        HBox.setHgrow(title.getChildren().get(1), Priority.ALWAYS); title.setAlignment(Pos.CENTER_LEFT);
        timelineAxis = new Canvas(TRACK_WIDTH, 38); drawAxis(timelineAxis.getGraphicsContext2D(), TRACK_WIDTH);
        HBox axisRow = new HBox(12, Ui.label("BERÇO / TERMINAL", "axis-berth"), timelineAxis); axisRow.setAlignment(Pos.CENTER_LEFT);
        ScrollPane chartsScroll = new ScrollPane(berthCards); chartsScroll.setFitToWidth(true); chartsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chartsScroll.setPrefViewportHeight(345); chartsScroll.setMinViewportHeight(250); chartsScroll.getStyleClass().add("chart-scroll");
        VBox legend = new VBox(4, Ui.label("STATUS DAS ATRACAÇÕES", "tiny-label"));
        HBox legendRow = new HBox(14,
                Ui.badge("Operando / Prioridade", "#e74c3c"), Ui.badge("Programado", "#e67e22"), Ui.badge("Confirmado", "#2ecc71"), Ui.badge("Prévia válida", "#16a34a"), Ui.badge("Destino bloqueado", "#dc2626"));
        legend.getChildren().add(legendRow);
        box.getChildren().addAll(title, Ui.label("Bloqueio RN002: navios com calado ≥ 13,5 m não podem ocupar os dias marcados em vermelho (maré ≤ 0,4 m).", "muted-text"), axisRow, chartsScroll, legend);
        return box;
    }
    private HBox buildPagination() {
        pagination.setAlignment(Pos.CENTER_RIGHT);
        return pagination;
    }
    private void refresh() {
        criticalDays = controller.tides("2026-11").stream().filter(TideMeasurement::isCritical)
                .map(t -> t.getMeasuredAt().getDayOfMonth()).collect(Collectors.toCollection(TreeSet::new));
        if (tideBadge != null) tideBadge.setText(criticalDays.isEmpty() ? "Sem maré baixa crítica" :
                "Maré baixa: dias " + criticalDays.stream().map(d -> String.format("%02d", d)).collect(Collectors.joining(", ")));
        if (timelineAxis != null) drawAxis(timelineAxis.getGraphicsContext2D(), TRACK_WIDTH);
        List<Berth> filtered = controller.berths().stream().filter(b -> selectedProfiles.contains(b.getCargoProfile())).toList();
        visibleBerths = filtered;
        int pages = Math.max(1, (int)Math.ceil(filtered.size() / (double)PAGE_SIZE));
        if (currentPage > pages) currentPage = pages;
        var metrics = controller.metrics(filtered);
        kpis.getChildren().setAll(
                kpi("BERÇOS VISÍVEIS", String.valueOf(metrics.berthCount()), "de 60 cadastrados", "#0284c7"),
                kpi("NAVIOS PROGRAMADOS", String.valueOf(metrics.scheduleCount()), "no período selecionado", "#7c3aed"),
                kpi("TONELAGEM PLANEJADA", Ui.money(metrics.tonnage()) + " t", "carga movimentada", "#0891b2"),
                kpi("TAXA DE OCUPAÇÃO", String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", metrics.occupancyPercent()), "ocupação média mensal", "#16a34a"));
        int from = (currentPage - 1) * PAGE_SIZE;
        List<Berth> page = filtered.stream().skip(from).limit(PAGE_SIZE).toList();
        berthCards.getChildren().clear();
        trackBerths.clear();
        for (Berth berth : page) berthCards.getChildren().add(createBerthRow(berth));
        buildPageButtons(pages);
    }
    private VBox kpi(String title, String value, String hint, String color) {
        VBox card = Ui.card("kpi-card");
        Label valueLabel = Ui.label(value, "kpi-value"); valueLabel.setStyle("-fx-text-fill: " + color + ";");
        card.getChildren().addAll(Ui.label(title, "kpi-caption"), valueLabel, Ui.label(hint, "muted-text"));
        HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE); return card;
    }
    private HBox createBerthRow(Berth berth) {
        VBox details = new VBox(5, Ui.label(berth.getName(), "berth-name"),
                Ui.label(berth.getCode() + "  •  " + berth.getSector(), "muted-text"),
                Ui.label("Calado " + Ui.decimal(berth.getMaxDraft()) + " m  |  " + berth.getCargoProfile().getLabel(), "small-text"));
        details.setPrefWidth(232); details.setMinWidth(232); details.setAlignment(Pos.CENTER_LEFT);
        Canvas track = new Canvas(TRACK_WIDTH, 64); trackBerths.put(track, berth); drawBerthTrack(track, berth);
        track.widthProperty().addListener((o,a,b)->drawBerthTrack(track, berth));
        track.setCursor(Cursor.HAND);
        track.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            draggingSchedule = scheduleAt(berth, event, track);
            if (draggingSchedule != null) {
                selectSchedule(draggingSchedule);
                dragSourceTrack = track;
                dragTargetTrack = track;
                dragPreviewStartDay = draggingSchedule.getStartDay();
                double dayW = track.getWidth() / 30.0;
                dragPreviewX = (draggingSchedule.getStartDay() - 1) * dayW;
                dragPointerOffsetDays = event.getX() / dayW - (draggingSchedule.getStartDay() - 1);
                dragMoved = false;
                validateDragPreview();
                track.setCursor(Cursor.CLOSED_HAND);
                redrawVisibleTracks();
            }
        });
        track.setOnMouseDragged(event -> {
            if (draggingSchedule != null) updateDragPreview(event);
        });
        track.setOnMouseReleased(event -> finishDrag(event));
        Tooltip.install(track, new Tooltip("Clique em uma barra para ver detalhes. Segure e arraste o navio: a prévia acompanha o mouse em tempo real. Destino vermelho = movimentação bloqueada."));
        HBox row = new HBox(12, details, track); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("berth-row");
        row.setPadding(new Insets(8, 10, 8, 10)); return row;
    }

    private void updateDragPreview(MouseEvent event) {
        dragMoved = true;
        Canvas target = findTrackAt(event.getSceneX(), event.getSceneY());
        dragTargetTrack = target;
        if (target != null) {
            Point2DLocal point = localPoint(target, event.getSceneX(), event.getSceneY());
            double dayW = target.getWidth() / 30.0;
            double desiredX = point.x - dragPointerOffsetDays * dayW;
            double maxX = Math.max(0, target.getWidth() - draggingSchedule.getCalendarDays() * dayW);
            dragPreviewX = Math.max(0, Math.min(maxX, desiredX));
            int maxStart = Math.max(1, 31 - draggingSchedule.getCalendarDays());
            dragPreviewStartDay = Math.max(1, Math.min(maxStart, (int)Math.round(dragPreviewX / dayW) + 1));
            validateDragPreview();
            Berth targetBerth = trackBerths.get(target);
            selectedBanner.setText("Arrastando " + vesselName(draggingSchedule) + " → "
                    + (targetBerth == null ? "—" : targetBerth.getCode()) + ", início no dia " + dragPreviewStartDay
                    + " • " + (dragPreviewValid ? "destino válido" : dragPreviewMessage));
        } else {
            dragPreviewValid = false;
            dragPreviewMessage = "Solte sobre a linha de um berço";
            selectedBanner.setText("Arrastando " + vesselName(draggingSchedule) + " — leve até a linha do berço desejado.");
        }
        redrawVisibleTracks();
    }

    private void validateDragPreview() {
        Berth targetBerth = dragTargetTrack == null ? null : trackBerths.get(dragTargetTrack);
        if (targetBerth == null) {
            dragPreviewValid = false;
            dragPreviewMessage = "Berço de destino inválido";
            return;
        }
        try {
            controller.validateMoveSchedule(draggingSchedule, targetBerth.getCode(), dragPreviewStartDay);
            dragPreviewValid = true;
            dragPreviewMessage = "Destino válido";
        } catch (RuntimeException ex) {
            dragPreviewValid = false;
            dragPreviewMessage = ex.getMessage() == null ? "Movimentação bloqueada" : ex.getMessage();
        }
    }

    private void finishDrag(MouseEvent event) {
        if (draggingSchedule == null) return;
        if (dragMoved) updateDragPreview(event);
        ShipSchedule schedule = draggingSchedule;
        Canvas targetTrack = dragTargetTrack;
        Berth targetBerth = targetTrack == null ? null : trackBerths.get(targetTrack);
        int targetDay = dragPreviewStartDay;
        boolean shouldMove = dragMoved && targetBerth != null;
        clearDragState();
        if (shouldMove) {
            try {
                controller.moveSchedule(schedule, targetBerth.getCode(), targetDay);
                selectedBanner.setText("Atracação movimentada: " + vesselName(schedule) + " → " + targetBerth.getCode() + ", dia " + targetDay + ".");
            } catch (RuntimeException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage(), ButtonType.OK).showAndWait();
                selectedBanner.setText("Movimentação cancelada: " + ex.getMessage());
            }
        }
        refresh();
    }

    private void clearDragState() {
        draggingSchedule = null;
        dragSourceTrack = null;
        dragTargetTrack = null;
        dragMoved = false;
        for (Canvas canvas : trackBerths.keySet()) canvas.setCursor(Cursor.HAND);
    }

    private void redrawVisibleTracks() {
        for (Map.Entry<Canvas, Berth> entry : new ArrayList<>(trackBerths.entrySet())) {
            if (entry.getKey().getScene() != null) drawBerthTrack(entry.getKey(), entry.getValue());
        }
    }
    private void drawAxis(GraphicsContext g, double width) {
        g.clearRect(0,0,width,38); double dayW = width/30.0;
        for(int d=1;d<=30;d++) {
            double x=(d-1)*dayW;
            if(criticalDays.contains(d)) { g.setFill(Color.web("#fee2e2")); g.fillRect(x,0,dayW,35); }
            g.setStroke(Color.web("#e2e8f0")); g.strokeLine(x,8,x,34);
            g.setFill(criticalDays.contains(d)?Color.web("#b91c1c"):Color.web("#64748b"));
            g.setFont(Font.font("Arial", FontWeight.NORMAL, 10)); g.fillText(String.valueOf(d), x+dayW/2-4, 24);
        }
        g.setStroke(Color.web("#cbd5e1")); g.strokeLine(0,35,width,35);
    }
    private void drawBerthTrack(Canvas canvas, Berth berth) {
        double width = canvas.getWidth() > 50 ? canvas.getWidth() : TRACK_WIDTH;
        double height = canvas.getHeight(); double dayW = width/30.0;
        GraphicsContext g = canvas.getGraphicsContext2D(); g.clearRect(0,0,width,height);
        g.setFill(Color.web("#f8fafc")); g.fillRoundRect(0,5,width,height-10,8,8);
        for(int d=1;d<=30;d++) {
            double x=(d-1)*dayW;
            if(criticalDays.contains(d)) { g.setFill(Color.web("#fecaca",0.55)); g.fillRect(x,5,dayW,height-10); }
            g.setStroke(Color.web("#e2e8f0")); g.strokeLine(x,5,x,height-5);
        }
        List<ShipSchedule> schedules = controller.schedules().stream().filter(s->s.getBerthCode().equals(berth.getCode())).toList();
        int lane=0;
        for(ShipSchedule s:schedules) {
            double x=(s.getStartDay()-1)*dayW+2; double w=Math.max(10,(s.getEndDay()-s.getStartDay())*dayW-4); double y=9+(lane%2)*25;
            Color color=Color.web(s.getStatus().getColorHex());
            boolean isBeingDragged = s == draggingSchedule;
            g.setGlobalAlpha(isBeingDragged ? 0.22 : 1.0);
            g.setFill(color); g.fillRoundRect(x,y,w,20,8,8);
            RegisteredVessel vessel=controller.vessels().stream().filter(v->v.getImo().equals(s.getVesselImo())).findFirst().orElse(null);
            String name=vessel==null?s.getVesselImo():vessel.getName();
            g.setFill(Color.WHITE); g.setFont(Font.font("Arial",FontWeight.BOLD,10));
            String text = name.length()>24 ? name.substring(0,22)+"…" : name;
            if(w>58) g.fillText(text,x+6,y+14);
            g.setGlobalAlpha(1.0);
            lane++;
        }
        // Prévia translúcida que acompanha o ponteiro durante todo o arraste.
        if (draggingSchedule != null && dragTargetTrack == canvas) {
            int start = dragPreviewStartDay;
            double x = dragPreviewX + 2;
            double w = Math.max(10, draggingSchedule.getCalendarDays() * dayW - 4);
            boolean occupiedLane = schedules.stream().filter(other -> other != draggingSchedule)
                    .anyMatch(other -> start < other.getEndDay() && other.getStartDay() < start + draggingSchedule.getCalendarDays());
            double y = occupiedLane ? 34 : 9;
            Color preview = dragPreviewValid ? Color.web("#16a34a") : Color.web("#dc2626");
            g.setFill(Color.color(preview.getRed(), preview.getGreen(), preview.getBlue(), 0.34));
            g.fillRoundRect(x,y,w,20,8,8);
            g.setStroke(preview); g.setLineWidth(2); g.strokeRoundRect(x,y,w,20,8,8);
            g.setFill(dragPreviewValid ? Color.web("#166534") : Color.web("#991b1b"));
            g.setFont(Font.font("Arial",FontWeight.BOLD,10));
            String label = vesselName(draggingSchedule);
            if (w > 70) g.fillText(label.length() > 22 ? label.substring(0,20) + "…" : label, x + 6, y + 14);
        }
    }

    private ShipSchedule scheduleAt(Berth berth, MouseEvent event, Canvas canvas) {
        double dayW = canvas.getWidth()/30.0;
        int lane = 0;
        for (ShipSchedule schedule : controller.schedules().stream()
                .filter(s -> s.getBerthCode().equals(berth.getCode())).toList()) {
            double x = (schedule.getStartDay() - 1) * dayW + 2;
            double w = Math.max(10, (schedule.getEndDay() - schedule.getStartDay()) * dayW - 4);
            double y = 9 + (lane % 2) * 25;
            if (event.getX() >= x && event.getX() <= x + w
                    && event.getY() >= y && event.getY() <= y + 20) return schedule;
            lane++;
        }
        return null;
    }
    private void selectSchedule(ShipSchedule schedule) {
        RegisteredVessel vessel = controller.vessels().stream().filter(v -> v.getImo().equals(schedule.getVesselImo())).findFirst().orElse(null);
        String ship = vessel == null ? schedule.getVesselImo() : vessel.getName();
        selectedBanner.setText("NAVIO SELECIONADO • " + ship + " | Berço " + schedule.getBerthCode()
                + " | Dias " + schedule.getStartDay() + "–" + schedule.getEndDay()
                + " | Calado: " + (vessel == null ? "—" : Ui.decimal(vessel.getMaxDraft()) + " m")
                + " | Carga: " + (vessel == null ? "—" : vessel.getLoadProduct())
                + " | Descarga: " + (vessel == null ? "—" : vessel.getDischargeProduct()));
    }
    private String vesselName(ShipSchedule schedule) {
        return controller.vessels().stream().filter(v -> v.getImo().equals(schedule.getVesselImo()))
                .map(RegisteredVessel::getName).findFirst().orElse(schedule.getVesselImo());
    }
    private Canvas findTrackAt(double sceneX, double sceneY) {
        for (Canvas canvas : trackBerths.keySet()) {
            var bounds = canvas.localToScene(canvas.getBoundsInLocal());
            if (bounds != null && bounds.contains(sceneX, sceneY)) return canvas;
        }
        return null;
    }
    private Point2DLocal localPoint(Canvas canvas, double sceneX, double sceneY) {
        var point = canvas.sceneToLocal(sceneX, sceneY);
        return new Point2DLocal(point.getX(), point.getY());
    }
    private record Point2DLocal(double x, double y) { }
    private void buildPageButtons(int pages) {
        pagination.getChildren().clear();
        Button first=Ui.button("«","page-button",()->goToPage(1)); Button previous=Ui.button("‹","page-button",()->goToPage(Math.max(1,currentPage-1)));
        pagination.getChildren().addAll(first,previous);
        for(int p=1;p<=pages;p++) { final int target=p; Button b=Ui.button(String.valueOf(p),p==currentPage?"page-button page-active":"page-button",()->goToPage(target)); pagination.getChildren().add(b); }
        Button next=Ui.button("›","page-button",()->goToPage(Math.min(pages,currentPage+1))); Button last=Ui.button("»","page-button",()->goToPage(pages));
        pagination.getChildren().addAll(next,last);
        pageInfo.setText("Página "+currentPage+" de "+pages+"  •  "+visibleBerths.size()+" berços filtrados");
        pagination.getChildren().add(pageInfo); pagination.setSpacing(6);
    }
    private void goToPage(int page) { currentPage=page; refresh(); }
}
