package br.com.meuonibus.core;
import org.json.*;

/** Coordinates are stored in [latitude, longitude] order. No routing-engine fallback. */
public final class ItineraryData {
  private ItineraryData(){}
  public static double[] stop(JSONObject data,JSONObject route,int index)throws JSONException {
    if(index<0||index>3)return null;
    JSONObject value=index==0?route.optJSONObject("origin"):data.getJSONArray("stops").getJSONObject(index);
    if(value==null||!value.optBoolean("confirmed",false))return null;
    return new double[]{value.getDouble("lat"),value.getDouble("lon")};
  }
  public static double[][] points(JSONArray path)throws JSONException {
    if(path.length()<2||path.length()>1000)throw new JSONException("Marque entre 2 e 1000 pontos do trajeto.");
    double[][] points=new double[path.length()][2];
    for(int i=0;i<path.length();i++){JSONArray p=path.getJSONArray(i);if(p.length()!=2||!(p.get(0) instanceof Number)||!(p.get(1) instanceof Number))throw new JSONException("Coordenada do trajeto inválida.");points[i][0]=p.getDouble(0);points[i][1]=p.getDouble(1);}
    return points;
  }
  public static void validate(JSONObject data,JSONObject route,JSONObject segment)throws JSONException {
    for(String key:new String[]{"from","to"}){Object value=segment.get(key);if(!(value instanceof Number)||((Number)value).doubleValue()!=Math.floor(((Number)value).doubleValue())||((Number)value).doubleValue()<0||((Number)value).doubleValue()>3)throw new JSONException("Parada do trecho inválida.");}
    for(String key:new String[]{"geometrySource","geometryReference","label","destination"})if(!(segment.get(key) instanceof String))throw new JSONException("Texto do itinerário inválido.");
    int from=segment.getInt("from"),to=segment.getInt("to");
    if(from==to||!Boolean.TRUE.equals(segment.opt("itineraryConfirmed"))||!(segment.opt("geometryVersion") instanceof Number)||segment.getDouble("geometryVersion")!=3||!ItineraryPolicy.provenance(true,3,segment.optString("geometrySource"),segment.optString("geometryReference")))throw new JSONException("Confirme o itinerário e informe sua origem.");
    double[] a=stop(data,route,from),b=stop(data,route,to);
    if(!ItineraryPolicy.path(points(segment.getJSONArray("path")),a,b))throw new JSONException("O desenho deve começar e terminar a até 50 m das paradas e ter pontos a no máximo 250 m entre si. Marque também todas as curvas.");
    for(String key:new String[]{"fromLat","fromLon","toLat","toLon"})if(!(segment.get(key) instanceof Number))throw new JSONException("Referência da parada do itinerário inválida.");
    double[] declaredFrom={segment.getDouble("fromLat"),segment.getDouble("fromLon")},declaredTo={segment.getDouble("toLat"),segment.getDouble("toLon")};
    for(double[] p:new double[][]{declaredFrom,declaredTo})if(!Double.isFinite(p[0])||!Double.isFinite(p[1])||Math.abs(p[0])>90||Math.abs(p[1])>180)throw new JSONException("Referência da parada inválida.");
    if(ItineraryPolicy.metres(a,declaredFrom)>=5||ItineraryPolicy.metres(b,declaredTo)>=5)throw new JSONException("A parada mudou; confira o trajeto novamente.");
    double minutes=segment.getDouble("travelMinutes");if(!(segment.get("travelMinutes") instanceof Number)||!Double.isFinite(minutes)||minutes<=0||minutes>180)throw new JSONException("Informe uma duração maior que 0 e de até 180 minutos.");
    for(String key:new String[]{"label","destination"})if(segment.getString(key).trim().isEmpty()||segment.getString(key).length()>100)throw new JSONException("Sentido inválido.");
  }
  public static void invalidateStop(JSONObject data,int routeIndex,int stopIndex)throws JSONException {
    JSONArray routes=data.getJSONArray("routes");
    for(int i=0;i<routes.length();i++){if(routeIndex>=0&&i!=routeIndex)continue;JSONArray segments=routes.getJSONObject(i).optJSONArray("roadSegments");if(segments==null)continue;for(int j=0;j<segments.length();j++){JSONObject segment=segments.getJSONObject(j);if(segment.optInt("from",-1)==stopIndex||segment.optInt("to",-1)==stopIndex)segment.put("itineraryConfirmed",false);}}
  }
}
