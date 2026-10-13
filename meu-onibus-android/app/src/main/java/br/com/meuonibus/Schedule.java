package br.com.meuonibus;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Schedule {
  public static final String[] STOPS={"Saída","Rodoviária","Pau Serrado","Shopping"};
  public static final double[][] COORDS={{-3.8800,-38.6200},{-3.8750,-38.6150},{-3.8650,-38.6050},{-3.8500,-38.5900}};
  public static String[] BOARD_NAMES={"Tabatinga","Umarizeiras","Amanari","Rodoviária","Pau Serrado","Shopping"};
  public static double[][] BOARD_COORDS=new double[6][2];public static boolean[] BOARD_VERIFIED=new boolean[6];
  public static String[] BOARD_IDS=new String[6];
  public static final String[] ROUTE_NAMES={"Tabatinga","Umarizeiras","Amanari"};
  public static int nearestVerified(double lat,double lon){int index=-1;double best=Double.POSITIVE_INFINITY;for(int i=0;i<BOARD_COORDS.length;i++){if(!BOARD_VERIFIED[i])continue;double d=distance(lat,lon,BOARD_COORDS[i][0],BOARD_COORDS[i][1]);if(d<best){best=d;index=i;}}return index;}
  public static final class Trip {
    public final int[] times; public final int departure,back;
    Trip(int departure,int terminal,int pau,int shopping,int back){this.departure=departure;this.back=back;times=new int[]{departure,terminal,pau,shopping};}
  }
  public static final class Route {
    public final List<RoadForecast.Segment> roadSegments=new ArrayList<>();
    public final String name,frequency;public final List<Trip> trips=new ArrayList<>();public final boolean unconfirmed;
    public final List<Event> events=new ArrayList<>();public boolean partial;public int intervalMinutes,averageTravelMinutes;public String note="";
    Route(String name,String frequency,boolean unconfirmed){this.name=name;this.frequency=frequency;this.unconfirmed=unconfirmed;}
  }
  public static final class Event {
    public final int stop,minute;public final String destination,vehicle,kind;
    Event(int stop,int minute,String destination,String vehicle,String kind){this.stop=stop;this.minute=minute;this.destination=destination;this.vehicle=vehicle;this.kind=kind;}
  }
  public static List<Route> routes(){
    List<Route> routes=new ArrayList<>();
    routes.add(new Route("Tabatinga","Carregando tabela",true));
    routes.add(new Route("Umarizeiras","Carregando tabela",true));
    routes.add(new Route("Amanari","Aguardando tabela de horários",true));return routes;
  }
  private static int parse(String value){String[] parts=value.split(":");return Integer.parseInt(parts[0])*60+Integer.parseInt(parts[1]);}
  public static String time(int value){return String.format(Locale.forLanguageTag("pt-BR"),"%02d:%02d",value/60,value%60);}
  public static ZonedDateTime now(){return ZonedDateTime.now(ZoneId.of("America/Fortaleza"));}
  public static double minute(ZonedDateTime now){return now.getHour()*60+now.getMinute()+now.getSecond()/60.0;}
  public static List<Trip> future(Route route,int stop,double now){List<Trip> result=new ArrayList<>();for(Trip t:route.trips)if(t.times[stop]>=0&&t.times[stop]>=now)result.add(t);result.sort((a,b)->Integer.compare(a.times[stop],b.times[stop]));return result;}
  public static boolean serves(Route route,int stop){for(Trip t:route.trips)if(t.times[stop]>=0)return true;return false;}
  public static int waitMinutes(int arrival,double now){return (int)Math.ceil(arrival-now);}
  public static double distance(double lat1,double lon1,double lat2,double lon2){double a=Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2),2);return 6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(Math.max(0,1-a)));}
  public static int nearest(double lat,double lon){int index=0;double best=Double.POSITIVE_INFINITY;for(int i=0;i<COORDS.length;i++){double d=distance(lat,lon,COORDS[i][0],COORDS[i][1]);if(d<best){best=d;index=i;}}return index;}
  // Observed Tabatinga variation, not a guaranteed operating tolerance.
  public static double earliestMinute(Route route,double reference){return route.name.equals("Tabatinga")?Math.max(0,reference-4):reference;}
  public static double latestMinute(Route route,int departureStop,double reference){return route.name.equals("Tabatinga")?Math.min(1439,reference+(departureStop==1?2:4)):reference;}
  public static List<Event> futureEvents(Route route,int stop,double now,String destination){List<Event> result=new ArrayList<>();for(Event e:route.events)if(e.stop==stop&&latestMinute(route,e.stop,e.minute)>=now&&(destination.isEmpty()||e.destination.equals(destination)))result.add(e);result.sort((a,b)->Integer.compare(a.minute,b.minute));return result;}
  public static String stopName(Route route,int stop){return stop==0?BOARD_NAMES[java.util.Arrays.asList(ROUTE_NAMES).indexOf(route.name)]:BOARD_NAMES[stop+2];}
}
