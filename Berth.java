package br.edu.unirv.portos.model;

import java.util.Objects;

public final class Berth {
    private final String id;
    private String code;
    private String name;
    private String sector;
    private double maxDraft;
    private CargoProfile cargoProfile;
    private double dailyRateTons;

    public Berth(String id, String code, String name, String sector, double maxDraft,
                 CargoProfile cargoProfile, double dailyRateTons) {
        this.id = Objects.requireNonNull(id);
        update(code, name, sector, maxDraft, cargoProfile, dailyRateTons);
    }
    public void update(String code, String name, String sector, double maxDraft,
                       CargoProfile cargoProfile, double dailyRateTons) {
        String newCode = requireText(code, "Código");
        String newName = requireText(name, "Nome");
        String newSector = requireText(sector, "Setor");
        if (!Double.isFinite(maxDraft) || maxDraft <= 0) throw new IllegalArgumentException("Calado deve ser maior que zero.");
        if (!Double.isFinite(dailyRateTons) || dailyRateTons <= 0) throw new IllegalArgumentException("Taxa diária deve ser maior que zero.");
        CargoProfile newProfile = Objects.requireNonNull(cargoProfile);
        this.code = newCode;
        this.name = newName;
        this.sector = newSector;
        this.maxDraft = maxDraft;
        this.cargoProfile = newProfile;
        this.dailyRateTons = dailyRateTons;
    }
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " é obrigatório.");
        return value.trim();
    }
    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public double getMaxDraft() { return maxDraft; }
    public CargoProfile getCargoProfile() { return cargoProfile; }
    public double getDailyRateTons() { return dailyRateTons; }
    @Override public String toString() { return code + " — " + name; }
}
