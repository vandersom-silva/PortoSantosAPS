package br.edu.unirv.portos.view;

import br.edu.unirv.portos.controller.PortController;
import br.edu.unirv.portos.model.*;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import java.util.Locale;

public final class BerthsView extends VBox {
    private final PortController controller;
    private final TextField code = Ui.textField("Ex.: SV-99");
    private final TextField name = Ui.textField("Nome oficial do berço");
    private final TextField sector = Ui.textField("Ex.: Saboó / Valongo");
    private final TextField draft = Ui.textField("Ex.: 14,5");
    private final TextField dailyRate = Ui.textField("Ex.: 28000");
    private final ComboBox<CargoProfile> profile = new ComboBox<>();
    private final TextField search = Ui.textField("Buscar por código, nome ou setor...");
    private final TableView<Berth> table = new TableView<>();
    private Berth editing;

    public BerthsView(PortController controller) {
        this.controller=controller; setSpacing(14); setPadding(new Insets(4,26,28,26));
        profile.getItems().setAll(CargoProfile.values()); profile.setMaxWidth(Double.MAX_VALUE);
        VBox form=Ui.card("form-card");
        HBox row1=new HBox(12,Ui.field("Código do berço",code),Ui.field("Nome oficial",name),Ui.field("Setor portuário",sector));
        HBox row2=new HBox(12,Ui.field("Calado máximo (m)",draft),Ui.field("Perfil de carga",profile),Ui.field("Taxa nominal (t/dia)",dailyRate));
        row1.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS)); row2.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
        Button save=Ui.button("Salvar berço","primary-button",this::save);
        Button clear=Ui.button("Limpar formulário","secondary-button",this::clearForm);
        HBox actions=new HBox(8,save,clear); actions.setAlignment(Pos.CENTER_LEFT);
        form.getChildren().addAll(new HBox(10,Ui.label("Cadastro e edição de berços", "section-title"),new Region(),Ui.badge("RF005", "#0284c7")),row1,row2,actions);
        HBox.setHgrow(form.getChildren().get(0),Priority.ALWAYS);
        HBox toolbar=new HBox(10,Ui.sectionTitle("Tabela de berços cadastrados","RF006 • Consulte a lista completa, inclusive os berços 59 e 60."),new Region(),search);
        HBox.setHgrow(toolbar.getChildren().get(1),Priority.ALWAYS); toolbar.setAlignment(Pos.CENTER_LEFT); search.setPrefWidth(280);
        buildTable();
        FilteredList<Berth> filtered=new FilteredList<>(controller.berths(),b->true);
        search.textProperty().addListener((obs,old,value)->filtered.setPredicate(b->{String q=value.toLowerCase(Locale.ROOT).trim();return q.isEmpty()||b.getCode().toLowerCase().contains(q)||b.getName().toLowerCase().contains(q)||b.getSector().toLowerCase().contains(q);}));
        table.setItems(filtered);
        getChildren().addAll(form,toolbar,table);
        VBox.setVgrow(table,Priority.ALWAYS); table.setPrefHeight(630);
    }
    private void buildTable() {
        Ui.configureTable(table);
        TableColumn<Berth,String> c1=new TableColumn<>("Código");c1.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getCode()));
        TableColumn<Berth,String> c2=new TableColumn<>("Nome");c2.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getName()));
        TableColumn<Berth,String> c3=new TableColumn<>("Setor");c3.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getSector()));
        TableColumn<Berth,String> c4=new TableColumn<>("Calado");c4.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.decimal(v.getValue().getMaxDraft())+" m"));
        TableColumn<Berth,String> c5=new TableColumn<>("Perfil");c5.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getCargoProfile().getLabel()));
        TableColumn<Berth,String> c6=new TableColumn<>("Taxa nominal");c6.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.money(v.getValue().getDailyRateTons())+" t/dia"));
        table.getColumns().addAll(c1,c2,c3,c4,c5,c6,Ui.actionColumn(this::edit,this::delete));
    }
    private void save() {
        try {
            controller.saveBerth(editing,code.getText(),name.getText(),sector.getText(),parseDecimal(draft.getText()),
                    profile.getValue()==null?CargoProfile.GENERAL_CARGO:profile.getValue(),parseDecimal(dailyRate.getText()));
            Ui.info("Berço salvo", "O cadastro foi atualizado em memória e o Lineup será atualizado automaticamente."); table.refresh(); clearForm();
        } catch(Exception ex){Ui.error(ex.getMessage());}
    }
    private double parseDecimal(String value) { return Double.parseDouble(value.trim().replace(",",".")); }
    private void edit(Berth b) { editing=b; code.setText(b.getCode());name.setText(b.getName());sector.setText(b.getSector());draft.setText(Ui.decimal(b.getMaxDraft()));dailyRate.setText(String.valueOf((long)b.getDailyRateTons()));profile.setValue(b.getCargoProfile()); }
    private void delete(Berth b) {
        long linked=controller.schedules().stream().filter(s->s.getBerthCode().equals(b.getCode())).count();
        String message=linked>0?"Este berço possui "+linked+" atracação(ões). Todas serão removidas em cascata (RN024). Deseja continuar?":"Deseja excluir o berço "+b.getCode()+"?";
        if(Ui.confirm("Confirmar exclusão",message)){controller.deleteBerth(b);if(editing==b)clearForm();}
    }
    private void clearForm(){editing=null;code.clear();name.clear();sector.clear();draft.clear();dailyRate.setText("28000");profile.setValue(CargoProfile.GENERAL_CARGO);}
}
