package br.edu.unirv.portos.model;

public enum CargoDirection {
    DISCHARGE("↓ Descarga"), LOAD("↑ Carga"), MIXED("↓↑ Misto");
    private final String label;
    CargoDirection(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
