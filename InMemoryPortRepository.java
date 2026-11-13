package br.edu.unirv.portos.repository;

import br.edu.unirv.portos.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryPortRepository implements PortRepository {
    private final ObservableList<Berth> berths = FXCollections.observableArrayList();
    private final ObservableList<RegisteredVessel> vessels = FXCollections.observableArrayList();
    private final ObservableList<CargoProduct> cargoProducts = FXCollections.observableArrayList();
    private final ObservableList<ShipSchedule> schedules = FXCollections.observableArrayList();
    private final Map<String, ObservableList<TideMeasurement>> tidesByMonth = new LinkedHashMap<>();
    @Override public ObservableList<Berth> getBerths() { return berths; }
    @Override public ObservableList<RegisteredVessel> getVessels() { return vessels; }
    @Override public ObservableList<CargoProduct> getCargoProducts() { return cargoProducts; }
    @Override public ObservableList<ShipSchedule> getSchedules() { return schedules; }
    @Override public Map<String, ObservableList<TideMeasurement>> getTidesByMonth() { return tidesByMonth; }
}
