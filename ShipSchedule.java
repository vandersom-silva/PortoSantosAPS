package br.edu.unirv.portos.model;

import java.util.Objects;

public final class ShipSchedule {
    private final String id;
    private String berthCode;
    private final String vesselImo;
    private int startDay;
    private int endDay;
    private final double tonnage;
    private final ScheduleStatus status;
    public ShipSchedule(String id, String berthCode, String vesselImo, int startDay, int endDay,
                        double tonnage, ScheduleStatus status) {
        this.id = Objects.requireNonNull(id);
        this.berthCode = Objects.requireNonNull(berthCode);
        this.vesselImo = Objects.requireNonNull(vesselImo);
        if (startDay < 1 || endDay > 31 || endDay <= startDay) throw new IllegalArgumentException("Intervalo de atracação inválido.");
        if (!Double.isFinite(tonnage) || tonnage <= 0) throw new IllegalArgumentException("Tonelagem deve ser maior que zero.");
        this.startDay = startDay;
        this.endDay = endDay;
        this.tonnage = tonnage;
        this.status = Objects.requireNonNull(status);
    }
    public String getId() { return id; }
    public String getBerthCode() { return berthCode; }
    public String getVesselImo() { return vesselImo; }
    public int getStartDay() { return startDay; }
    public int getEndDay() { return endDay; }
    public double getTonnage() { return tonnage; }
    public ScheduleStatus getStatus() { return status; }
    public int getCalendarDays() { return endDay - startDay; }
    public void moveTo(String berthCode, int startDay, int endDay) {
        if (startDay < 1 || endDay > 31 || endDay <= startDay) throw new IllegalArgumentException("Intervalo de atracação inválido.");
        this.berthCode = Objects.requireNonNull(berthCode);
        this.startDay = startDay;
        this.endDay = endDay;
    }
    public int getEstimatedDurationDays(Berth berth) { return Math.max(1, (int)Math.ceil(tonnage / berth.getDailyRateTons())); }
}
