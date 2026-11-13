package br.edu.unirv.portos.model;

import java.util.Arrays;
import java.util.List;

public enum CargoProfile {
    VEGETABLE_BULK("Granel Vegetal"),
    CONTAINERS("Contêineres"),
    PULP("Celulose"),
    CHEMICAL_LIQUIDS("Líquidos Químicos"),
    STEEL("Siderurgia"),
    MINERAL_BULK("Granéis Minerais"),
    CITRUS_JUICE("Sucos Cítricos"),
    GENERAL_CARGO("Carga Geral"),
    PASSENGERS("Passageiros"),
    OFFSHORE("Offshore"),
    FERTILIZERS("Fertilizantes");

    private final String label;
    CargoProfile(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
    public static List<CargoProfile> all() { return List.of(values()); }
    public static CargoProfile fromLabel(String value) {
        return Arrays.stream(values()).filter(v -> v.label.equalsIgnoreCase(value)).findFirst().orElse(GENERAL_CARGO);
    }
}
