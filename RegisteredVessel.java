package br.edu.unirv.portos.model;

import java.util.Objects;

public final class RegisteredVessel {
    private final String id;
    private String imo;
    private String name;
    private String vesselClass;
    private double maxDraft;
    private long dwt;
    private String standardProduct;
    private String dischargeProduct;
    private String loadProduct;

    public RegisteredVessel(String id, String imo, String name, String vesselClass, double maxDraft,
                            long dwt, String standardProduct, String dischargeProduct, String loadProduct) {
        this.id = Objects.requireNonNull(id);
        update(imo, name, vesselClass, maxDraft, dwt, standardProduct, dischargeProduct, loadProduct);
    }
    public void update(String imo, String name, String vesselClass, double maxDraft, long dwt,
                       String standardProduct, String dischargeProduct, String loadProduct) {
        if (imo == null || !imo.trim().matches("(?i)IMO-\\d{7}")) throw new IllegalArgumentException("IMO inválido. Use IMO- seguido de sete dígitos.");
        String newName = required(name, "Nome");
        String newClass = required(vesselClass, "Classe");
        if (!Double.isFinite(maxDraft) || maxDraft <= 0) throw new IllegalArgumentException("Calado deve ser maior que zero.");
        if (dwt <= 0) throw new IllegalArgumentException("DWT deve ser maior que zero.");
        this.imo = imo.trim().toUpperCase();
        this.name = newName;
        this.vesselClass = newClass;
        this.maxDraft = maxDraft;
        this.dwt = dwt;
        this.standardProduct = emptyToDash(standardProduct);
        this.dischargeProduct = emptyToDash(dischargeProduct);
        this.loadProduct = emptyToDash(loadProduct);
    }
    private static String required(String v, String f) { if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " é obrigatório."); return v.trim(); }
    private static String emptyToDash(String v) { return v == null || v.isBlank() ? "—" : v.trim(); }
    public String getId() { return id; }
    public String getImo() { return imo; }
    public String getName() { return name; }
    public String getVesselClass() { return vesselClass; }
    public double getMaxDraft() { return maxDraft; }
    public long getDwt() { return dwt; }
    public String getStandardProduct() { return standardProduct; }
    public String getDischargeProduct() { return dischargeProduct; }
    public String getLoadProduct() { return loadProduct; }
    @Override public String toString() { return name + " (" + imo + ")"; }
}
