package br.edu.unirv.portos.view;

import br.edu.unirv.portos.controller.PortController;
import br.edu.unirv.portos.model.RegisteredVessel;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import java.util.Locale;

public final class VesselsView extends VBox {
    private final PortController controller;
    private final TextField imo=Ui.textField("IMO-9810101"), name=Ui.textField("Nome da embarcação"), draft=Ui.textField("Ex.: 14,2"), dwt=Ui.textField("Ex.: 68500");
    private final ComboBox<String> vesselClass=new ComboBox<>();
    private final TextField standard=Ui.textField("Produto padrão"), discharge=Ui.textField("Produto de descarga"), load=Ui.textField("Produto de carga");
    private final TextField search=Ui.textField("Buscar por nome, IMO ou classe...");
    private final TableView<RegisteredVessel> table=new TableView<>();
    private RegisteredVessel editing;
    public VesselsView(PortController controller){
        this.controller=controller;setSpacing(14);setPadding(new Insets(4,26,28,26));
        vesselClass.getItems().setAll("Panamax","Post-Panamax","Handymax","Suezmax","Kamsarmax");vesselClass.setValue("Panamax");
        VBox form=Ui.card("form-card");
        HBox r1=new HBox(12,Ui.field("Código IMO",imo),Ui.field("Nome do navio",name),Ui.field("Classe de porte",vesselClass),Ui.field("Calado máx. (m)",draft),Ui.field("Porte DWT (t)",dwt));
        HBox r2=new HBox(12,Ui.field("Produto padrão",standard),Ui.field("Descarga ↓",discharge),Ui.field("Carga ↑",load));
        r1.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));r2.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
        Button save=Ui.button("Salvar navio","primary-button",this::save);Button clear=Ui.button("Limpar formulário","secondary-button",this::clear);
        form.getChildren().addAll(new HBox(10,Ui.label("Cadastro de navios mercantes","section-title"),new Region(),Ui.badge("RF008","#0284c7")),r1,r2,new HBox(8,save,clear));HBox.setHgrow(form.getChildren().get(0),Priority.ALWAYS);
        HBox toolbar=new HBox(10,Ui.sectionTitle("Frota cadastrada","Mais de 150 registros • tabela completa com rolagem e busca instantânea."),new Region(),search);HBox.setHgrow(toolbar.getChildren().get(1),Priority.ALWAYS);toolbar.setAlignment(Pos.CENTER_LEFT);search.setPrefWidth(280);
        Ui.configureTable(table);
        TableColumn<RegisteredVessel,String> c1=new TableColumn<>("IMO");c1.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getImo()));
        TableColumn<RegisteredVessel,String> c2=new TableColumn<>("Nome");c2.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getName()));
        TableColumn<RegisteredVessel,String> c3=new TableColumn<>("Classe");c3.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getVesselClass()));
        TableColumn<RegisteredVessel,String> c4=new TableColumn<>("Calado");c4.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.decimal(v.getValue().getMaxDraft())+" m"));
        TableColumn<RegisteredVessel,String> c5=new TableColumn<>("Porte DWT");c5.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.money(v.getValue().getDwt())+" t"));
        TableColumn<RegisteredVessel,String> c6=new TableColumn<>("Descarga ↓");c6.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getDischargeProduct()));
        TableColumn<RegisteredVessel,String> c7=new TableColumn<>("Carga ↑");c7.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getLoadProduct()));
        table.getColumns().addAll(c1,c2,c3,c4,c5,c6,c7,Ui.actionColumn(this::edit,this::delete));
        FilteredList<RegisteredVessel> filtered=new FilteredList<>(controller.vessels(),v->true);
        search.textProperty().addListener((o,a,v)->filtered.setPredicate(s->{String q=v.toLowerCase(Locale.ROOT).trim();return q.isEmpty()||s.getName().toLowerCase().contains(q)||s.getImo().toLowerCase().contains(q)||s.getVesselClass().toLowerCase().contains(q);}));table.setItems(filtered);
        getChildren().addAll(form,toolbar,table);table.setPrefHeight(650);VBox.setVgrow(table,Priority.ALWAYS);
    }
    private void save(){try{controller.saveVessel(editing,imo.getText(),name.getText(),vesselClass.getValue(),Double.parseDouble(draft.getText().trim().replace(",",".")),Long.parseLong(dwt.getText().replace(".","").trim()),standard.getText(),discharge.getText(),load.getText());Ui.info("Navio salvo","Navio disponível para seleção na programação de atracações.");table.refresh();clear();}catch(Exception ex){Ui.error(ex.getMessage());}}
    private void edit(RegisteredVessel v){editing=v;imo.setText(v.getImo());name.setText(v.getName());vesselClass.setValue(v.getVesselClass());draft.setText(Ui.decimal(v.getMaxDraft()));dwt.setText(String.valueOf(v.getDwt()));standard.setText(v.getStandardProduct());discharge.setText(v.getDischargeProduct());load.setText(v.getLoadProduct());}
    private void delete(RegisteredVessel v){long linked=controller.schedules().stream().filter(s->s.getVesselImo().equals(v.getImo())).count();String msg="Excluir "+v.getName()+"?"+(linked>0?" As "+linked+" atracações vinculadas também serão excluídas.":"");if(Ui.confirm("Excluir navio",msg)){controller.deleteVessel(v);if(editing==v)clear();}}
    private void clear(){editing=null;imo.clear();name.clear();vesselClass.setValue("Panamax");draft.clear();dwt.clear();standard.clear();discharge.clear();load.clear();}
}
