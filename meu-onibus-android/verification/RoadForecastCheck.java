package br.com.meuonibus;
import java.util.*;
public final class RoadForecastCheck {
  static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
  static void close(double value,double expected,String message){check(Math.abs(value-expected)<0.00001,message+": "+value);}
  static Schedule.Event event(int stop,int minute,String destination,String vehicle){return new Schedule.Event(stop,minute,destination,vehicle,"estimado");}
  // Existing fixtures represent known bus itineraries, explicitly confirmed under the new contract.
  static RoadForecast.Segment segment(int from,int to,String destination,int minutes,boolean paired,double[][] path){return new RoadForecast.Segment(from,to,destination,destination,minutes,paired,path,null,true);}
  public static void main(String[] args){
    // A car shortcut 13 m away is not a bus itinerary; the real bus road is over 400 m away.
    Schedule.Route shortcut=new Schedule.Route("Tabatinga","",false);
    shortcut.events.add(event(0,376,"Rodoviária","A"));
    shortcut.roadSegments.add(new RoadForecast.Segment(0,1,"Rodoviária","Caminho de carro",21,false,new double[][]{{.000117,-.01},{.000117,.01}}));
    check(RoadForecast.predict(shortcut,0,0,376).isEmpty(),"Never snap a passenger to an unconfirmed car itinerary 13 m away");
    shortcut.roadSegments.add(segment(0,1,"Rodoviária",21,false,new double[][]{{.004,-.01},{.004,.01}}));
    check(RoadForecast.predict(shortcut,0,0,376).isEmpty(),"A confirmed bus road over 400 m away must not be reported as 13 m away");
    RoadForecast.Direction correct=RoadForecast.nearestConfirmed(shortcut,0,0);
    close(correct.snap.distanceMetres,444.779706578235,"Show the real distance to the confirmed bus road, excluding the 13 m car shortcut");
    // Bug caught: projecting onto the endpoints instead of the nearest point on the road.
    RoadForecast.Snap snap=RoadForecast.snap(.005,.011,new double[][]{{0,0},{0,.01},{.01,.01}});
    close(snap.lat,.005,"Projection latitude");close(snap.lon,.01,"Projection longitude");close(snap.fraction,.75,"Progress uses road length, not straight-line distance");
    close(RoadForecast.snap(0,-.02,new double[][]{{0,0},{0,.01}}).fraction,0,"Projection clamps before origin");
    close(RoadForecast.snap(0,.02,new double[][]{{0,0},{0,.01}}).fraction,1,"Projection clamps past destination");
    check(RoadForecast.snap(Double.NaN,0,new double[][]{{0,0},{0,.01}})==null,"Invalid GPS");
    check(RoadForecast.snap(0,0,new double[][]{{0,0},{0,0}})==null,"Degenerate road");
    // Map timing: first edge takes 5 seconds, second 15. Halfway through second => 62.5% of travel time.
    close(RoadForecast.snap(.005,.011,new double[][]{{0,0},{0,.01},{.01,.01}},new double[]{5,15}).fraction,.625,"Internet edge durations weight progress");
    close(RoadForecast.snap(.005,.011,new double[][]{{0,0},{0,.01},{.01,.01}},new double[]{Double.NaN,15}).fraction,.75,"Invalid timing falls back to distance");
    close(RoadForecast.snap(.005,.011,new double[][]{{0,0},{0,.01},{.01,.01}},new double[]{0,0}).fraction,.75,"Zero timing falls back to distance");
    close(RoadForecast.snap(.005,.011,new double[][]{{0,0},{0,.01},{.01,.01}},new double[]{5}).fraction,.75,"Mismatched timing falls back to distance");
    List<RoadForecast.Place> landmarks=new ArrayList<>();landmarks.add(new RoadForecast.Place("Escola",0,.006));landmarks.add(new RoadForecast.Place("Outro bairro",.01,.005));
    check(RoadForecast.nearestPlace(0,.005,landmarks).name.equals("Escola"),"Known place near the roadside");
    check(RoadForecast.nearestPlace(0,0,landmarks)==null,"No distant landmark is assigned");
    check(RoadForecast.nearestPlace(0,0,Collections.singletonList(new RoadForecast.Place("Dentro",.00134,0)))!=null,"Landmark within 150 metres");
    check(RoadForecast.nearestPlace(0,0,Collections.singletonList(new RoadForecast.Place("Fora",.00136,0)))==null,"Landmark outside 150 metres");
    // Worked timetable: departs 06:16, next stop 06:37, passenger halfway => 06:26:30.
    Schedule.Route route=new Schedule.Route("Tabatinga","",false);
    route.roadSegments.add(segment(0,1,"Rodoviária",21,true,new double[][]{{0,0},{0,.01}}));
    route.roadSegments.add(segment(1,0,"Tabatinga",21,true,new double[][]{{.0001,.01},{.0001,0}}));
    route.events.add(event(0,376,"Rodoviária","A"));route.events.add(event(1,377,"Outro destino","B"));route.events.add(event(1,397,"Tabatinga","A"));route.events.add(event(0,418,"Rodoviária","A"));
    List<RoadForecast.Direction> directions=RoadForecast.predict(route,0,.005,380);
    check(directions.size()==2,"Both sides of a nearby road must be retained");
    close(directions.get(0).future.get(0).minute,386.5,"Arrival interpolates between matched vehicle departures");
    check(directions.get(0).future.get(0).departed(380),"Already departed buses must not be discarded");
    check(!directions.get(1).future.get(0).departed(380),"Future departure is not reported as already left");
    check(directions.get(0).future.size()==1,"No projection past the last paired timetable stop");
    check(RoadForecast.predict(route,0,.005,386.499).get(0).future.size()==1,"Just before passage");
    check(RoadForecast.predict(route,0,.005,386.5).get(0).future.size()==1,"Exactly at passage");
    RoadForecast.Direction passed=RoadForecast.predict(route,0,.005,386.501).get(0);
    check(passed.future.isEmpty()&&passed.last!=null,"Just after passage shows last passage, not the same bus coming");
    Schedule.Route limited=new Schedule.Route("Umarizeiras","",false);
    limited.roadSegments.add(segment(1,3,"Shopping",2,false,new double[][]{{0,0},{0,.01}}));
    limited.events.add(event(1,393,"Shopping → Umarizeiras","Não identificado"));
    close(RoadForecast.predict(limited,0,.005,393.5).get(0).future.get(0).minute,394,"06:33 + 1 min at midpoint");
    check(RoadForecast.predict(limited,.00179,.005,393).size()==1,"Road within 200 metres");
    check(RoadForecast.predict(limited,.00180,.005,393).isEmpty(),"Road outside 200 metres");
    check(RoadForecast.predict(limited,.00224,.005,393).isEmpty(),"Do not associate distant streets");
    Schedule.Route midnight=new Schedule.Route("Umarizeiras","",false);
    midnight.roadSegments.add(segment(1,3,"Shopping",10,false,new double[][]{{0,0},{0,.01}}));midnight.events.add(event(1,1438,"Shopping","A"));
    check(RoadForecast.predict(midnight,0,.008,1439).get(0).future.isEmpty(),"Do not invent next-day service");
    check(RoadForecast.predict(new Schedule.Route("Amanari","",true),0,0,380).isEmpty(),"No data produces no forecast");
    System.out.println("PASS: snapping, both directions, departed buses, paired bounds, time boundaries, unavailable geometry and overnight limits");
  }
}
