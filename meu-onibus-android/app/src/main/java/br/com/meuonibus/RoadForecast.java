package br.com.meuonibus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Schedule projection on a street polyline. No live vehicle positions are assumed. */
public final class RoadForecast {
  public static final class Segment {
    public final int from,to; public final String destination,label;
    public final double travelMinutes; public final boolean paired;
    public final double[][] path;public final double[] edgeSeconds;public final boolean itineraryConfirmed;
    public Segment(int from,int to,String destination,String label,double minutes,boolean paired,double[][] path){
      this(from,to,destination,label,minutes,paired,path,null);
    }
    public Segment(int from,int to,String destination,String label,double minutes,boolean paired,double[][] path,double[] edgeSeconds){
      this(from,to,destination,label,minutes,paired,path,edgeSeconds,false);
    }
    public Segment(int from,int to,String destination,String label,double minutes,boolean paired,double[][] path,double[] edgeSeconds,boolean itineraryConfirmed){
      this.from=from;this.to=to;this.destination=destination;this.label=label;
      this.travelMinutes=minutes;this.paired=paired;this.path=path;this.edgeSeconds=edgeSeconds;this.itineraryConfirmed=itineraryConfirmed;
    }
  }
  public static final class Snap {
    public final double lat,lon,distanceMetres,fraction;
    Snap(double lat,double lon,double distance,double fraction){this.lat=lat;this.lon=lon;this.distanceMetres=distance;this.fraction=fraction;}
  }
  public static final class Passage {
    public final Schedule.Event departure; public final double minute,earliestMinute,latestMinute;
    Passage(Schedule.Event departure,double minute,double earliest,double latest){this.departure=departure;this.minute=minute;this.earliestMinute=earliest;this.latestMinute=latest;}
    public boolean departed(double now){return departure.minute<=now;}
  }
  public static final class Direction {
    public final Segment segment;public final Snap snap;
    public final List<Passage> future=new ArrayList<>();public Passage last;
    Direction(Segment segment,Snap snap){this.segment=segment;this.snap=snap;}
  }
  public static Direction nearestConfirmed(Schedule.Route route,double lat,double lon){
    Direction nearest=null;
    for(Segment segment:route.roadSegments){if(!segment.itineraryConfirmed)continue;Snap point=snap(lat,lon,segment.path,segment.edgeSeconds);if(point!=null&&(nearest==null||point.distanceMetres<nearest.snap.distanceMetres))nearest=new Direction(segment,point);}
    return nearest;
  }
  public static Snap snap(double lat,double lon,double[][] path){
    return snap(lat,lon,path,null);
  }
  public static Snap snap(double lat,double lon,double[][] path,double[] edgeSeconds){
    if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||path==null||path.length<2)return null;
    double physical=0;for(int i=1;i<path.length;i++)physical+=Schedule.distance(path[i-1][0],path[i-1][1],path[i][0],path[i][1]);
    if(physical<0.000001)return null;
    boolean weighted=edgeSeconds!=null&&edgeSeconds.length==path.length-1;double totalSeconds=0;
    if(weighted)for(double seconds:edgeSeconds){if(!Double.isFinite(seconds)||seconds<0)weighted=false;totalSeconds+=seconds;}
    weighted=weighted&&totalSeconds>0;double total=weighted?totalSeconds:physical;
    double best=Double.POSITIVE_INFINITY,bestLat=0,bestLon=0,bestAlong=0,along=0;
    double scale=Math.cos(Math.toRadians(lat));
    for(int i=1;i<path.length;i++){
      double[] a=path[i-1],b=path[i];double ax=(a[1]-lon)*scale,ay=a[0]-lat,bx=(b[1]-lon)*scale,by=b[0]-lat;
      double dx=bx-ax,dy=by-ay,length=dx*dx+dy*dy;
      double t=length==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/length));
      double pLat=a[0]+t*(b[0]-a[0]),pLon=a[1]+t*(b[1]-a[1]);
      double distance=Schedule.distance(lat,lon,pLat,pLon)*1000;
      double leg=weighted?edgeSeconds[i-1]:Schedule.distance(a[0],a[1],b[0],b[1]);
      if(distance<best){best=distance;bestLat=pLat;bestLon=pLon;bestAlong=along+t*leg;}along+=leg;
    }
    return new Snap(bestLat,bestLon,best,Math.max(0,Math.min(1,bestAlong/total)));
  }
  public static List<Direction> predict(Schedule.Route route,double lat,double lon,double now){
    List<Direction> all=new ArrayList<>();double nearest=Double.POSITIVE_INFINITY;
    for(Segment segment:route.roadSegments){if(!segment.itineraryConfirmed)continue;Snap snap=snap(lat,lon,segment.path,segment.edgeSeconds);if(snap==null)continue;nearest=Math.min(nearest,snap.distanceMetres);all.add(new Direction(segment,snap));}
    List<Direction> result=new ArrayList<>();if(nearest>200)return result;
    for(Direction direction:all){
      // Include nearby parallel carriageways instead of assigning the passenger a travel direction.
      if(direction.snap.distanceMetres>Math.min(200,nearest+80))continue;
      Segment segment=direction.segment;
      for(Schedule.Event event:route.events){
        if(event.stop!=segment.from||!event.destination.startsWith(segment.destination))continue;
        double duration=segment.travelMinutes;
        if(segment.paired){
          Schedule.Event end=null;
          for(Schedule.Event candidate:route.events)if(candidate.stop==segment.to&&candidate.minute>event.minute&&candidate.minute-event.minute<=60&&candidate.vehicle.equals(event.vehicle)&&(end==null||candidate.minute<end.minute))end=candidate;
          if(end==null)continue;duration=end.minute-event.minute;
        }
        double arrival=event.minute+duration*direction.snap.fraction;
        if(arrival>=1440)continue;
        Passage passage=new Passage(event,arrival,Schedule.earliestMinute(route,arrival),Schedule.latestMinute(route,event.stop,arrival));
        if(passage.latestMinute>=now)direction.future.add(passage);else if(direction.last==null||arrival>direction.last.minute)direction.last=passage;
      }
      direction.future.sort(Comparator.comparingDouble(p->p.minute));result.add(direction);
    }
    return result;
  }
  public static final class Place {
    public final String name;public final double lat,lon;
    public Place(String name,double lat,double lon){this.name=name;this.lat=lat;this.lon=lon;}
  }
  public static Place nearestPlace(double lat,double lon,List<Place> places){
    Place best=null;double distance=0.150;
    for(Place place:places){double d=Schedule.distance(lat,lon,place.lat,place.lon);if(d<=distance){best=place;distance=d;}}
    return best;
  }
}
