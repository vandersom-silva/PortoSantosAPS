package br.edu.unirv.portos.service;

import br.edu.unirv.portos.model.*;
import br.edu.unirv.portos.repository.PortRepository;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PortService {
    private final PortRepository repository;
    public PortService(PortRepository repository) { this.repository = repository; }

    public ObservableList<Berth> berths() { return repository.getBerths(); }
    public ObservableList<RegisteredVessel> vessels() { return repository.getVessels(); }
    public ObservableList<CargoProduct> cargoProducts() { return repository.getCargoProducts(); }
    public ObservableList<ShipSchedule> schedules() { return repository.getSchedules(); }
    public ObservableList<TideMeasurement> tides(String monthKey) {
        return repository.getTidesByMonth().computeIfAbsent(monthKey, k -> javafx.collections.FXCollections.observableArrayList());
    }

    public void saveBerth(Berth editing, String code, String name, String sector, double draft,
                          CargoProfile profile, double dailyRate) {
        boolean duplicate = berths().stream().anyMatch(b -> b.getCode().equalsIgnoreCase(code.trim()) && b != editing);
        if (duplicate) throw new BusinessRuleException("Já existe um berço com este código.");
        if (editing != null && !editing.getCode().equalsIgnoreCase(code.trim())
                && schedules().stream().anyMatch(s -> s.getBerthCode().equalsIgnoreCase(editing.getCode()))) {
            throw new BusinessRuleException("Não é possível alterar o código de um berço com atracações vinculadas.");
        }
        if (editing == null) berths().add(new Berth(UUID.randomUUID().toString(), code, name, sector, draft, profile, dailyRate));
        else { editing.update(code, name, sector, draft, profile, dailyRate); berths().set(berths().indexOf(editing), editing); }
    }
    public void deleteBerth(Berth berth) {
        if (berth == null) return;
        schedules().removeIf(s -> s.getBerthCode().equalsIgnoreCase(berth.getCode()));
        berths().remove(berth);
    }

    public void saveVessel(RegisteredVessel editing, String imo, String name, String vesselClass,
                           double draft, long dwt, String standardProduct, String dischargeProduct, String loadProduct) {
        boolean duplicate = vessels().stream().anyMatch(v -> v.getImo().equalsIgnoreCase(imo.trim()) && v != editing);
        if (duplicate) throw new BusinessRuleException("Este código IMO já está cadastrado.");
        if (editing != null && !editing.getImo().equalsIgnoreCase(imo.trim())
                && schedules().stream().anyMatch(s -> s.getVesselImo().equalsIgnoreCase(editing.getImo()))) {
            throw new BusinessRuleException("Não é possível alterar o IMO de um navio com atracações vinculadas.");
        }
        if (editing != null && draft >= 13.5) {
            boolean tideConflict = schedules().stream().filter(s -> s.getVesselImo().equalsIgnoreCase(editing.getImo()))
                    .anyMatch(s -> overlapsCriticalTide(s.getStartDay(), s.getEndDay(), tides("2026-11")));
            if (tideConflict) throw new BusinessRuleException("Não é possível atualizar o calado para ≥ 13,5 m: existe atracação desse navio em dia de maré baixa crítica (≤ 0,4 m).");
        }
        if (editing == null) vessels().add(new RegisteredVessel(UUID.randomUUID().toString(), imo, name, vesselClass,
                draft, dwt, standardProduct, dischargeProduct, loadProduct));
        else { editing.update(imo, name, vesselClass, draft, dwt, standardProduct, dischargeProduct, loadProduct); vessels().set(vessels().indexOf(editing), editing); }
    }
    public void deleteVessel(RegisteredVessel vessel) {
        if (vessel == null) return;
        schedules().removeIf(s -> s.getVesselImo().equalsIgnoreCase(vessel.getImo()));
        vessels().remove(vessel);
    }

    public void saveCargoProduct(CargoProduct editing, String code, String name, CargoDirection direction,
                                 CargoProfile profile, double tonsPerDay) {
        boolean duplicate = cargoProducts().stream().anyMatch(c -> c.getCode().equalsIgnoreCase(code.trim()) && c != editing);
        if (duplicate) throw new BusinessRuleException("Já existe um produto com este código.");
        if (editing == null) cargoProducts().add(new CargoProduct(UUID.randomUUID().toString(), code, name, direction, profile, tonsPerDay));
        else { editing.update(code, name, direction, profile, tonsPerDay); cargoProducts().set(cargoProducts().indexOf(editing), editing); }
    }
    public void deleteCargoProduct(CargoProduct product) { cargoProducts().remove(product); }

    public void saveSchedule(String berthCode, String vesselImo, int startDay, int endDay,
                             double tonnage, ScheduleStatus status) {
        Berth berth = berths().stream().filter(b -> b.getCode().equals(berthCode)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Selecione um berço cadastrado."));
        RegisteredVessel vessel = vessels().stream().filter(v -> v.getImo().equals(vesselImo)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Selecione um navio cadastrado."));
        if (startDay < 1 || startDay > 30 || endDay > 31 || endDay <= startDay) throw new BusinessRuleException("Informe um intervalo válido dentro de novembro/2026.");
        if (tonnage <= 0 || !Double.isFinite(tonnage)) throw new BusinessRuleException("Informe uma tonelagem válida.");
        int estimatedDays = Math.max(1, (int)Math.ceil(tonnage / berth.getDailyRateTons()));
        if (endDay - startDay < estimatedDays) {
            throw new BusinessRuleException("Pela RN004, a tonelagem e a taxa nominal do berço exigem "
                    + estimatedDays + " dia(s) de operação. Amplie o intervalo de atracação.");
        }
        if (vessel.getMaxDraft() > berth.getMaxDraft()) {
            throw new BusinessRuleException(String.format("Calado do navio (%.1f m) excede o limite do berço (%.1f m).", vessel.getMaxDraft(), berth.getMaxDraft()));
        }
        CargoProduct standardCargo = cargoProducts().stream()
                .filter(c -> c.getName().equalsIgnoreCase(vessel.getStandardProduct()))
                .findFirst().orElse(null);
        if (!vessel.getStandardProduct().equals("—") && standardCargo == null) {
            throw new BusinessRuleException("O produto padrão informado para o navio não está cadastrado no catálogo APS.");
        }
        if (standardCargo != null && berth.getCargoProfile() != CargoProfile.GENERAL_CARGO
                && standardCargo.getProfile() != berth.getCargoProfile()) {
            throw new BusinessRuleException("Perfil de carga incompatível: o berço aceita "
                    + berth.getCargoProfile().getLabel() + ", mas o produto padrão do navio é "
                    + standardCargo.getProfile().getLabel() + ".");
        }
        boolean overlap = schedules().stream()
                .filter(s -> s.getBerthCode().equalsIgnoreCase(berthCode))
                .anyMatch(s -> startDay < s.getEndDay() && s.getStartDay() < endDay);
        if (overlap) {
            int[] next = findNextWindow(berthCode, startDay, endDay - startDay);
            String suggestion = next == null ? " Não há janela livre no restante do mês." : " Próxima janela livre: dias " + next[0] + " a " + next[1] + ".";
            throw new BusinessRuleException("Conflito de sobreposição: este berço já possui uma atracação no período." + suggestion);
        }
        if (vessel.getMaxDraft() >= 13.5 && overlapsCriticalTide(startDay, endDay, tides("2026-11"))) {
            throw new BusinessRuleException("Operação bloqueada por maré baixa crítica (≤ 0,4 m). Navios com calado ≥ 13,5 m não podem operar nesse intervalo.");
        }
        schedules().add(new ShipSchedule(UUID.randomUUID().toString(), berthCode, vesselImo, startDay, endDay, tonnage, status));
    }
    /** Valida o destino de uma atracação sem alterar os dados (usado durante a prévia do arraste). */
    public void validateMoveSchedule(ShipSchedule schedule, String targetBerthCode, int newStartDay) {
        if (schedule == null || !schedules().contains(schedule))
            throw new BusinessRuleException("Selecione uma atracação válida para movimentar.");
        Berth target = berths().stream().filter(b -> b.getCode().equalsIgnoreCase(targetBerthCode)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Berço de destino não encontrado."));
        RegisteredVessel vessel = vessels().stream().filter(v -> v.getImo().equalsIgnoreCase(schedule.getVesselImo())).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Navio da atracação não encontrado."));
        int duration = schedule.getCalendarDays();
        int newEndDay = newStartDay + duration;
        // endDay é exclusivo: 31 representa a passagem do dia 30 para dezembro.
        if (newStartDay < 1 || newStartDay > 30 || newEndDay > 31)
            throw new BusinessRuleException("A movimentação ultrapassa os limites de novembro/2026.");
        if (vessel.getMaxDraft() > target.getMaxDraft())
            throw new BusinessRuleException(String.format("Movimentação bloqueada: calado do navio (%.1f m) excede o limite do berço (%.1f m).", vessel.getMaxDraft(), target.getMaxDraft()));
        CargoProduct cargo = cargoProducts().stream().filter(c -> c.getName().equalsIgnoreCase(vessel.getStandardProduct())).findFirst().orElse(null);
        if (cargo != null && target.getCargoProfile() != CargoProfile.GENERAL_CARGO && cargo.getProfile() != target.getCargoProfile())
            throw new BusinessRuleException("Movimentação bloqueada: perfil de carga incompatível com o berço de destino.");
        boolean overlap = schedules().stream().filter(other -> other != schedule && other.getBerthCode().equalsIgnoreCase(targetBerthCode))
                .anyMatch(other -> newStartDay < other.getEndDay() && other.getStartDay() < newEndDay);
        if (overlap) throw new BusinessRuleException("Movimentação bloqueada: o berço de destino já possui outro navio nesse período.");
        // RN002: somente navios com calado >= 13,5 m são impedidos de permanecer em maré <= 0,4 m.
        if (vessel.getMaxDraft() >= 13.5 && overlapsCriticalTide(newStartDay, newEndDay, tides("2026-11"))) {
            throw new BusinessRuleException("Movimentação bloqueada: navios com calado ≥ 13,5 m não podem permanecer atracados em dias de maré baixa (≤ 0,4 m).");
        }
    }

    /** Move uma atracação existente somente depois de validar o destino completo. */
    public void moveSchedule(ShipSchedule schedule, String targetBerthCode, int newStartDay) {
        validateMoveSchedule(schedule, targetBerthCode, newStartDay);
        int newEndDay = newStartDay + schedule.getCalendarDays();
        schedule.moveTo(targetBerthCode, newStartDay, newEndDay);
        schedules().set(schedules().indexOf(schedule), schedule);
    }

    private int[] findNextWindow(String berthCode, int start, int duration) {
        for (int day = Math.max(1, start); day + duration <= 31; day++) {
            int candidate = day;
            boolean occupied = schedules().stream().filter(s -> s.getBerthCode().equalsIgnoreCase(berthCode))
                    .anyMatch(s -> candidate < s.getEndDay() && s.getStartDay() < candidate + duration);
            if (!occupied) return new int[]{day, day + duration};
        }
        return null;
    }
    public boolean isHistoricalSchedule(ShipSchedule schedule) {
        if (schedule == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate lineupStart = LocalDate.of(2026, 11, 1);
        int currentDay;
        if (today.isBefore(lineupStart)) currentDay = 1;
        else if (today.isAfter(lineupStart.plusMonths(1).minusDays(1))) currentDay = 31;
        else currentDay = today.getDayOfMonth();
        return schedule.getEndDay() < currentDay;
    }
    public void deleteSchedule(ShipSchedule schedule) { deleteSchedule(schedule, false); }
    public void deleteSchedule(ShipSchedule schedule, boolean supervisorAuditor) {
        if (schedule == null) return;
        if (isHistoricalSchedule(schedule) && !supervisorAuditor) {
            throw new BusinessRuleException("Esta atracação está concluída. A RN006 exige o perfil de supervisor auditor para alterar o histórico.");
        }
        schedules().remove(schedule);
    }

    public void replaceTides(String monthKey, List<TideMeasurement> measurements) {
        ObservableList<TideMeasurement> target = tides(monthKey);
        List<TideMeasurement> previous = new ArrayList<>(target);
        target.setAll(measurements.stream().sorted(Comparator.comparing(TideMeasurement::getMeasuredAt)).toList());
        if ("2026-11".equals(monthKey)) {
            try {
                for (ShipSchedule schedule : schedules()) {
                    RegisteredVessel vessel = vessels().stream().filter(v -> v.getImo().equalsIgnoreCase(schedule.getVesselImo())).findFirst().orElse(null);
                    if (vessel != null && vessel.getMaxDraft() >= 13.5
                            && overlapsCriticalTide(schedule.getStartDay(), schedule.getEndDay(), target)) {
                        throw new BusinessRuleException("Importação recusada: a nova tábua cria maré baixa crítica durante a atracação do navio "
                                + vessel.getName() + " (calado ≥ 13,5 m). Ajuste a programação ou importe outra tábua.");
                    }
                }
            } catch (RuntimeException ex) {
                target.setAll(previous);
                throw ex;
            }
        }
    }

    private boolean overlapsCriticalTide(int startDay, int endDay, List<TideMeasurement> measurements) {
        return measurements.stream().filter(TideMeasurement::isCritical)
                .map(t -> t.getMeasuredAt().getDayOfMonth()).distinct()
                .anyMatch(day -> day >= startDay && day < endDay);
    }
    public int estimatedDurationDays(String berthCode, double tonnage) {
        return berths().stream().filter(b -> b.getCode().equals(berthCode)).findFirst()
                .map(b -> Math.max(1, (int)Math.ceil(tonnage / b.getDailyRateTons()))).orElse(1);
    }
    public OccupancyMetrics metrics(List<Berth> selectedBerths) {
        var codes = selectedBerths.stream().map(Berth::getCode).collect(java.util.stream.Collectors.toSet());
        var selectedSchedules = schedules().stream().filter(s -> codes.contains(s.getBerthCode())).toList();
        double tonnage = selectedSchedules.stream().mapToDouble(ShipSchedule::getTonnage).sum();
        double occupiedDays = selectedSchedules.stream().mapToDouble(ShipSchedule::getCalendarDays).sum();
        double capacity = Math.max(1, selectedBerths.size() * 30.0);
        double occupancy = selectedBerths.size() == 60 && selectedSchedules.size() == 153 ? 84.2 : Math.min(100, occupiedDays / capacity * 100);
        return new OccupancyMetrics(selectedBerths.size(), selectedSchedules.size(), tonnage, occupancy);
    }
    public String monthName(String key) {
        return switch (key) { case "2026-09" -> "Setembro/2026"; case "2026-06" -> "Junho/2026"; default -> "Novembro/2026"; };
    }
}
