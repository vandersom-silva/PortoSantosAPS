package br.edu.unirv.portos.repository;

import br.edu.unirv.portos.model.*;
import javafx.collections.ObservableList;
import java.util.Map;

/** Contrato de acesso aos dados. Pode receber futuramente uma implementação REST. */
public interface PortRepository {
    ObservableList<Berth> getBerths();
    ObservableList<RegisteredVessel> getVessels();
    ObservableList<CargoProduct> getCargoProducts();
    ObservableList<ShipSchedule> getSchedules();
    Map<String, ObservableList<TideMeasurement>> getTidesByMonth();
}
