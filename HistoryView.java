package br.edu.unirv.portos.view;

import br.edu.unirv.portos.controller.PortController;
import br.edu.unirv.portos.model.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Comparator;
import java.util.Locale;

public final class HistoryView extends VBox {
    private final PortController controller;
    private final TextField search=Ui.textField("Buscar navio, IMO, berço ou status...");
    private final TableView<ShipSchedule> table=new TableView<>();
    private final CheckBox supervisorAuditor = new CheckBox("Perfil supervisor auditor");
    public HistoryView(PortController controller){
        this.controller=controller;setSpacing(14);setPadding(new Insets(4,26,28,26));
        supervisorAuditor.setTooltip(new Tooltip("Ativa a permissão simulada de auditor para corrigir registros históricos concluídos (RN006)."));
        HBox toolbar=new HBox(10,Ui.sectionTitle("Auditoria cronológica de atracações","RF012 • 153 escalas demonstrativas no mês de novembro/2026."),new Region(),supervisorAuditor,search);HBox.setHgrow(toolbar.getChildren().get(1),Priority.ALWAYS);toolbar.setAlignment(Pos.CENTER_LEFT);search.setPrefWidth(260);
        VBox card=Ui.card("table-card");
        HBox stats=new HBox(12,kpi("ATRACAÇÕES REGISTRADAS",String.valueOf(controller.schedules().size())),kpi("NAVIOS NA FROTA",String.valueOf(controller.vessels().size())),kpi("BERÇOS CADASTRADOS",String.valueOf(controller.berths().size())));
        Ui.configureTable(table);
        TableColumn<ShipSchedule,String> c1=new TableColumn<>("Berço");c1.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getBerthCode()));
        TableColumn<ShipSchedule,String> c2=new TableColumn<>("Navio");c2.setCellValueFactory(v->new ReadOnlyStringWrapper(controller.vessels().stream().filter(s->s.getImo().equals(v.getValue().getVesselImo())).map(RegisteredVessel::getName).findFirst().orElse(v.getValue().getVesselImo())));
        TableColumn<ShipSchedule,String> c3=new TableColumn<>("IMO");c3.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getVesselImo()));
        TableColumn<ShipSchedule,String> c4=new TableColumn<>("Início");c4.setCellValueFactory(v->new ReadOnlyStringWrapper(String.format("%02d/11/2026",v.getValue().getStartDay())));
        TableColumn<ShipSchedule,String> c5=new TableColumn<>("Fim");c5.setCellValueFactory(v->new ReadOnlyStringWrapper(String.format("%02d/11/2026",v.getValue().getEndDay())));
        TableColumn<ShipSchedule,String> c6=new TableColumn<>("Duração");c6.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getEstimatedDurationDays(controller.berths().stream().filter(b->b.getCode().equals(v.getValue().getBerthCode())).findFirst().orElseThrow())+" dias"));
        TableColumn<ShipSchedule,String> c7=new TableColumn<>("Tonelagem");c7.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.money(v.getValue().getTonnage())+" t"));
        TableColumn<ShipSchedule,String> c8=new TableColumn<>("Status");c8.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getStatus().getLabel()));
        table.getColumns().addAll(c1,c2,c3,c4,c5,c6,c7,c8,Ui.actionColumn("Detalhes",s->Ui.info("Detalhes da atracação",details(s)),this::delete));
        FilteredList<ShipSchedule> filtered=new FilteredList<>(controller.schedules(),s->true);
        search.textProperty().addListener((o,a,v)->filtered.setPredicate(s->{String q=v.toLowerCase(Locale.ROOT).trim();if(q.isEmpty())return true;String ship=controller.vessels().stream().filter(x->x.getImo().equals(s.getVesselImo())).map(RegisteredVessel::getName).findFirst().orElse("");return s.getBerthCode().toLowerCase().contains(q)||s.getVesselImo().toLowerCase().contains(q)||ship.toLowerCase().contains(q)||s.getStatus().getLabel().toLowerCase().contains(q);}));
        SortedList<ShipSchedule> sorted = new SortedList<>(filtered, Comparator.comparingInt(ShipSchedule::getStartDay));
        table.setItems(sorted);
        card.getChildren().addAll(stats,table);table.setPrefHeight(650);VBox.setVgrow(table,Priority.ALWAYS);
        getChildren().addAll(toolbar,card);VBox.setVgrow(card,Priority.ALWAYS);
    }
    private VBox kpi(String title,String value){VBox b=Ui.card("mini-kpi");b.getChildren().addAll(Ui.label(title,"kpi-caption"),Ui.label(value,"kpi-value"));HBox.setHgrow(b,Priority.ALWAYS);b.setMaxWidth(Double.MAX_VALUE);return b;}
    private String details(ShipSchedule s) {
        RegisteredVessel v = controller.vessels().stream()
                .filter(x -> x.getImo().equals(s.getVesselImo())).findFirst().orElse(null);
        return "Berço: " + s.getBerthCode()
                + "\nNavio: " + (v == null ? s.getVesselImo() : v.getName())
                + "\nPeríodo: " + s.getStartDay() + " a " + s.getEndDay() + "/11/2026"
                + "\nTonelagem: " + Ui.money(s.getTonnage()) + " t"
                + "\nCarga: " + (v == null ? "—" : v.getLoadProduct())
                + "\nDescarga: " + (v == null ? "—" : v.getDischargeProduct())
                + "\nStatus: " + s.getStatus().getLabel();
    }
    private void delete(ShipSchedule s){
        if(controller.isHistoricalSchedule(s) && !supervisorAuditor.isSelected()){
            Ui.error("Esta atracação já foi concluída. A RN006 impede alterações no histórico sem perfil de supervisor auditor.\nAtive o perfil de auditor para simular a autorização.");
            return;
        }
        if(Ui.confirm("Excluir programação","Deseja excluir esta atracação? A barra correspondente será removida do Lineup.")){
            try{controller.deleteSchedule(s,supervisorAuditor.isSelected());}catch(Exception ex){Ui.error(ex.getMessage());}
        }
    }
}
