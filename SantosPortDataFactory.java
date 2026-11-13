package br.edu.unirv.portos.service;

import br.edu.unirv.portos.model.*;
import br.edu.unirv.portos.repository.InMemoryPortRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fábrica de dados demonstrativos para execução sem backend. */
public final class SantosPortDataFactory {
    private SantosPortDataFactory() { }
    public static InMemoryPortRepository createRepository() {
        InMemoryPortRepository repo = new InMemoryPortRepository();
        repo.getBerths().addAll(createBerths());
        repo.getVessels().addAll(createVessels());
        repo.getCargoProducts().addAll(createCargoProducts());
        repo.getSchedules().addAll(createSchedules(repo.getBerths(), repo.getVessels(), repo.getCargoProducts()));
        repo.getTidesByMonth().put("2026-11", javafx.collections.FXCollections.observableArrayList(createTides(2026, 11, 8)));
        repo.getTidesByMonth().put("2026-09", javafx.collections.FXCollections.observableArrayList(createTides(2026, 9, 5)));
        repo.getTidesByMonth().put("2026-06", javafx.collections.FXCollections.observableArrayList(createTides(2026, 6, 11)));
        return repo;
    }
    private static List<Berth> createBerths() {
        List<Berth> result = new ArrayList<>();
        String[] special = {
            "Saboó 01", "Valongo 02", "Paquetá 03", "Outeirinhos 04", "Corredor de Exportação de Grãos",
            "TEAG Grãos 01", "TGG Grãos 02", "Cutrale 03", "Terminal Açucareiro 04", "Granéis Vegetais 05",
            "BTP 1 - Contêineres", "BTP 2 - Contêineres", "Tecon Santos Brasil 1", "Tecon Santos Brasil 2", "Tecon Santos Brasil 3",
            "Macuco 16", "Ponta da Praia 17", "Alemoa 18", "Santos Brasil 19", "DP World 20"
        };
        String[] sectors = {"Saboó", "Valongo", "Paquetá", "Outeirinhos", "Corredor de Exportação", "Margem Esquerda", "Alemoa", "Macuco", "Ponta da Praia"};
        CargoProfile[] profiles = {CargoProfile.GENERAL_CARGO, CargoProfile.GENERAL_CARGO, CargoProfile.GENERAL_CARGO,
                CargoProfile.VEGETABLE_BULK, CargoProfile.VEGETABLE_BULK, CargoProfile.VEGETABLE_BULK,
                CargoProfile.VEGETABLE_BULK, CargoProfile.CITRUS_JUICE, CargoProfile.VEGETABLE_BULK,
                CargoProfile.VEGETABLE_BULK, CargoProfile.CONTAINERS, CargoProfile.CONTAINERS,
                CargoProfile.CONTAINERS, CargoProfile.CONTAINERS, CargoProfile.CONTAINERS,
                CargoProfile.PULP, CargoProfile.STEEL, CargoProfile.CHEMICAL_LIQUIDS, CargoProfile.CONTAINERS, CargoProfile.CONTAINERS};
        for (int i = 1; i <= 60; i++) {
            String name = i <= special.length ? special[i-1] : String.format(Locale.forLanguageTag("pt-BR"), "Berço %02d - %s", i, sectors[(i-1) % sectors.length]);
            CargoProfile profile = i <= profiles.length ? profiles[i-1] : CargoProfile.values()[(i*3) % CargoProfile.values().length];
            String sector = sectors[(i-1) % sectors.length];
            if (i == 59) { name = "Concais - Passageiros"; profile = CargoProfile.PASSENGERS; sector = "Ponta da Praia"; }
            if (i == 60) { name = "Base Offshore Pré-Sal"; profile = CargoProfile.OFFSHORE; sector = "Margem Esquerda"; }
            double draft = i >= 11 && i <= 15 ? 15.0 : 12.8 + ((i * 7) % 19) / 10.0;
            double dailyRate = 6_000 + (i % 5) * 250;
            if (i == 5) dailyRate = 8_500; // comporta os 66.500 t do MV Star Gran 19 em oito dias
            if (i >= 11 && i <= 15) dailyRate = 12_000; // terminais de contêineres
            result.add(new Berth("B" + i, String.format("BR-%02d", i), name, sector, draft, profile, dailyRate));
        }
        return result;
    }
    private static List<RegisteredVessel> createVessels() {
        List<RegisteredVessel> list = new ArrayList<>();
        String[] names = {"MV Star Gran 19", "Maersk Triple E", "MSC Gülsün", "CMA CGM Jacques Saadé", "MV Sol do Atlântico",
                "Suzano Celulose I", "Transpetro Almirante", "Petrobras 1001", "Costa Diadema", "MSC Seaview",
                "Graneleiro Santos Pioneer", "Panamax Aurora", "Kamsarmax Brasil", "Tecon Express", "TGG Exportador"};
        String[] classes = {"Panamax", "Post-Panamax", "Handymax", "Suezmax", "Kamsarmax"};
        String[] products = {"Soja em Grãos", "Contêineres", "Celulose", "Diesel", "Açúcar VHP", "Fertilizantes KCl", "Milho",
                "Bauxita", "Bobinas de Aço", "Suco de Laranja", "Carga Geral Diversa", "Passageiros", "Equipamentos Offshore"};
        for (int i = 0; i < 160; i++) {
            String name = i < names.length ? names[i] : String.format("Navio Mercante %03d", i + 1);
            String imo = String.format("IMO-%07d", 9000000 + i * 37 + 1);
            String product = (i >= 1 && i <= 3) ? "Contêineres" : products[i % products.length];
            double draft = 11.2 + (i % 40) * 0.11;
            if (i == 1) draft = 14.6;
            if (i == 2) draft = 14.8;
            if (i == 3) draft = 14.4;
            long dwt = 28_000L + (i % 75) * 1_100L;
            list.add(new RegisteredVessel("V" + (i+1), imo, name, classes[i % classes.length], draft, dwt,
                    product, products[(i+2) % products.length], products[i % products.length]));
        }
        return list;
    }
    private static List<CargoProduct> createCargoProducts() {
        String[][] data = {
            {"CRG-01","Soja em Grãos","MIXED","VEGETABLE_BULK","30000"},
            {"CRG-02","Milho","LOAD","VEGETABLE_BULK","28000"},
            {"CRG-03","Açúcar VHP","LOAD","VEGETABLE_BULK","22000"},
            {"CRG-04","Celulose","LOAD","PULP","18500"},
            {"CRG-05","Fertilizantes KCl","DISCHARGE","FERTILIZERS","16000"},
            {"CRG-06","Contêineres","MIXED","CONTAINERS","25000"},
            {"CRG-07","Diesel","DISCHARGE","CHEMICAL_LIQUIDS","12000"},
            {"CRG-08","Bauxita","DISCHARGE","MINERAL_BULK","21000"},
            {"CRG-09","Bobinas de Aço","MIXED","STEEL","9000"},
            {"CRG-10","Suco de Laranja","LOAD","CITRUS_JUICE","8500"},
            {"CRG-11","Carga Geral Diversa","MIXED","GENERAL_CARGO","7000"},
            {"CRG-12","Petróleo e Derivados","DISCHARGE","CHEMICAL_LIQUIDS","30000"},
            {"CRG-13","Veículos","LOAD","GENERAL_CARGO","4500"},
            {"CRG-14","Passageiros","MIXED","PASSENGERS","1200"},
            {"CRG-15","Equipamentos Offshore","MIXED","OFFSHORE","5000"}
        };
        List<CargoProduct> products = new ArrayList<>();
        for (int i=0;i<data.length;i++) products.add(new CargoProduct("C"+(i+1), data[i][0], data[i][1],
                CargoDirection.valueOf(data[i][2]), CargoProfile.valueOf(data[i][3]), Double.parseDouble(data[i][4])));
        return products;
    }
    private static List<ShipSchedule> createSchedules(List<Berth> berths, List<RegisteredVessel> vessels,
                                                       List<CargoProduct> cargoProducts) {
        List<ShipSchedule> list = new ArrayList<>();
        var profileByProduct = cargoProducts.stream().collect(java.util.stream.Collectors.toMap(
                CargoProduct::getName, CargoProduct::getProfile));
        var compatibleVessels = new java.util.EnumMap<CargoProfile, List<RegisteredVessel>>(CargoProfile.class);
        for (CargoProfile profile : CargoProfile.values()) {
            List<RegisteredVessel> candidates = vessels.stream()
                    .filter(v -> v.getMaxDraft() < 13.5)
                    .filter(v -> profileByProduct.get(v.getStandardProduct()) == profile)
                    .toList();
            compatibleVessels.put(profile, candidates);
        }

        int idx = 0;
        for (int b = 0; b < berths.size(); b++) {
            List<int[]> ranges;
            if (b >= 10 && b <= 14) {
                // Navios de grande porte ficam fora dos dias críticos 08, 14 e 22.
                ranges = List.of(new int[]{1, 7}, new int[]{9, 13}, new int[]{15, 21});
            } else if (b < 10) ranges = List.of(new int[]{1, 9}, new int[]{10, 19}, new int[]{20, 29});
            else if (b < 33) ranges = List.of(new int[]{1, 9}, new int[]{10, 18}, new int[]{19, 28});
            else ranges = List.of(new int[]{1, 13}, new int[]{15, 28});

            Berth berth = berths.get(b);
            for (int[] range : ranges) {
                double tons = idx == 12 ? 66_500 : (idx < 152 ? 44_000 + (idx % 7) * 250 : 28_000);
                RegisteredVessel vessel;
                if (b == 4 && range[0] == 1) {
                    vessel = vessels.get(0); // MV Star Gran 19 no Berço 05
                } else if (b >= 10 && b <= 14) {
                    vessel = vessels.get(1 + (idx % 3)); // Maersk, MSC e CMA CGM nos berços de contêineres
                } else {
                    List<RegisteredVessel> candidates = compatibleVessels.getOrDefault(berth.getCargoProfile(), List.of())
                            .stream().filter(v -> v.getMaxDraft() <= berth.getMaxDraft()).toList();
                    if (candidates.isEmpty()) {
                        throw new IllegalStateException("Não há navio mocado compatível com o perfil do berço " + berth.getCode());
                    }
                    vessel = candidates.get(idx % candidates.size());
                }
                ScheduleStatus status = ScheduleStatus.values()[idx % ScheduleStatus.values().length];
                list.add(new ShipSchedule("S" + (idx + 1), berth.getCode(), vessel.getImo(),
                        range[0], range[1], tons, status));
                idx++;
            }
        }
        return list;
    }
    private static List<TideMeasurement> createTides(int year, int month, int phaseShift) {
        List<TideMeasurement> list = new ArrayList<>(1440);
        LocalDate first = LocalDate.of(year, month, 1);
        for (int day=0;day<first.lengthOfMonth();day++) {
            LocalDate date = first.plusDays(day);
            int calendarDay = day + 1;
            boolean criticalDay = calendarDay == phaseShift || calendarDay == phaseShift + 6 || calendarDay == phaseShift + 14;
            for (int slot=0;slot<48;slot++) {
                double phase = (2.0 * Math.PI * slot / 24.8) + (day * 0.17);
                double amplitude = criticalDay ? 0.93 : 0.48;
                double height = 1.02 + amplitude * Math.sin(phase);
                height = Math.max(0.12, Math.min(1.82, height));
                list.add(new TideMeasurement(date.atStartOfDay().plusMinutes(slot * 30L), Math.round(height * 100.0) / 100.0));
            }
        }
        return list;
    }
}
