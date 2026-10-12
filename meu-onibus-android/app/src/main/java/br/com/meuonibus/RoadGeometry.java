package br.com.meuonibus;

import org.json.*;
import java.util.*;

/** Uses only explicitly confirmed bus itineraries from the schedule database. */
public final class RoadGeometry {
  public static final List<RoadForecast.Place> places=new ArrayList<>();
  public static double[] point(JSONObject data,JSONObject route,int index)throws JSONException {
    JSONObject p=index==0?route.optJSONObject("origin"):data.getJSONArray("stops").getJSONObject(index);
    if(p==null||!p.optBoolean("confirmed",false))return null;
    return new double[]{p.getDouble("lat"),p.getDouble("lon")};
  }
  public static boolean current(JSONObject segment,double[] from,double[] to){
    if(from==null||to==null)return false;
    return Schedule.distance(from[0],from[1],segment.optDouble("fromLat",Double.NaN),segment.optDouble("fromLon",Double.NaN))<0.005&&Schedule.distance(to[0],to[1],segment.optDouble("toLat",Double.NaN),segment.optDouble("toLon",Double.NaN))<0.005;
  }
  /** Keep cloud itineraries intact. Never replace them with a generic car route. */
  public static void ensure(JSONObject snapshot){}
  public static void load(JSONObject data,JSONObject row,Schedule.Route route)throws JSONException {
    JSONArray segments=row.optJSONArray("roadSegments");if(segments==null)return;
    for(int i=0;i<Math.min(segments.length(),20);i++)try{
      JSONObject s=segments.getJSONObject(i);int from=s.getInt("from"),to=s.getInt("to");if(from<0||from>3||to<0||to>3||from==to||!current(s,point(data,row,from),point(data,row,to)))continue;
      br.com.meuonibus.core.ItineraryData.validate(data,row,s);
      double minutes=s.getDouble("travelMinutes");if(!Double.isFinite(minutes)||minutes<=0||minutes>180)continue;
      JSONArray path=s.getJSONArray("path");if(path.length()<2||path.length()>4000)continue;double[][] points=new double[path.length()][2];boolean valid=true;
      for(int j=0;j<path.length();j++){JSONArray p=path.getJSONArray(j);points[j][0]=p.getDouble(0);points[j][1]=p.getDouble(1);if(!Double.isFinite(points[j][0])||!Double.isFinite(points[j][1])||Math.abs(points[j][0])>90||Math.abs(points[j][1])>180)valid=false;}
      JSONArray edge=s.optJSONArray("edgeSeconds");double[] seconds=null;if(edge!=null&&edge.length()==path.length()-1){seconds=new double[edge.length()];for(int j=0;j<seconds.length;j++)seconds[j]=edge.getDouble(j);}
      if(valid)route.roadSegments.add(new RoadForecast.Segment(from,to,s.getString("destination"),s.getString("label"),minutes,s.optBoolean("paired",false),points,seconds,true));
    }catch(JSONException ignored){}
  }
  public static void loadPlaces(JSONObject data){
    places.clear();
    JSONArray landmarks=data.optJSONArray("landmarks");if(landmarks!=null)for(int i=0;i<Math.min(landmarks.length(),500);i++)try{JSONObject p=landmarks.getJSONObject(i);String name=p.getString("name");double lat=p.getDouble("lat"),lon=p.getDouble("lon");if(!name.trim().isEmpty()&&name.length()<=150&&Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180)places.add(new RoadForecast.Place(name,lat,lon));}catch(JSONException ignored){}
    for(int i=0;i<Schedule.BOARD_NAMES.length;i++)if(Schedule.BOARD_VERIFIED[i])places.add(new RoadForecast.Place(Schedule.BOARD_NAMES[i],Schedule.BOARD_COORDS[i][0],Schedule.BOARD_COORDS[i][1]));
  }
}
