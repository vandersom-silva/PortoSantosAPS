package br.edu.unirv.portos.controller;

import br.edu.unirv.portos.model.*;
import br.edu.unirv.portos.service.PortService;
import javafx.collections.ObservableList;
import java.util.List;

public final class PortController {
    private final PortService service;
    public PortController(PortService service) { this.service = service; }
    public ObservableList<Berth> berths() { return service.berths(); }
    public ObservableList<RegisteredVessel> vessels() { return service.vessels(); }
    public ObservableList<CargoProduct> cargoProducts() { return service.cargoProducts(); }
    public ObservableList<ShipSchedule> schedules() { return service.schedules(); }
    public ObservableList<TideMeasurement> tides(String month) { return service.tides(month); }
    public void saveBerth(Berth e,String c,String n,String s,double d,CargoProfile p,double r){service.saveBerth(e,c,n,s,d,p,r);}
    public void deleteBerth(Berth b){service.deleteBerth(b);}
    public void saveVessel(RegisteredVessel e,String i,String n,String c,double d,long w,String p,String down,String up){service.saveVessel(e,i,n,c,d,w,p,down,up);}
    public void deleteVessel(RegisteredVessel v){service.deleteVessel(v);}
    public void saveCargo(CargoProduct e,String c,String n,CargoDirection d,CargoProfile p,double r){service.saveCargoProduct(e,c,n,d,p,r);}
    public void deleteCargo(CargoProduct p){service.deleteCargoProduct(p);}
    public void saveSchedule(String b,String v,int a,int z,double t,ScheduleStatus s){service.saveSchedule(b,v,a,z,t,s);}
    public void deleteSchedule(ShipSchedule s){service.deleteSchedule(s);}
    public void validateMoveSchedule(ShipSchedule s, String berthCode, int startDay){service.validateMoveSchedule(s, berthCode, startDay);}
    public void moveSchedule(ShipSchedule s, String berthCode, int startDay){service.moveSchedule(s, berthCode, startDay);}
    public void deleteSchedule(ShipSchedule s, boolean supervisorAuditor){service.deleteSchedule(s,supervisorAuditor);}
    public boolean isHistoricalSchedule(ShipSchedule s){return service.isHistoricalSchedule(s);}
    public void replaceTides(String month,List<TideMeasurement> values){service.replaceTides(month,values);}
    public int estimatedDuration(String berth,double tons){return service.estimatedDurationDays(berth,tons);}
    public OccupancyMetrics metrics(List<Berth> berths){return service.metrics(berths);}
    public String monthName(String key){return service.monthName(key);}
}
