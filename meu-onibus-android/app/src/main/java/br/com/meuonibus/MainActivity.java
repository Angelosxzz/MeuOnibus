package br.com.meuonibus;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.json.*;

public class MainActivity extends Activity {
  private static final int GREEN=Color.rgb(11,85,71), INK=Color.rgb(23,51,45), MUTED=Color.rgb(77,99,86), BG=Color.rgb(244,246,242);
  private final Handler handler=new Handler(Looper.getMainLooper());
  private List<Schedule.Route> routes=Schedule.routes();
  private AppUpdates updates;private CloudDatabase database;private TextView syncStatus;private TextView demoNotice;private boolean syncing;private long lastSync;
  private final ExecutorService executor=Executors.newSingleThreadExecutor();
  private int boardingPosition=4;private JSONObject suggestionDraft;private AlertDialog suggestionDialog;private EditText suggestionName,suggestionReason;private TextView suggestionPoint;private Double suggestionLat,suggestionLon;private boolean newStop;private String suggestionTarget="",suggestionRoute="Tabatinga";
  private final java.util.Map<String,String> directions=new java.util.HashMap<>();
  private LinearLayout cards;private TextView clock,location;private Spinner spinner;private Button gps;private int selected=2;
  private boolean pointMode;private double passengerLat,passengerLon,passengerAccuracy;private LocationManager locationManager;private LocationListener listener;private boolean locating;
  private final Runnable ticker=new Runnable(){public void run(){render();if(android.os.SystemClock.elapsedRealtime()-lastSync>60000)sync();handler.postDelayed(this,15000);}};
  private final Runnable gpsTimeout=()->{if(locating){stopLocation();location.setText("O GPS demorou para responder. Selecione a parada manualmente.");}};
  @Override public void onCreate(Bundle state){
    super.onCreate(state);if(state!=null){passengerLat=state.getDouble("point_lat");passengerLon=state.getDouble("point_lon");passengerAccuracy=state.getDouble("point_accuracy");pointMode=state.getBoolean("point_mode")&&Double.isFinite(passengerLat)&&Math.abs(passengerLat)<=90&&Double.isFinite(passengerLon)&&Math.abs(passengerLon)<=180;for(String name:Schedule.ROUTE_NAMES)directions.put(name,state.getString("direction_"+name,""));}boardingPosition=getPreferences(MODE_PRIVATE).getInt("boardingPosition",4);if(boardingPosition<0)boardingPosition=4;
    database=new CloudDatabase(this);try{routes=CloudDatabase.load(database.snapshot());}catch(JSONException e){Toast.makeText(this,"Não foi possível ler a tabela salva.",Toast.LENGTH_LONG).show();}
    if(boardingPosition>=Schedule.BOARD_NAMES.length)boardingPosition=4;selected=boardingPosition<3?0:boardingPosition-2;
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
    LinearLayout content=column();content.setPadding(dp(20),dp(20),dp(20),dp(24));scroll.addView(content);
    if(android.os.Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,insets)->{int top=insets.getInsets(WindowInsets.Type.systemBars()).top;int bottom=insets.getInsets(WindowInsets.Type.systemBars()).bottom;v.setPadding(0,top,0,bottom);return insets;});
    else scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;});
    content.addView(text("↗  meu ônibus.",25,INK,true));gap(content,28);
    content.addView(text("TABATINGA · UMARIZEIRAS · AMANARI",10,MUTED,true));gap(content,12);
    content.addView(text("Seu próximo ônibus,\nna palma da mão.",32,INK,true));gap(content,8);
    content.addView(text("Escolha sua parada e consulte as próximas passagens.",14,MUTED,false));gap(content,20);
    TextView demo=text("PREVISÕES ESTIMADAS\nCalculadas pelos horários e trajetos cadastrados. Sem rastreamento dos veículos e sem trânsito em tempo real.",12,Color.rgb(113,89,41),false);demoNotice=demo;demo.setPadding(dp(16),dp(14),dp(16),dp(14));demo.setBackground(background(Color.rgb(255,243,218),12));content.addView(demo);gap(content,18);
    LinearLayout panel=column();panel.setPadding(dp(20),dp(20),dp(20),dp(18));panel.setBackground(background(GREEN,16));content.addView(panel);
    panel.addView(text("Onde você vai embarcar?",20,Color.WHITE,true));gap(panel,14);
    TextView label=text("Sua parada",12,Color.rgb(206,225,212),false);panel.addView(label);
    spinner=new Spinner(this);spinner.setContentDescription("Sua parada");ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Schedule.BOARD_NAMES);spinner.setAdapter(adapter);spinner.setBackground(background(Color.WHITE,8));spinner.setMinimumHeight(dp(52));spinner.setSelection(boardingPosition);panel.addView(spinner,new LinearLayout.LayoutParams(-1,-2));gap(panel,10);
    gps=button("⌖ Usar meu GPS",Color.rgb(208,247,119),INK);panel.addView(gps);gap(panel,8);
    location=text("Use o GPS ou escolha no mapa para ver os dois sentidos da pista.",12,Color.rgb(206,225,212),false);panel.addView(location);
    gap(content,22);content.addView(text("Outras formas de consultar",20,INK,true));gap(content,6);content.addView(text("Escolha como quer ver os ônibus. As opções abaixo não precisam do GPS.",13,MUTED,false));gap(content,14);
    Button both=consultOption(content,"Ida e volta na parada escolhida","Usa a parada selecionada acima para mostrar os ônibus nos dois sentidos, com o destino de cada um.","Ver os dois sentidos desta parada");
    both.setOnClickListener(v->{if(!Schedule.BOARD_VERIFIED[boardingPosition]){Toast.makeText(this,"A localização desta parada ainda não foi cadastrada.",Toast.LENGTH_LONG).show();return;}passengerLat=Schedule.BOARD_COORDS[boardingPosition][0];passengerLon=Schedule.BOARD_COORDS[boardingPosition][1];passengerAccuracy=0;pointMode=true;render();});
    spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){if(position!=boardingPosition){pointMode=false;stopLocation();location.setText("Consultando a parada "+Schedule.BOARD_NAMES[position]+".");}boardingPosition=position;selected=position<3?0:position-2;getPreferences(MODE_PRIVATE).edit().putInt("boardingPosition",position).apply();render();}});
    gps.setOnClickListener(v->requestGps());Button roadPoint=consultOption(content,"Vou embarcar em outro ponto","Abra o mapa, toque onde você pretende pegar o ônibus e confirme. A previsão será calculada no ponto mais próximo da pista.","Escolher ponto na pista pelo mapa");roadPoint.setOnClickListener(v->{double lat=pointMode?passengerLat:Schedule.BOARD_COORDS[boardingPosition][0],lon=pointMode?passengerLon:Schedule.BOARD_COORDS[boardingPosition][1];startActivityForResult(new android.content.Intent(this,MapPickerActivity.class).putExtra("name","Meu ponto de embarque").putExtra("lat",lat).putExtra("lon",lon),220);});Button manual=consultOption(content,"Consultar apenas a tabela de horários","Mostra as próximas saídas da parada selecionada acima. Use também para sair da previsão por GPS ou pelo mapa e voltar à tabela.","Ver horários da parada selecionada");manual.setOnClickListener(v->{pointMode=false;location.setText("Consultando a parada selecionada.");render();});gap(content,22);Button mapButton=button("Sugerir alteração ou nova parada",Color.rgb(208,247,119),GREEN);content.addView(mapButton);gap(content,10);mapButton.setOnClickListener(v->chooseSuggestion());gap(content,16);
    LinearLayout title=new LinearLayout(this);title.setOrientation(LinearLayout.HORIZONTAL);TextView heading=text("Próximas passagens",22,INK,true);title.addView(heading,new LinearLayout.LayoutParams(0,-2,1));Button refresh=button("↻",Color.WHITE,GREEN);refresh.setContentDescription("Atualizar previsões e horários da nuvem");title.addView(refresh,new LinearLayout.LayoutParams(dp(52),dp(48)));refresh.setOnClickListener(v->{render();sync();});content.addView(title);
    clock=text("",12,MUTED,false);content.addView(clock);syncStatus=text("Tabela salva no aparelho · buscando atualização…",12,MUTED,false);content.addView(syncStatus);gap(content,16);cards=column();content.addView(cards);
    
    Button checkUpdate=button("Verificar atualização do app",Color.rgb(231,237,223),GREEN);content.addView(checkUpdate);checkUpdate.setOnClickListener(v->{if(updates!=null)updates.check(true);});gap(content,12);Button about=button("Sobre os horários e a localização",Color.rgb(231,237,223),GREEN);content.addView(about);about.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Sobre este protótipo").setMessage("Consultar os horários salvos funciona sem internet. Carregar o mapa, enviar sugestões e atualizar os dados exige conexão. Os horários foram transcritos das fotos enviadas. Dias da semana e feriados ainda não foram informados. Trânsito e atrasos não estão incluídos.\n\nAmanari ainda não tem horários cadastrados.\n\nO GPS associa você ao ponto mais próximo dos trajetos estimados pelas ruas entre as paradas cadastradas. Os sentidos são mostrados separadamente; confira o lado da pista e o destino do ônibus. Os trajetos por ruas são referências e podem diferir do itinerário real. O GPS da consulta não é enviado ao banco. O mapa carrega imagens de terceiros das áreas exibidas. Ao enviar uma sugestão, o ponto escolhido é enviado para análise. Sem trajeto ou coordenadas cadastradas não é possível prever a passagem no seu ponto.\n\nO relógio usa o fuso do Ceará e depende da hora correta do celular.").setPositiveButton("Entendi",null).show());gap(content,16);content.addView(text("Menos espera. Mais caminho.",11,MUTED,false));
    setContentView(scroll);render();sync();updates=new AppUpdates(this);updates.check(false);
  }
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);ticker.run();if(updates!=null)updates.resumed();}
  @Override protected void onPause(){super.onPause();handler.removeCallbacks(ticker);stopLocation();}
  private void render(){
    if(cards==null)return;ZonedDateTime now=Schedule.now();double minute=Schedule.minute(now);clock.setText("Horário do Ceará · "+now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));cards.removeAllViews();if(pointMode){renderRoad(minute);return;}
    for(Schedule.Route route:routes){
      if(selected==0&&!route.name.equals(Schedule.ROUTE_NAMES[boardingPosition]))continue;
      LinearLayout card=column();card.setPadding(dp(20),dp(20),dp(20),dp(20));card.setBackground(background(Color.WHITE,16));cards.addView(card);gap(cards,14);card.addView(text(route.name,20,INK,true));gap(card,5);card.addView(text(route.frequency,12,MUTED,false));gap(card,10);
      String destination=directions.getOrDefault(route.name,"");java.util.Set<String> choices=new java.util.TreeSet<>();for(Schedule.Event event:route.events)if(event.stop==selected)choices.add(event.destination);
      if(!destination.isEmpty()&&!choices.contains(destination)){destination="";directions.put(route.name,"");}
      if(choices.size()>1){Button choose=button("Destino: "+(destination.isEmpty()?"Todos":destination),Color.rgb(238,244,238),GREEN);card.addView(choose);choose.setOnClickListener(v->{java.util.List<String> options=new java.util.ArrayList<>();options.add("Todos os destinos");options.addAll(choices);new AlertDialog.Builder(this).setTitle("Sentido de "+route.name).setItems(options.toArray(new String[0]),(d,i)->{directions.put(route.name,i==0?"":options.get(i));render();}).show();});gap(card,12);}
      List<Schedule.Event> future=Schedule.futureEvents(route,selected,minute,destination);
      if(route.unconfirmed){card.addView(text("Horários não cadastrados",23,MUTED,true));gap(card,8);card.addView(text("Aguardando uma tabela de horários.",13,MUTED,false));}
      else if(choices.isEmpty()){card.addView(text("Sem horários nesta parada",23,MUTED,true));gap(card,8);card.addView(text(route.name.equals("Umarizeiras")&&!route.note.isEmpty()?route.note:"A tabela recebida não informa esta parada.",13,MUTED,false));}
      else{
        if(route.partial&&route.intervalMinutes>0){card.addView(text("~"+route.intervalMinutes+" min",40,GREEN,true));card.addView(text("Intervalo médio relatado da linha",13,MUTED,false));gap(card,7);card.addView(text("Os horários abaixo são apenas do 5º carro, não de toda a frota.",12,MUTED,false));gap(card,15);}
        if(future.isEmpty()){card.addView(text(route.partial?"Sem mais horários do carro da tabela hoje":"Sem mais saídas na tabela hoje",21,MUTED,true));}
        else{Schedule.Event next=future.get(0);int wait=Schedule.waitMinutes(next.minute,minute);
          card.addView(text(route.partial?"5º carro: "+Schedule.time(next.minute):(wait==0?"Agora":wait+" min"),route.partial?23:44,GREEN,true));
          if(route.partial)card.addView(text("Em "+wait+" min · referência da tabela",13,MUTED,false));
          gap(card,6);card.addView(text((next.kind.equals("estimado")?"Estimativa":"Horário da tabela")+": "+Schedule.time(next.minute),14,MUTED,false));
          card.addView(text(Schedule.stopName(route,selected)+" → "+next.destination,16,INK,true));if(route.name.equals("Umarizeiras")&&selected==3&&next.destination.equals("Umarizeiras")&&route.averageTravelMinutes>0){int arrival=next.minute+route.averageTravelMinutes;card.addView(text("Chegada estimada a Umarizeiras: "+Schedule.time(arrival%1440)+(arrival>=1440?" (dia seguinte)":"")+" · ~"+route.averageTravelMinutes+" min após a saída",13,MUTED,false));}
          if(!next.vehicle.equals("Não identificado")&&!next.vehicle.isEmpty())card.addView(text(next.vehicle,12,MUTED,false));
          gap(card,12);StringBuilder after=new StringBuilder("Depois: ");for(int i=1;i<Math.min(4,future.size());i++){if(i>1)after.append(" · ");Schedule.Event e=future.get(i);after.append(Schedule.time(e.minute)).append(" → ").append(e.destination);}if(future.size()==1)after.append("último horário cadastrado hoje");card.addView(text(after.toString(),12,MUTED,false));
        }
        if(route.averageTravelMinutes>0){gap(card,12);card.addView(text((route.name.equals("Umarizeiras")?"Volta Shopping → Umarizeiras: ~":"Viagem média informada: ~")+route.averageTravelMinutes+" min. Pode variar.",12,MUTED,false));}
        gap(card,14);Button all=button("Ver horários e fonte",Color.rgb(238,244,238),GREEN);card.addView(all);final String chosen=destination;all.setOnClickListener(v->{StringBuilder table=new StringBuilder(route.note+"\n\n");java.util.List<Schedule.Event> events=Schedule.futureEvents(route,selected,0,chosen);for(Schedule.Event e:events)table.append(Schedule.time(e.minute)).append(" → ").append(e.destination).append(" · ").append(e.vehicle).append('\n');new AlertDialog.Builder(this).setTitle(route.name+" · "+Schedule.stopName(route,selected)).setMessage(table.toString()).setPositiveButton("Fechar",null).show();});
      }
    }
  }
  private void renderRoad(double minute){
    double nearest=Double.POSITIVE_INFINITY;int count=0;java.util.Set<String> matched=new java.util.HashSet<>();
    for(Schedule.Route route:routes){
      List<RoadForecast.Direction> predictions=RoadForecast.predict(route,passengerLat,passengerLon,minute);
      if(!predictions.isEmpty())matched.add(route.name);
      for(RoadForecast.Direction direction:predictions){
        nearest=Math.min(nearest,direction.snap.distanceMetres);count++;
        LinearLayout card=column();card.setPadding(dp(20),dp(20),dp(20),dp(20));card.setBackground(background(Color.WHITE,16));cards.addView(card);gap(cards,14);
        card.addView(text(route.name,20,INK,true));card.addView(text(direction.segment.label,18,GREEN,true));gap(card,10);
        RoadForecast.Place place=RoadForecast.nearestPlace(direction.snap.lat,direction.snap.lon,RoadGeometry.places);
        card.addView(text(place==null?"Ponto mais próximo deste sentido da pista":"Ponto na pista perto de "+place.name,12,MUTED,false));
        if(direction.future.isEmpty()){gap(card,10);card.addView(text("Sem mais passagens previstas hoje",20,MUTED,true));}
        else{
          RoadForecast.Passage next=direction.future.get(0);int wait=(int)Math.ceil(next.minute-minute);gap(card,8);
          card.addView(text(wait==0?"Agora":"~"+wait+" min",40,GREEN,true));
          card.addView(text("Passagem estimada: "+roadTime(next.minute),14,MUTED,false));
          String from=Schedule.stopName(route,direction.segment.from);
          card.addView(text(next.departed(minute)?"Pela tabela, já saiu de "+from+" às "+Schedule.time(next.departure.minute):"Saída prevista de "+from+" às "+Schedule.time(next.departure.minute),13,MUTED,false));
          if(!next.departure.vehicle.equals("Não identificado"))card.addView(text(next.departure.vehicle,12,MUTED,false));
          if(direction.future.size()>1){StringBuilder later=new StringBuilder("Próximos neste sentido:");for(int i=1;i<Math.min(4,direction.future.size());i++){RoadForecast.Passage passage=direction.future.get(i);later.append("\n").append(roadTime(passage.minute)).append(" · ").append(passage.departed(minute)?"Pela tabela, já saiu de ":"Saída prevista de ").append(from).append(" às ").append(Schedule.time(passage.departure.minute));}gap(card,10);card.addView(text(later.toString(),12,MUTED,false));}
        }
        if(direction.last!=null){gap(card,6);card.addView(text("Última passagem prevista: "+roadTime(direction.last.minute),12,MUTED,false));}
        gap(card,10);card.addView(text("Confira o destino e o lado da pista. Trajeto por ruas estimado; sem localização ao vivo do ônibus.",12,MUTED,false));
        Button view=button("Ver ponto associado na pista",Color.rgb(238,244,238),GREEN);card.addView(view);
        view.setOnClickListener(v->startActivity(new android.content.Intent(this,MapPickerActivity.class).putExtra("name",direction.segment.label).putExtra("lat",direction.snap.lat).putExtra("lon",direction.snap.lon).putExtra("readOnly",true)));
      }
    }
    if(count==0){cards.addView(text("Nenhum trajeto cadastrado a até 200 m deste ponto. Escolha outro ponto no mapa ou consulte uma parada.",18,MUTED,true));location.setText("Sem ponto próximo dos trajetos disponíveis.");}
    else location.setText(String.format(Locale.forLanguageTag("pt-BR"),"Ponto associado à pista a ~%.0f m de você.%s\nPrevisões separadas por sentido.",nearest,passengerAccuracy>0?" Precisão do GPS: ±"+Math.round(passengerAccuracy)+" m.":""));
    for(Schedule.Route route:routes)if(!matched.contains(route.name)){gap(cards,10);cards.addView(text(route.name+(route.unconfirmed?": horários ainda não cadastrados.":route.roadSegments.isEmpty()?": aguardando coordenadas e trajeto cadastrados.":": sem trecho disponível perto deste ponto. Consulte as paradas da linha."),13,MUTED,false));}
  }
  private String roadTime(double minute){return Schedule.time((int)Math.floor(minute));}
  private void requestGps(){if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);}else startLocation();}
  @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==10){if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)startLocation();else location.setText("Permissão de localização negada. Selecione a parada no menu.");}}
  private void startLocation(){
    stopLocation();locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);locating=true;gps.setEnabled(false);gps.setText("Localizando…");location.setText("Aguardando a localização do aparelho…");
    listener=new LocationListener(){public void onLocationChanged(Location fix){if(!locating)return;passengerLat=fix.getLatitude();passengerLon=fix.getLongitude();passengerAccuracy=fix.getAccuracy();pointMode=true;stopLocation();render();return;}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int state,Bundle b){}};
    try{boolean enabled=false;for(String provider:locationManager.getProviders(true)){if(provider.equals(LocationManager.GPS_PROVIDER)&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)continue;if(provider.equals(LocationManager.GPS_PROVIDER)||provider.equals(LocationManager.NETWORK_PROVIDER)){locationManager.requestLocationUpdates(provider,0,0,listener,Looper.getMainLooper());enabled=true;}}if(!enabled){stopLocation();location.setText("Ative a localização do Android ou selecione a parada no menu.");return;}handler.postDelayed(gpsTimeout,15000);}catch(SecurityException e){stopLocation();location.setText("Permissão de localização indisponível. Selecione a parada no menu.");}
  }
  private void stopLocation(){handler.removeCallbacks(gpsTimeout);if(locationManager!=null&&listener!=null){locationManager.removeUpdates(listener);listener=null;}locating=false;if(gps!=null){gps.setEnabled(true);gps.setText("⌖ Usar meu GPS");}}
  private Button consultOption(LinearLayout parent,String title,String explanation,String action){
    LinearLayout option=column();option.setPadding(dp(16),dp(16),dp(16),dp(16));
    GradientDrawable surface=background(Color.WHITE,12);surface.setStroke(dp(1),Color.rgb(217,227,219));option.setBackground(surface);
    parent.addView(option);option.addView(text(title,18,INK,true));gap(option,8);option.addView(text(explanation,13,MUTED,false));gap(option,14);
    Button button=button(action,Color.rgb(231,237,223),GREEN);option.addView(button);gap(parent,16);return button;
  }
  private void ui(Runnable action){handler.post(()->{if(!isFinishing()&&!isDestroyed())action.run();});}
  private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return l;}
  private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  private Button button(String value,int color,int textColor){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(textColor);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(background(color,9));b.setMinimumHeight(dp(48));b.setPadding(dp(12),dp(8),dp(12),dp(8));return b;}
  private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private void gap(LinearLayout parent,int height){View gap=new View(this);parent.addView(gap,new LinearLayout.LayoutParams(1,dp(height)));}
  private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
  private void sync(){if(syncing||syncStatus==null)return;syncing=true;lastSync=android.os.SystemClock.elapsedRealtime();syncStatus.setText("Buscando horários na nuvem…");executor.execute(()->{try{JSONObject data=database.download();RoadGeometry.ensure(data);database.store(data);ui(()->{try{routes=CloudDatabase.load(data);refreshStops();render();syncStatus.setText("Nuvem sincronizada · versão "+data.optLong("revision"));}catch(JSONException e){syncStatus.setText("Tabela recebida inválida; mantida a cópia local.");}syncing=false;});}catch(Exception e){ui(()->{syncing=false;syncStatus.setText("Sem atualização da nuvem · usando tabela salva");});}});}
  private void refreshStops(){if(boardingPosition>=Schedule.BOARD_NAMES.length)boardingPosition=4;spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,Schedule.BOARD_NAMES));spinner.setSelection(boardingPosition);}
  private void chooseSuggestion(){new AlertDialog.Builder(this).setTitle("Enviar sugestão").setItems(new String[]{"Alterar uma parada existente","Sugerir uma nova parada"},(d,index)->{newStop=index==1;if(newStop){suggestionTarget="";showSuggestion("Nova parada",null);}else new AlertDialog.Builder(this).setTitle("Qual parada quer alterar?").setItems(Schedule.BOARD_NAMES,(dialog,board)->{suggestionTarget=Schedule.BOARD_IDS[board];showSuggestion(Schedule.BOARD_NAMES[board],board);}).setNegativeButton("Cancelar",null).show();}).setNegativeButton("Cancelar",null).show();}
  private void showSuggestion(String name,Integer board){suggestionLat=null;suggestionLon=null;LinearLayout form=column();form.setPadding(dp(20),dp(12),dp(20),dp(12));form.addView(text(newStop?"Indique o nome, a linha e o local da nova parada.":"Informe o novo nome e/ou escolha outro local. A mudança depende da análise do administrador.",13,MUTED,false));suggestionName=new EditText(this);suggestionName.setHint(newStop?"Nome da nova parada":"Novo nome da parada (opcional)");suggestionName.setSingleLine(true);form.addView(suggestionName);if(newStop){Spinner line=new Spinner(this);line.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,Schedule.ROUTE_NAMES));form.addView(line);line.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){suggestionRoute=Schedule.ROUTE_NAMES[position];}});}suggestionPoint=text("Nenhum novo ponto selecionado.",12,MUTED,false);form.addView(suggestionPoint);Button map=button("Escolher no mapa / usar minha localização",Color.rgb(208,247,119),GREEN);form.addView(map);map.setOnClickListener(v->{double lat=board==null?-3.88:Schedule.BOARD_COORDS[board][0],lon=board==null?-38.62:Schedule.BOARD_COORDS[board][1];startActivityForResult(new android.content.Intent(this,MapPickerActivity.class).putExtra("name",name).putExtra("lat",lat).putExtra("lon",lon),210);});suggestionReason=new EditText(this);suggestionReason.setHint("Explique sua sugestão (opcional)");suggestionReason.setMinLines(2);form.addView(suggestionReason);ScrollView scroll=new ScrollView(this);scroll.addView(form);suggestionDialog=new AlertDialog.Builder(this).setTitle(name).setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Enviar para análise",null).create();suggestionDialog.setOnShowListener(d->suggestionDialog.getButton(-1).setOnClickListener(v->sendSuggestion()));suggestionDialog.show();suggestionDialog.getButton(-1).setEnabled(false);executor.execute(()->{try{JSONObject settings=database.suggestionSettings();ui(()->{if(suggestionDialog!=null&&suggestionDialog.isShowing()){boolean receiving=settings.optBoolean("accepting",false);suggestionDialog.getButton(-1).setEnabled(receiving);if(!receiving)suggestionPoint.setText("O administrador pausou o recebimento de sugestões.");}});}catch(Exception e){ui(()->{if(suggestionDialog!=null&&suggestionDialog.isShowing())suggestionPoint.setText("Não foi possível verificar o recebimento. Tente novamente com internet.");});}});}
  @Override protected void onActivityResult(int request,int result,android.content.Intent intent){super.onActivityResult(request,result,intent);if(request==220&&result==RESULT_OK&&intent!=null){double lat=intent.getDoubleExtra("lat",Double.NaN),lon=intent.getDoubleExtra("lon",Double.NaN);if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180){passengerLat=lat;passengerLon=lon;passengerAccuracy=0;pointMode=true;render();}return;}if(request==210&&result==RESULT_OK&&intent!=null){double lat=intent.getDoubleExtra("lat",Double.NaN),lon=intent.getDoubleExtra("lon",Double.NaN);if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180){suggestionLat=lat;suggestionLon=lon;if(suggestionPoint!=null)suggestionPoint.setText(String.format(Locale.US,"Novo ponto: %.6f, %.6f",lat,lon));}}}
  private void sendSuggestion(){try{String name=suggestionName.getText().toString().trim(),reason=suggestionReason.getText().toString().trim();if(name.length()>100||reason.length()>1000)throw new Exception("Use até 100 caracteres no nome e 1000 na observação.");if(newStop&&(name.isEmpty()||suggestionLat==null))throw new Exception("Nova parada exige nome e localização no mapa.");if(!newStop&&name.isEmpty()&&suggestionLat==null)throw new Exception("Informe um novo nome ou selecione um ponto no mapa.");JSONObject proposal=new JSONObject().put("operation",newStop?"add":"update").put("target",suggestionTarget).put("name",name).put("route",newStop?suggestionRoute:"").put("reason",reason).put("lat",suggestionLat==null?JSONObject.NULL:suggestionLat).put("lon",suggestionLon==null?JSONObject.NULL:suggestionLon);suggestionDialog.getButton(-1).setEnabled(false);executor.execute(()->{try{database.sendSuggestion(proposal);ui(()->{suggestionDialog.dismiss();Toast.makeText(this,"Sugestão enviada. Ela aguarda análise do administrador.",Toast.LENGTH_LONG).show();});}catch(Exception e){ui(()->{suggestionDialog.getButton(-1).setEnabled(true);new AlertDialog.Builder(this).setTitle("Não foi possível enviar").setMessage(e.getMessage()).setPositiveButton("Entendi",null).show();});}});}catch(Exception e){new AlertDialog.Builder(this).setTitle("Confira sua sugestão").setMessage(e.getMessage()).setPositiveButton("Entendi",null).show();}}
  @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("point_mode",pointMode);state.putDouble("point_lat",passengerLat);state.putDouble("point_lon",passengerLon);state.putDouble("point_accuracy",passengerAccuracy);for(String name:Schedule.ROUTE_NAMES)state.putString("direction_"+name,directions.getOrDefault(name,""));super.onSaveInstanceState(state);}
  @Override protected void onDestroy(){super.onDestroy();if(updates!=null)updates.close();handler.removeCallbacksAndMessages(null);stopLocation();executor.shutdown();}
}









