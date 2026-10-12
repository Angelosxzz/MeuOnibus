package br.com.meuonibus;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Cloud schedules with an atomic SQLite cache. Admin key is never persisted. */
public final class CloudDatabase extends SQLiteOpenHelper {
  public static final String ENDPOINT="https://meu-onibus.miguel-angelo09.chatgpt.site/api/schedule";
  private final Context context;private final String endpoint;
  public CloudDatabase(Context context){this(context,ENDPOINT);}
  CloudDatabase(Context context,String endpoint){super(context,"bus-schedules.db",null,1);this.context=context.getApplicationContext();this.endpoint=endpoint;}
  @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE cache(id INTEGER PRIMARY KEY, payload TEXT NOT NULL)");try{ContentValues v=new ContentValues();v.put("id",1);v.put("payload",seed().toString());db.insertOrThrow("cache",null,v);}catch(JSONException e){throw new IllegalStateException(e);}}
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
  public synchronized JSONObject snapshot() throws JSONException {try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM cache WHERE id=1",null)){if(c.moveToFirst())try{JSONObject data=new JSONObject(c.getString(0));validate(data);return data;}catch(JSONException invalid){/* Recover with the bundled table; never invent a cloud revision. */}}return seed();}
  public synchronized void store(JSONObject response) throws JSONException {validate(response);ContentValues values=new ContentValues();values.put("id",1);values.put("payload",response.toString());getWritableDatabase().insertWithOnConflict("cache",null,values,SQLiteDatabase.CONFLICT_REPLACE);}
  public JSONObject download() throws Exception {
    JSONObject cached=null;try{cached=snapshot();validate(cached);}catch(JSONException invalid){}
    return requestAt(endpoint,"GET",null,null,cached);
  }
  public JSONObject sendSuggestion(JSONObject body) throws Exception {return requestAt(endpoint.replace("/api/schedule","/api/suggestions"),"POST",body,null);}
  public JSONObject suggestionSettings() throws Exception {return requestAt(endpoint.replace("/api/schedule","/api/suggestions/settings"),"GET",null,null);}
  private JSONObject request(String method,JSONObject body,String key) throws Exception {
    return requestAt(endpoint,method,body,key);
  }
  private JSONObject requestAt(String endpoint,String method,JSONObject body,String key) throws Exception {return requestAt(endpoint,method,body,key,null);}
  private JSONObject requestAt(String endpoint,String method,JSONObject body,String key,JSONObject cached) throws Exception {
    HttpURLConnection connection=(HttpURLConnection)new URL(endpoint).openConnection();connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod(method);connection.setRequestProperty("Accept","application/json");
    try{if(cached!=null&&cached.optLong("revision")>0)connection.setRequestProperty("If-None-Match","\"schedule-"+cached.getLong("revision")+"\"");if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");if(key!=null&&!key.isEmpty())connection.setRequestProperty("Authorization","Bearer "+key);try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
      int code=connection.getResponseCode();if(code==304){if(cached==null)throw new IOException("Resposta sem tabela local.");return cached;}if(code>=300&&code<400)throw new IOException("O serviço de horários exige autenticação ou mudou de endereço.");InputStream stream=code<400?connection.getInputStream():connection.getErrorStream();if(stream==null)throw new IOException("Serviço indisponível ("+code+").");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=stream){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){bytes.write(buffer,0,n);if(bytes.size()>250000)throw new IOException("Tabela recebida muito grande.");}}
      JSONObject response;try{response=new JSONObject(bytes.toString("UTF-8"));}catch(JSONException e){throw new IOException("O serviço não retornou os dados esperados.");}if(code>=400)throw new IOException(response.optString("error","Serviço indisponível ("+code+")."));if(endpoint.equals(this.endpoint))validate(response);return response;
    }finally{connection.disconnect();}
  }
  public static void validate(JSONObject response) throws JSONException {br.com.meuonibus.core.ScheduleValidator.validate(response);}
  public static List<Schedule.Route> load(JSONObject snapshot) throws JSONException {
    JSONObject data=snapshot.getJSONObject("data");JSONArray stops=data.getJSONArray("stops"),rows=data.getJSONArray("routes"),extra=data.optJSONArray("additionalStops");int count=extra==null?0:extra.length();
    Schedule.BOARD_NAMES=new String[6+count];Schedule.BOARD_IDS=new String[6+count];Schedule.BOARD_COORDS=new double[6+count][2];Schedule.BOARD_VERIFIED=new boolean[6+count];List<Schedule.Route> result=new ArrayList<>();
    for(int i=0;i<4;i++){JSONObject stop=stops.getJSONObject(i);Schedule.COORDS[i][0]=stop.getDouble("lat");Schedule.COORDS[i][1]=stop.getDouble("lon");if(i>0){int board=i+2;Schedule.BOARD_NAMES[board]=stop.optString("displayName",stop.getString("name"));Schedule.BOARD_IDS[board]="stop:"+stop.getString("name");Schedule.BOARD_COORDS[board][0]=stop.getDouble("lat");Schedule.BOARD_COORDS[board][1]=stop.getDouble("lon");Schedule.BOARD_VERIFIED[board]=stop.optBoolean("confirmed",false);}}
    for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i),origin=row.optJSONObject("origin");Schedule.BOARD_NAMES[i]=origin==null?row.getString("name"):origin.optString("displayName",row.getString("name"));Schedule.BOARD_IDS[i]="origin:"+row.getString("name");Schedule.BOARD_COORDS[i][0]=origin==null?Schedule.COORDS[0][0]:origin.getDouble("lat");Schedule.BOARD_COORDS[i][1]=origin==null?Schedule.COORDS[0][1]:origin.getDouble("lon");Schedule.BOARD_VERIFIED[i]=origin!=null&&origin.optBoolean("confirmed",false);JSONArray events=row.getJSONArray("events");Schedule.Route route=new Schedule.Route(row.getString("name"),row.getString("source")+(row.getBoolean("confirmed")?"":" · não conferida"),events.length()==0);route.partial=row.getBoolean("partial");route.intervalMinutes=row.getInt("intervalMinutes");route.averageTravelMinutes=row.getInt("averageTravelMinutes");route.note=row.getString("note");for(int j=0;j<events.length();j++){JSONObject e=events.getJSONObject(j);route.events.add(new Schedule.Event(e.getInt("stop"),e.getInt("minute"),e.getString("destination"),e.getString("vehicle"),e.getString("kind")));}RoadGeometry.load(data,row,route);result.add(route);}
    for(int i=0;i<count;i++){JSONObject point=extra.getJSONObject(i);int board=i+6;Schedule.BOARD_NAMES[board]=point.getString("name");Schedule.BOARD_IDS[board]="extra:"+point.getString("id");Schedule.BOARD_COORDS[board][0]=point.getDouble("lat");Schedule.BOARD_COORDS[board][1]=point.getDouble("lon");Schedule.BOARD_VERIFIED[board]=point.optBoolean("confirmed",false);}
    RoadGeometry.loadPlaces(data);return result;
  }
  private JSONObject seed() throws JSONException {try(InputStream in=context.getAssets().open("schedules.json")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);return new JSONObject(bytes.toString("UTF-8"));}catch(IOException e){throw new JSONException("Não foi possível ler os horários iniciais.");}}
}


