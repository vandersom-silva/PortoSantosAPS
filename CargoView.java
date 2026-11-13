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

public final class CargoView extends VBox {
    private final PortController controller;
    private final TextField code=Ui.textField("Ex.: CRG-16"), name=Ui.textField("Commodity portuária");
    private final TextField rate=Ui.textField("Ex.: 30000");
    private final ComboBox<CargoDirection> direction=new ComboBox<>();
    private final ComboBox<CargoProfile> profile=new ComboBox<>();
    private final TextField search=Ui.textField("Buscar produto ou código...");
    private final TableView<CargoProduct> table=new TableView<>();
    private CargoProduct editing;
    public CargoView(PortController controller){
        this.controller=controller;setSpacing(14);setPadding(new Insets(4,26,28,26));
        direction.getItems().setAll(CargoDirection.values());profile.getItems().setAll(CargoProfile.values());
        direction.setValue(CargoDirection.MIXED);profile.setValue(CargoProfile.VEGETABLE_BULK);
        VBox form=Ui.card("form-card");
        HBox row=new HBox(12,Ui.field("Código do produto",code),Ui.field("Nome da commodity",name),Ui.field("Sentido operacional",direction),Ui.field("Perfil APS",profile),Ui.field("Taxa média (t/dia)",rate));
        row.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
        Button save=Ui.button("Salvar produto","primary-button",this::save);Button clear=Ui.button("Limpar","secondary-button",this::clear);
        form.getChildren().addAll(new HBox(10,Ui.label("Catálogo de cargas e perfis portuários APS","section-title"),new Region(),Ui.badge("RF007","#0284c7")),row,new HBox(8,save,clear));
        HBox.setHgrow(form.getChildren().get(0),Priority.ALWAYS);
        HBox toolbar=new HBox(10,Ui.sectionTitle("Commodities cadastradas","15 produtos demonstrativos • 11 perfis de carga APS."),new Region(),search);HBox.setHgrow(toolbar.getChildren().get(1),Priority.ALWAYS);toolbar.setAlignment(Pos.CENTER_LEFT);search.setPrefWidth(270);
        Ui.configureTable(table);
        TableColumn<CargoProduct,String> c1=new TableColumn<>("Código");c1.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getCode()));
        TableColumn<CargoProduct,String> c2=new TableColumn<>("Produto");c2.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getName()));
        TableColumn<CargoProduct,String> c3=new TableColumn<>("Sentido");c3.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getDirection().getLabel()));
        TableColumn<CargoProduct,String> c4=new TableColumn<>("Perfil de carga");c4.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getProfile().getLabel()));
        TableColumn<CargoProduct,String> c5=new TableColumn<>("Produtividade média");c5.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.money(v.getValue().getTonsPerDay())+" t/dia"));
        table.getColumns().addAll(c1,c2,c3,c4,c5,Ui.actionColumn(this::edit,this::delete));
        FilteredList<CargoProduct> filtered=new FilteredList<>(controller.cargoProducts(),p->true);
        search.textProperty().addListener((o,a,v)->filtered.setPredicate(p->{String q=v.toLowerCase(Locale.ROOT).trim();return q.isEmpty()||p.getName().toLowerCase().contains(q)||p.getCode().toLowerCase().contains(q);}));table.setItems(filtered);
        getChildren().addAll(form,toolbar,table);table.setPrefHeight(650);VBox.setVgrow(table,Priority.ALWAYS);
    }
    private void save(){try{controller.saveCargo(editing,code.getText(),name.getText(),direction.getValue(),profile.getValue(),Double.parseDouble(rate.getText().trim().replace(".","").replace(",",".")));Ui.info("Catálogo atualizado","Produto salvo em memória. A produtividade diária será considerada nas próximas operações.");table.refresh();clear();}catch(Exception ex){Ui.error(ex.getMessage());}}
    private void edit(CargoProduct p){editing=p;code.setText(p.getCode());name.setText(p.getName());direction.setValue(p.getDirection());profile.setValue(p.getProfile());rate.setText(String.valueOf((long)p.getTonsPerDay()));}
    private void delete(CargoProduct p){if(Ui.confirm("Excluir produto","Excluir "+p.getName()+" do catálogo?"))controller.deleteCargo(p);}
    private void clear(){editing=null;code.clear();name.clear();rate.clear();direction.setValue(CargoDirection.MIXED);profile.setValue(CargoProfile.VEGETABLE_BULK);}
}
