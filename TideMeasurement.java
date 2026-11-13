package br.edu.unirv.portos.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class TideMeasurement {
    public static final double CRITICAL_LEVEL_METERS = 0.4;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final LocalDateTime measuredAt;
    private final double heightMeters;
    public TideMeasurement(LocalDateTime measuredAt, double heightMeters) {
        this.measuredAt = Objects.requireNonNull(measuredAt);
        if (!Double.isFinite(heightMeters)) throw new IllegalArgumentException("Altura de maré inválida.");
        this.heightMeters = heightMeters;
    }
    public LocalDateTime getMeasuredAt() { return measuredAt; }
    public double getHeightMeters() { return heightMeters; }
    public String getDateLabel() { return measuredAt.format(DATE_FORMAT); }
    public String getTimeLabel() { return measuredAt.format(TIME_FORMAT); }
    public String getCondition() { return isCritical() ? "Maré Baixa" : "Regular"; }
    public boolean isCritical() { return heightMeters <= CRITICAL_LEVEL_METERS; }
}
