package br.com.meuonibus;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Routes only the public registered stops; a passenger's location never goes to OSRM. */
public final class RoadGeometry {
  public static final List<RoadForecast.Place> places=new ArrayList<>();
  private static final String[][] TAB={{"0","1","Rodoviária","Indo para Rodoviária","21"},{"1","2","Pau Serrado","Indo para Pau Serrado","17"},{"2","3","Shopping","Voltando para Shopping","18"},{"3","0","Tabatinga","Voltando para Tabatinga","24"}};
  private static final String[][] UMA={{"0","1","Rodoviária","Indo para Rodoviária","40"},{"1","3","Shopping","Indo para Shopping → Umarizeiras","2"},{"3","0","Umarizeiras","Voltando para Umarizeiras","40"}};
  public static double[] point(JSONObject data,JSONObject route,int index)throws JSONException {
    JSONObject p=index==0?route.optJSONObject("origin"):data.getJSONArray("stops").getJSONObject(index);
    if(p==null||!p.optBoolean("confirmed",false))return null;
    return new double[]{p.getDouble("lat"),p.getDouble("lon")};
  }
  public static boolean current(JSONObject segment,double[] from,double[] to){
    if(from==null||to==null)return false;
    return Schedule.distance(from[0],from[1],segment.optDouble("fromLat",Double.NaN),segment.optDouble("fromLon",Double.NaN))<0.005&&Schedule.distance(to[0],to[1],segment.optDouble("toLat",Double.NaN),segment.optDouble("toLon",Double.NaN))<0.005;
  }
  public static void ensure(JSONObject snapshot){
    try{JSONObject data=snapshot.getJSONObject("data");JSONArray routes=data.getJSONArray("routes");
      for(int i=0;i<routes.length();i++){
        JSONObject route=routes.getJSONObject(i);String name=route.getString("name");String[][] templates=name.equals("Tabatinga")?TAB:name.equals("Umarizeiras")?UMA:new String[0][];
        JSONArray old=route.optJSONArray("roadSegments"),updated=new JSONArray();
        for(String[] template:templates){
          int from=Integer.parseInt(template[0]),to=Integer.parseInt(template[1]);double[] a=point(data,route,from),b=point(data,route,to);if(a==null||b==null)continue;
          JSONObject segment=null;
          if(old!=null)for(int j=0;j<old.length();j++){JSONObject s=old.getJSONObject(j);if(s.optInt("from",-1)==from&&s.optInt("to",-1)==to&&s.optInt("geometryVersion",0)>=2&&current(s,a,b)){segment=s;break;}}
          if(segment==null)try{segment=fetch(a,b).put("from",from).put("to",to).put("destination",template[2]).put("label",template[3]).put("travelMinutes",Integer.parseInt(template[4])).put("paired",name.equals("Tabatinga"));}catch(Exception ignored){}
          if(segment!=null)updated.put(segment);
        }
        route.put("roadSegments",updated);
      }
    }catch(JSONException ignored){}
  }
  private static JSONObject fetch(double[] a,double[] b)throws Exception {
    String url=String.format(Locale.US,"https://router.project-osrm.org/route/v1/driving/%.7f,%.7f;%.7f,%.7f?overview=full&geometries=geojson&annotations=duration",a[1],a[0],b[1],b[0]);
    HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(false);
    try{if(connection.getResponseCode()!=200)throw new IOException("Mapa indisponível");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){bytes.write(buffer,0,n);if(bytes.size()>180000)throw new IOException("Trajeto muito grande");}}
      JSONObject result=new JSONObject(bytes.toString("UTF-8"));if(!result.getString("code").equals("Ok"))throw new IOException("Sem trajeto");JSONArray coordinates=result.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates"),path=new JSONArray();if(coordinates.length()>4000)throw new IOException("Trajeto muito grande");for(int i=0;i<coordinates.length();i++){JSONArray c=coordinates.getJSONArray(i);path.put(new JSONArray().put(c.getDouble(1)).put(c.getDouble(0)));}
      JSONObject segment=new JSONObject().put("fromLat",a[0]).put("fromLon",a[1]).put("toLat",b[0]).put("toLon",b[1]).put("path",path).put("geometryVersion",2);
      JSONArray duration=result.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("annotation").getJSONArray("duration");
      if(duration.length()==path.length()-1)segment.put("edgeSeconds",duration);
      return segment;
    }finally{connection.disconnect();}
  }
  public static void load(JSONObject data,JSONObject row,Schedule.Route route)throws JSONException {
    JSONArray segments=row.optJSONArray("roadSegments");if(segments==null)return;
    for(int i=0;i<Math.min(segments.length(),20);i++)try{
      JSONObject s=segments.getJSONObject(i);int from=s.getInt("from"),to=s.getInt("to");if(from<0||from>3||to<0||to>3||from==to||!current(s,point(data,row,from),point(data,row,to)))continue;
      double minutes=s.getDouble("travelMinutes");if(!Double.isFinite(minutes)||minutes<=0||minutes>180)continue;
      JSONArray path=s.getJSONArray("path");if(path.length()<2||path.length()>4000)continue;double[][] points=new double[path.length()][2];boolean valid=true;
      for(int j=0;j<path.length();j++){JSONArray p=path.getJSONArray(j);points[j][0]=p.getDouble(0);points[j][1]=p.getDouble(1);if(!Double.isFinite(points[j][0])||!Double.isFinite(points[j][1])||Math.abs(points[j][0])>90||Math.abs(points[j][1])>180)valid=false;}
      JSONArray edge=s.optJSONArray("edgeSeconds");double[] seconds=null;if(edge!=null&&edge.length()==path.length()-1){seconds=new double[edge.length()];for(int j=0;j<seconds.length;j++)seconds[j]=edge.getDouble(j);}
      if(valid)route.roadSegments.add(new RoadForecast.Segment(from,to,s.getString("destination"),s.getString("label"),minutes,s.optBoolean("paired",false),points,seconds));
    }catch(JSONException ignored){}
  }
  public static void loadPlaces(JSONObject data){
    places.clear();
    JSONArray landmarks=data.optJSONArray("landmarks");if(landmarks!=null)for(int i=0;i<Math.min(landmarks.length(),500);i++)try{JSONObject p=landmarks.getJSONObject(i);String name=p.getString("name");double lat=p.getDouble("lat"),lon=p.getDouble("lon");if(!name.trim().isEmpty()&&name.length()<=150&&Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180)places.add(new RoadForecast.Place(name,lat,lon));}catch(JSONException ignored){}
    for(int i=0;i<Schedule.BOARD_NAMES.length;i++)if(Schedule.BOARD_VERIFIED[i])places.add(new RoadForecast.Place(Schedule.BOARD_NAMES[i],Schedule.BOARD_COORDS[i][0],Schedule.BOARD_COORDS[i][1]));
  }
}
