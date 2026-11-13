package br.edu.unirv.portos;

import br.edu.unirv.portos.controller.PortController;
import br.edu.unirv.portos.repository.InMemoryPortRepository;
import br.edu.unirv.portos.service.PortService;
import br.edu.unirv.portos.service.SantosPortDataFactory;
import br.edu.unirv.portos.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class PortosApplication extends Application {
    @Override public void start(Stage stage) {
        InMemoryPortRepository repository = SantosPortDataFactory.createRepository();
        PortController controller = new PortController(new PortService(repository));
        MainView root = new MainView(controller);
        Scene scene = new Scene(root, 1480, 940);
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle("Gêmeo Digital do Porto de Santos | APS - FIPS");
        stage.setMinWidth(1180); stage.setMinHeight(780);
        stage.setScene(scene); stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
