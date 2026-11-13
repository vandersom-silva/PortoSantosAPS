package br.edu.unirv.portos.view;

import br.edu.unirv.portos.controller.PortController;
import br.edu.unirv.portos.model.TideMeasurement;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TidesView extends VBox {
    private final PortController controller;
    private final ComboBox<String> month=new ComboBox<>();
    private final Canvas chart=new Canvas(1000,250);
    private final TableView<TideMeasurement> table=new TableView<>();
    private final Label summary=Ui.label("1.440 medições carregadas", "muted-text");
    private String monthKey="2026-11";
    public TidesView(PortController controller){
        this.controller=controller;setSpacing(14);setPadding(new Insets(4,26,28,26));
        month.getItems().setAll("Novembro / 2026","Setembro / 2026","Junho / 2026");month.setValue("Novembro / 2026");
        month.valueProperty().addListener((o,a,v)->{monthKey=switch(v){case "Setembro / 2026"->"2026-09";case "Junho / 2026"->"2026-06";default->"2026-11";};refresh();});
        VBox chartCard=Ui.card("chart-card");
        HBox top=new HBox(10,Ui.label("Gráfico hidrodinâmico", "section-title"),new Region(),Ui.badge("Linha crítica: 0,4 m", "#dc2626"));HBox.setHgrow(top.getChildren().get(1),Priority.ALWAYS);top.setAlignment(Pos.CENTER_LEFT);
        chart.widthProperty().addListener((o,a,b)->drawChart());
        StackPane chartWrap=new StackPane(chart);chartWrap.setMinHeight(260);chartWrap.setPrefHeight(260);chart.widthProperty().bind(chartWrap.widthProperty());
        chartCard.getChildren().addAll(top,chartWrap);
        HBox toolbar=new HBox(10,Ui.sectionTitle("Medições da tábua", "Granularidade de 30 minutos • 48 medições por dia."),new Region(),month,Ui.button("Importar Tábua CSV","primary-button",this::importCsv));
        HBox.setHgrow(toolbar.getChildren().get(1),Priority.ALWAYS);toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox tableCard=Ui.card("table-card");
        HBox tableTitle=new HBox(8,Ui.label("Registros astronômicos", "section-title"),new Region(),summary);HBox.setHgrow(tableTitle.getChildren().get(1),Priority.ALWAYS);tableTitle.setAlignment(Pos.CENTER_LEFT);
        Ui.configureTable(table);
        TableColumn<TideMeasurement,String> date=new TableColumn<>("Data");date.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getDateLabel()));
        TableColumn<TideMeasurement,String> time=new TableColumn<>("Hora");time.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getTimeLabel()));
        TableColumn<TideMeasurement,String> height=new TableColumn<>("Altura (m)");height.setCellValueFactory(v->new ReadOnlyStringWrapper(Ui.decimal(v.getValue().getHeightMeters())));
        TableColumn<TideMeasurement,String> condition=new TableColumn<>("Condição");condition.setCellValueFactory(v->new ReadOnlyStringWrapper(v.getValue().getCondition()));
        table.getColumns().addAll(date,time,height,condition);table.setPrefHeight(410);
        tableCard.getChildren().addAll(tableTitle,table);
        getChildren().addAll(toolbar,chartCard,tableCard);VBox.setVgrow(tableCard,Priority.ALWAYS);
        refresh();
    }
    private void refresh(){
        List<TideMeasurement> values=controller.tides(monthKey).stream().sorted(Comparator.comparing(TideMeasurement::getMeasuredAt)).toList();
        table.getItems().setAll(values);
        long critical=values.stream().filter(TideMeasurement::isCritical).count();
        summary.setText(String.format("%,d medições  •  %d críticas",values.size(),critical));
        drawChart();
    }
    private void drawChart(){
        double w=Math.max(500,chart.getWidth()),h=chart.getHeight();GraphicsContext g=chart.getGraphicsContext2D();g.clearRect(0,0,w,h);
        g.setFill(Color.WHITE);g.fillRect(0,0,w,h);
        double left=48,right=18,top=18,bottom=30,plotW=w-left-right,plotH=h-top-bottom;
        g.setFont(Font.font("Arial",10));g.setFill(Color.web("#64748b"));
        for(int level=0;level<=2;level++){
            double y=top+plotH-(level/2.0)*plotH;g.setStroke(Color.web("#e2e8f0"));g.strokeLine(left,y,w-right,y);g.fillText(String.format("%.1f m",level*0.9),4,y+4);
        }
        double thresholdY=top+plotH-(0.4/1.9)*plotH;g.setStroke(Color.web("#dc2626"));g.setLineDashes(6,4);g.strokeLine(left,thresholdY,w-right,thresholdY);g.setLineDashes();
        List<TideMeasurement> values=controller.tides(monthKey);
        if(values.isEmpty()){g.setFill(Color.web("#64748b"));g.fillText("Importe uma tábua CSV com 1.440 medições para este mês.",left+12,top+plotH/2);return;}
        g.setStroke(Color.web("#0284c7"));g.setLineWidth(1.5);boolean first=true;
        for(int i=0;i<values.size();i++){
            double x=left+(i/(double)Math.max(1,values.size()-1))*plotW;
            double y=top+plotH-(Math.max(0,Math.min(1.9,values.get(i).getHeightMeters()))/1.9)*plotH;
            if(first){g.beginPath();g.moveTo(x,y);first=false;}else g.lineTo(x,y);
        }
        g.stroke();g.setLineWidth(1);g.setFill(Color.web("#64748b"));g.fillText("0h",left,h-8);g.fillText("06h",left+plotW*0.25,h-8);g.fillText("12h",left+plotW*0.5,h-8);g.fillText("18h",left+plotW*0.75,h-8);g.fillText("24h",w-right-24,h-8);
        g.setFill(Color.web("#dc2626"));g.fillText("0,4 m",w-right-40,thresholdY-5);
    }
    private void importCsv(){
        FileChooser chooser=new FileChooser();chooser.setTitle("Importar tábua de marés CSV");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV","*.csv","*.txt"));
        File file=chooser.showOpenDialog(getScene().getWindow());if(file==null)return;
        try{
            List<TideMeasurement> parsed=new ArrayList<>();int lineNo=0;
            for(String line:Files.readAllLines(file.toPath())){
                lineNo++;if(line.isBlank())continue;String[] p=line.trim().split(line.contains(";")?";":",");
                if(p.length<2)continue;
                try{
                    LocalDateTime dt;double height;
                    if(p.length>=3){LocalDate d=parseDate(p[0].trim());LocalTime t=LocalTime.parse(p[1].trim());dt=LocalDateTime.of(d,t);height=parseNumber(p[2]);}
                    else{dt=LocalDateTime.parse(p[0].trim());height=parseNumber(p[1]);}
                    if(!dt.toLocalDate().getMonth().equals(java.time.Month.of(Integer.parseInt(monthKey.substring(5)))))throw new IllegalArgumentException("O CSV contém datas fora do mês selecionado.");
                    parsed.add(new TideMeasurement(dt,height));
                }catch(Exception ex){if(lineNo==1)continue;throw new IllegalArgumentException("Erro na linha "+lineNo+": "+ex.getMessage());}
            }
            if(parsed.size()!=1440)throw new IllegalArgumentException("A tábua precisa conter exatamente 1.440 medições. Foram encontradas "+parsed.size()+".");
            parsed.sort(Comparator.comparing(TideMeasurement::getMeasuredAt));
            int year = Integer.parseInt(monthKey.substring(0,4));
            int monthNumber = Integer.parseInt(monthKey.substring(5));
            LocalDateTime expected = LocalDate.of(year, monthNumber, 1).atStartOfDay();
            for (int i=0; i<parsed.size(); i++) {
                if (!parsed.get(i).getMeasuredAt().equals(expected.plusMinutes(i*30L))) {
                    throw new IllegalArgumentException("A tábua deve ter medições consecutivas de 30 em 30 minutos, sem lacunas ou duplicidades.");
                }
            }
            controller.replaceTides(monthKey,parsed);refresh();Ui.info("Tábua importada","As 1.440 medições foram carregadas em memória.");
        }catch(Exception ex){Ui.error(ex.getMessage());}
    }
    private LocalDate parseDate(String value){try{return LocalDate.parse(value);}catch(Exception ignored){return LocalDate.parse(value,DateTimeFormatter.ofPattern("dd/MM/yyyy"));}}
    private double parseNumber(String value){return Double.parseDouble(value.trim().replace(" ","").replace(",","."));}
}
