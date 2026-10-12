package br.com.meuonibus;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.concurrent.*;

/** Real SQLite, HTTP and Android lifecycle checks. Never runs against the passenger package. */
public final class AuditChecks extends Instrumentation {
  private int failures;private Bundle arguments;
  @Override public void onCreate(Bundle args){super.onCreate(args);arguments=args;start();}
  private interface Check {void run() throws Exception;}
  private void check(String name,Check action){Bundle result=new Bundle();try{action.run();result.putString("PASS",name);}catch(Throwable error){failures++;result.putString("FAIL",name+": "+error.getMessage());}sendStatus(0,result);}
  private void require(boolean value,String message){if(!value)throw new AssertionError(message);}
  private JSONObject initial()throws Exception {try(java.io.InputStream in=getTargetContext().getAssets().open("schedules.json")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new JSONObject(out.toString("UTF-8")).put("revision",2);}}
  private void rejected(JSONObject data)throws Exception {boolean rejected=false;try{CloudDatabase.validate(data);}catch(JSONException expected){rejected=true;}require(rejected,"Dados inválidos foram aceitos");}
  private View find(View root,String label,Class<?> type){if(type.isInstance(root)&&(!(root instanceof TextView)||label.isEmpty()||((TextView)root).getText().toString().equals(label)))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View result=find(group.getChildAt(i),label,type);if(result!=null)return result;}}return null;}
  private boolean contains(View root,String text){if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return true;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)if(contains(group.getChildAt(i),text))return true;}return false;}
  private <T extends View>T view(Activity activity,String label,Class<T> type){View[] result={null};runOnMainSync(()->result[0]=find(activity.getWindow().getDecorView(),label,type));require(result[0]!=null,"Controle ausente: "+label);return type.cast(result[0]);}
  private boolean evaluate(WebView web,String script)throws Exception {CountDownLatch latch=new CountDownLatch(1);String[] result={""};runOnMainSync(()->web.evaluateJavascript(script,value->{result[0]=value;latch.countDown();}));require(latch.await(4,TimeUnit.SECONDS),"WebView sem resposta");return "true".equals(result[0]);}
  private void ready(WebView web)throws Exception {long deadline=SystemClock.elapsedRealtime()+25000;while(SystemClock.elapsedRealtime()<deadline){if(evaluate(web,"Boolean(window.BusMap && window.busMap)"))return;Thread.sleep(200);}throw new AssertionError("Mapa não carregou no prazo; não foi possível verificar o fluxo");}
  @Override public void onStart(){
    Bundle result=new Bundle();
    if(!getTargetContext().getPackageName().endsWith(".audit")||!CloudDatabase.ENDPOINT.startsWith("http://127.0.0.1:8187/")){result.putString("audit","BLOCKED: exige pacote .audit e API local");finish(Activity.RESULT_CANCELED,result);return;}
    check("Cache íntegro após rejeitar uma parada sem coordenadas",()->{
      CloudDatabase database=new CloudDatabase(getTargetContext());JSONObject good=initial();database.store(good);
      JSONObject bad=new JSONObject(good.toString());bad.getJSONObject("data").put("additionalStops",new JSONArray().put(new JSONObject().put("id","test-extra").put("name","Teste").put("confirmed",true).put("routes",new JSONArray().put("Tabatinga"))));
      try{boolean rejected=false;try{database.store(bad);}catch(JSONException expected){rejected=true;}require(rejected,"Cache foi substituído por dados inválidos");require(database.snapshot().toString().equals(good.toString()),"Última tabela válida foi perdida");}finally{database.store(good);database.close();}
    });
    check("Revisão fracionária rejeitada",()->{rejected(initial().put("revision",2.5));});
    check("Origem com latitude fora do planeta rejeitada",()->{JSONObject bad=initial();bad.getJSONObject("data").getJSONArray("routes").getJSONObject(0).getJSONObject("origin").put("lat",91);rejected(bad);});
    check("304 mantém a geometria e os metadados locais",()->{
      CloudDatabase database=new CloudDatabase(getTargetContext());JSONObject local=initial();local.getJSONObject("data").put("localMarker","keep-on-304");database.store(local);
      try{JSONObject response=database.download();require(response.getJSONObject("data").optString("localMarker").equals("keep-on-304"),"Tabela inalterada foi baixada novamente");}finally{database.store(initial());database.close();}
    });
    check("Mapa preserva seleção após recriação",()->{
      MapPickerActivity first=(MapPickerActivity)startActivitySync(new Intent(getTargetContext(),MapPickerActivity.class).putExtra("name","Parada de teste").putExtra("lat",-3.89372).putExtra("lon",-38.68215).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      ready(view(first,"",WebView.class));evaluate(view(first,"",WebView.class),"(window.BusMap.select(-3.89375,-38.68216),true)");waitForIdleSync();
      require(view(first,"Usar este ponto",Button.class).isEnabled(),"Seleção não chegou ao app");
      ActivityMonitor monitor=addMonitor(MapPickerActivity.class.getName(),null,false);runOnMainSync(first::recreate);
      Activity restored=monitor.waitForActivityWithTimeout(10000);removeMonitor(monitor);require(restored!=null,"Recriação não ocorreu");waitForIdleSync();
      try{require(view(restored,"Usar este ponto",Button.class).isEnabled(),"Seleção foi perdida ao recriar");boolean[] has={false};runOnMainSync(()->has[0]=contains(restored.getWindow().getDecorView(),"-3.893750"));require(has[0],"Coordenada selecionada não foi restaurada");}finally{runOnMainSync(restored::finish);}
    });
    check("Trocar parada sem toque sai da previsão por ponto",()->{
      MainActivity main=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
      try{
        Spinner spinner=view(main,"",Spinner.class);runOnMainSync(()->spinner.setSelection(4));waitForIdleSync();
        ActivityMonitor monitor=addMonitor(MapPickerActivity.class.getName(),null,false);
        Button choose=view(main,"Escolher ponto na pista pelo mapa",Button.class);runOnMainSync(choose::performClick);
        Activity picker=monitor.waitForActivityWithTimeout(10000);removeMonitor(monitor);require(picker!=null,"Seletor não abriu");
        WebView web=view(picker,"",WebView.class);ready(web);evaluate(web,"(window.BusMap.select(0,0),true)");waitForIdleSync();
        Button accept=view(picker,"Usar este ponto",Button.class);runOnMainSync(accept::performClick);waitForIdleSync();
        boolean[] pointMode={false};runOnMainSync(()->pointMode[0]=contains(main.getWindow().getDecorView(),"Nenhum trajeto cadastrado a até 200 m"));require(pointMode[0],"Não entrou na consulta pelo ponto escolhido");
        runOnMainSync(()->spinner.setSelection(5));waitForIdleSync();
        runOnMainSync(()->pointMode[0]=contains(main.getWindow().getDecorView(),"Nenhum trajeto cadastrado a até 200 m"));require(!pointMode[0],"Menu mudou, mas a consulta ficou no ponto anterior");
      }finally{runOnMainSync(main::finish);}
    });
    result.putString("audit",failures==0?"PASS":"FAIL");result.putInt("failures",failures);finish(failures==0?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
  }
}
