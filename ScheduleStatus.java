package br.edu.unirv.portos.model;

public enum ScheduleStatus {
    OPERATING("Operando / Prioridade", "#e74c3c"),
    PROGRAMMED("Programado", "#e67e22"),
    CONFIRMED("Confirmado", "#2ecc71");
    private final String label;
    private final String colorHex;
    ScheduleStatus(String label, String colorHex) { this.label = label; this.colorHex = colorHex; }
    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
    @Override public String toString() { return label; }
}
