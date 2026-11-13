package br.edu.unirv.portos.model;

import java.util.Objects;

public final class CargoProduct {
    private final String id;
    private String code;
    private String name;
    private CargoDirection direction;
    private CargoProfile profile;
    private double tonsPerDay;

    public CargoProduct(String id, String code, String name, CargoDirection direction,
                        CargoProfile profile, double tonsPerDay) {
        this.id = Objects.requireNonNull(id);
        update(code, name, direction, profile, tonsPerDay);
    }
    public void update(String code, String name, CargoDirection direction, CargoProfile profile, double tonsPerDay) {
        String newCode = required(code, "Código");
        String newName = required(name, "Produto");
        CargoDirection newDirection = Objects.requireNonNull(direction);
        CargoProfile newProfile = Objects.requireNonNull(profile);
        if (!Double.isFinite(tonsPerDay) || tonsPerDay <= 0) throw new IllegalArgumentException("Taxa diária deve ser maior que zero.");
        this.code = newCode;
        this.name = newName;
        this.direction = newDirection;
        this.profile = newProfile;
        this.tonsPerDay = tonsPerDay;
    }
    private static String required(String value, String label) { if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " é obrigatório."); return value.trim(); }
    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public CargoDirection getDirection() { return direction; }
    public CargoProfile getProfile() { return profile; }
    public double getTonsPerDay() { return tonsPerDay; }
    @Override public String toString() { return name; }
}
