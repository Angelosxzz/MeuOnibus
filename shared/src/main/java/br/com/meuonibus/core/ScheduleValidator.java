package br.com.meuonibus.core;

import org.json.*;
import java.util.HashSet;
import java.util.Set;

/** One schema contract for passenger and admin caches; never stores a partial response. */
public final class ScheduleValidator {
  private static final String[] STOPS={"Saída","Rodoviária","Pau Serrado","Shopping"};
  private static final String[] ROUTES={"Tabatinga","Umarizeiras","Amanari"};
  private ScheduleValidator(){}
  private static double number(JSONObject object,String field)throws JSONException {
    Object value=object.get(field);if(!(value instanceof Number))throw new JSONException("Número inválido: "+field);
    double result=((Number)value).doubleValue();if(!Double.isFinite(result))throw new JSONException("Número inválido: "+field);return result;
  }
  private static int integer(JSONObject object,String field,int maximum)throws JSONException {
    double value=number(object,field);if(value!=Math.floor(value)||value<0||value>maximum)throw new JSONException("Valor inválido: "+field);return (int)value;
  }
  private static void bool(JSONObject object,String field)throws JSONException {if(!(object.get(field) instanceof Boolean))throw new JSONException("Confirmação inválida: "+field);}
  private static String text(JSONObject object,String field,int maximum,boolean required)throws JSONException {
    String value=object.getString(field);if(value.length()>maximum||(required&&value.trim().isEmpty()))throw new JSONException("Texto inválido: "+field);return value;
  }
  private static void point(JSONObject object,boolean requireConfirmation)throws JSONException {
    if(Math.abs(number(object,"lat"))>90||Math.abs(number(object,"lon"))>180)throw new JSONException("Coordenadas inválidas.");
    if(requireConfirmation)bool(object,"confirmed");
    if(object.has("displayName"))text(object,"displayName",100,true);
  }
  public static void validate(JSONObject response)throws JSONException {
    double revision=number(response,"revision");if(revision<0||revision!=Math.floor(revision)||revision>9007199254740991d)throw new JSONException("Versão inválida.");
    JSONObject data=response.getJSONObject("data");if(number(data,"schemaVersion")!=2)throw new JSONException("Tabela antiga. Atualize os dados na nuvem.");
    JSONArray stops=data.getJSONArray("stops"),routes=data.getJSONArray("routes");if(stops.length()!=4||routes.length()!=3)throw new JSONException("Tabela incompleta.");
    for(int i=0;i<4;i++){JSONObject stop=stops.getJSONObject(i);if(!STOPS[i].equals(stop.getString("name")))throw new JSONException("Parada inválida.");point(stop,false);if(stop.has("confirmed"))bool(stop,"confirmed");}
    JSONArray extra=data.has("additionalStops")?data.getJSONArray("additionalStops"):new JSONArray();if(extra.length()>100)throw new JSONException("Máximo de 100 paradas adicionais.");
    Set<String> ids=new HashSet<>();
    for(int i=0;i<extra.length();i++){
      JSONObject stop=extra.getJSONObject(i);String id=text(stop,"id",100,true);if(!ids.add(id))throw new JSONException("Parada adicional duplicada.");
      text(stop,"name",100,true);point(stop,true);JSONArray served=stop.getJSONArray("routes");if(served.length()<1||served.length()>3)throw new JSONException("Linha da parada inválida.");
      for(int j=0;j<served.length();j++){String name=served.getString(j);boolean found=false;for(String allowed:ROUTES)if(allowed.equals(name))found=true;if(!found)throw new JSONException("Linha da parada inválida.");}
    }
    for(int i=0;i<3;i++){
      JSONObject route=routes.getJSONObject(i);if(!ROUTES[i].equals(route.getString("name")))throw new JSONException("Linha inválida.");
      bool(route,"confirmed");bool(route,"partial");text(route,"source",150,false);text(route,"note",1000,false);integer(route,"intervalMinutes",1440);integer(route,"averageTravelMinutes",1440);
      if(route.has("origin")&&!route.isNull("origin"))point(route.getJSONObject("origin"),true);
      JSONArray segments=route.optJSONArray("roadSegments");if(segments!=null){if(segments.length()>20)throw new JSONException("Máximo de 20 trechos por linha.");for(int j=0;j<segments.length();j++){JSONObject segment=segments.getJSONObject(j);if(segment.optBoolean("itineraryConfirmed",false))ItineraryData.validate(data,route,segment);}}
      JSONArray events=route.getJSONArray("events");if(events.length()>2000)throw new JSONException("Tabela muito grande.");
      for(int j=0;j<events.length();j++){JSONObject event=events.getJSONObject(j);integer(event,"stop",3+extra.length());integer(event,"minute",1439);text(event,"destination",100,true);text(event,"vehicle",100,false);String kind=event.getString("kind");if(!kind.equals("tabela")&&!kind.equals("estimado"))throw new JSONException("Tipo de horário inválido.");}
    }
  }
}
