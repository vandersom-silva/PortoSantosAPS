package br.edu.unirv.portos.view;

import br.edu.unirv.portos.controller.PortController;
import br.edu.unirv.portos.model.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Locale;

public final class SchedulesView extends VBox {
    private final PortController controller;
    private final ComboBox<Berth> berth=new ComboBox<>();
    private final ComboBox<RegisteredVessel> vessel=new ComboBox<>();
    private final Spinner<Integer> startDay=new Spinner<>(1,28,3);
    private final Spinner<Integer> endDay=new Spinner<>(2,30,12);
    private final TextField tonnage=Ui.textField("Ex.: 65000");
    private final ComboBox<ScheduleStatus> status=new ComboBox<>();
    private final Label duration=Ui.label("Duração estimada: —", "muted-text");
    private final TableView<ShipSchedule> table=new TableView<>();
    public SchedulesView(PortController controller){
        this.controller=controller;setSpacing(14);setPadding(new Insets(4,26,28,26));
        berth.setItems(controller.berths());berth.setMaxWidth(Double.MAX_VALUE);vessel.setItems(controller.vessels());vessel.setMaxWidth(Double.MAX_VALUE);
        startDay.setEditable(true);endDay.setEditable(true);startDay.setMaxWidth(Double.MAX_VALUE);endDay.setMaxWidth(Double.MAX_VALUE);
        status.getItems().setAll(ScheduleStatus.values());status.setValue(ScheduleStatus.PROGRAMMED);status.setMaxWidth(Double.MAX_VALUE);
        berth.getSelectionModel().selectFirst();vessel.getSelectionModel().selectFirst();
        berth.valueProperty().addListener((o,a,b)->updateDuration());tonnage.textProperty().addListener((o,a,b)->updateDuration());
        VBox form=Ui.card("form-card");
        HBox r1=new HBox(12,Ui.field("Berço de destino",berth),Ui.field("Navio mercante",vessel),Ui.field("Dia de início (Nov/2026)",startDay),Ui.field("Dia de fim (exclusivo)",endDay));
        HBox r2=new HBox(12,Ui.field("Tonelagem total (t)",tonnage),Ui.field("Status operacional",status),new VBox(6,Ui.fieldLabel("Duração determinística"),duration));
        r1.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));r2.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
        Button save=Ui.button("Programar atracação","primary-button",this::save);Button reset=Ui.button("Limpar campos","secondary-button",this::reset);
        form.getChildren().addAll(new HBox(10,Ui.label("Nova programação de chegada e atracação","section-title"),new Region(),Ui.badge("RF011","#7c3aed")),r1,r2,new HBox(8,save,reset),Ui.label("Regras aplicadas: berço compatível, sem sobreposição temporal e bloqueio de maré ≤ 0,4 m para calado ≥ 13,5 m.","hint-text"));HBox.setHgrow(form.getChildren().get(0),Priority.ALWAYS);
        VBox listCard=Ui.card("table-card");HBox heading=new HBox(8,Ui.label("Atracações programadas", "section-title"),new Region(),Ui.badge("Atualização automática do Lineup","#16a34a"));HBox.setHgrow(heading.getChildren().get(1),Priority.ALWAYS);
        buildTable();table.setItems(controller.schedules());table.setPrefHeight(520);listCard.getChildren().addAll(heading,table);
        getChildren().addAll(form,listCard);VBox.setVgrow(listCard,Priority.ALWAYS);updateDuration();
    }
    private void buildTable(){
        Ui.configureTable(table);
        TableColumn<ShipSchedule,String> c1=new TableColumn<>("Berço");c1.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getBerthCode()));
        TableColumn<ShipSchedule,String> c2=new TableColumn<>("Navio");c2.setCellValueFactory(v->new ReadOnlyStringWrapper(controller.vessels().stream().filter(s->s.getImo().equals(v.getValue().getVesselImo())).map(RegisteredVessel::getName).findFirst().orElse(v.getValue().getVesselImo())));
        TableColumn<ShipSchedule,String> c3=new TableColumn<>("Início");c3.setCellValueFactory(v->new ReadOnlyStringWrapper(String.format("%02d/11/2026",v.getValue().getStartDay())));
        TableColumn<ShipSchedule,String> c4=new TableColumn<>("Fim");c4.setCellValueFactory(v->new ReadOnlyStringWrapper(String.format("%02d/11/2026",v.getValue().getEndDay())));
        TableColumn<ShipSchedule,String> c5=new TableColumn<>("Duração");c5.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getEstimatedDurationDays(controller.berths().stream().filter(b->b.getCode().equals(v.getValue().getBerthCode())).findFirst().orElseThrow())+" dias"));
        TableColumn<ShipSchedule,String> c6=new TableColumn<>("Tonelagem");c6.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.money(v.getValue().getTonnage())+" t"));
        TableColumn<ShipSchedule,String> c7=new TableColumn<>("Status");c7.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getStatus().getLabel()));
        table.getColumns().addAll(c1,c2,c3,c4,c5,c6,c7);
    }
    private void updateDuration(){try{if(berth.getValue()!=null&&!tonnage.getText().isBlank()){double tons=Double.parseDouble(tonnage.getText().trim().replace(".","").replace(",","."));int days=controller.estimatedDuration(berth.getValue().getCode(),tons);duration.setText(days+" dia(s) • ⌈tonelagem / taxa diária⌉");}else duration.setText("Informe a tonelagem.");}catch(Exception e){duration.setText("Informe uma tonelagem válida.");}}
    private void save(){try{if(berth.getValue()==null||vessel.getValue()==null)throw new IllegalArgumentException("Selecione o berço e o navio.");double tons=Double.parseDouble(tonnage.getText().trim().replace(".","").replace(",","."));int start=Integer.parseInt(startDay.getEditor().getText());int end=Integer.parseInt(endDay.getEditor().getText());controller.saveSchedule(berth.getValue().getCode(),vessel.getValue().getImo(),start,end,tons,status.getValue());Ui.info("Atracação programada","A nova escala foi incluída no Lineup e no histórico.");tonnage.clear();updateDuration();}catch(Exception ex){Ui.error(ex.getMessage());}}
    private void reset(){startDay.getValueFactory().setValue(3);endDay.getValueFactory().setValue(12);tonnage.clear();status.setValue(ScheduleStatus.PROGRAMMED);updateDuration();}
}
