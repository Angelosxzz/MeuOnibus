package br.com.meuonibus;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;
import android.view.WindowInsets;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Handler;
import android.os.Looper;

/** Only selects a point; publication is handled and authorized by MainActivity. */
public final class MapPickerActivity extends Activity {
  private WebView map;private TextView coordinates;private Button accept;private double lat,lon;private boolean selected;private boolean readOnly;
  private Button gps;private LocationManager manager;private LocationListener listener;private final Handler handler=new Handler(Looper.getMainLooper());private Location userLocation;
  private final Runnable timeout=()->{stopGps();coordinates.setText("O GPS demorou para responder. Confira se a localização do Android está ativa.");};
  @Override public void onCreate(Bundle state){super.onCreate(state);readOnly=getIntent().getBooleanExtra("readOnly",false);lat=getIntent().getDoubleExtra("lat",-3.88);lon=getIntent().getDoubleExtra("lon",-38.62);if(state!=null){double savedLat=state.getDouble("map_lat",lat),savedLon=state.getDouble("map_lon",lon);if(Double.isFinite(savedLat)&&Math.abs(savedLat)<=90&&Double.isFinite(savedLon)&&Math.abs(savedLon)<=180){lat=savedLat;lon=savedLon;selected=state.getBoolean("map_selected",false);}}LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(244,246,242));
    if(android.os.Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(0,bars.top,0,bars.bottom);return i;});else root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(0,i.getSystemWindowInsetTop(),0,i.getSystemWindowInsetBottom());return i;});
    TextView title=new TextView(this);title.setText("Selecionar: "+getIntent().getStringExtra("name"));title.setTextSize(20);title.setTextColor(Color.rgb(11,85,71));title.setPadding(24,16,24,12);root.addView(title);
    gps=new Button(this);gps.setText("⌖ Minha localização");gps.setAllCaps(false);root.addView(gps);gps.setOnClickListener(v->requestGps());
    map=new WebView(this);map.getSettings().setJavaScriptEnabled(true);map.getSettings().setUserAgentString(map.getSettings().getUserAgentString()+" MeuOnibusAndroid");map.getSettings().setDomStorageEnabled(true);map.getSettings().setAllowFileAccess(false);map.getSettings().setAllowContentAccess(false);map.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);map.addJavascriptInterface(new Object(){@JavascriptInterface public void select(double latitude,double longitude){if(!Double.isFinite(latitude)||Math.abs(latitude)>90||!Double.isFinite(longitude)||Math.abs(longitude)>180)return;runOnUiThread(()->{lat=latitude;lon=longitude;selected=true;coordinates.setText(String.format(Locale.US,"%.6f, %.6f",lat,lon));accept.setEnabled(true);});}},"BusMap");
    map.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String url){if(!br.com.meuonibus.core.MapUrlPolicy.allowed(url))return;v.evaluateJavascript(String.format(Locale.US,"if(window.busMap){window.busMap.setView([%.8f,%.8f],17);if(typeof marker!=='undefined')marker.setLatLng([%.8f,%.8f]);}",lat,lon,lat,lon),null);showGps();}@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();return !br.com.meuonibus.core.MapUrlPolicy.allowed(u.toString());}@Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError error){if(r.isForMainFrame())coordinates.setText("Não foi possível carregar o mapa. Verifique a conexão.");}});
    root.addView(map,new LinearLayout.LayoutParams(-1,0,1));coordinates=new TextView(this);coordinates.setText(selected?String.format(Locale.US,"%.6f, %.6f",lat,lon):"Toque no mapa para selecionar o ponto exato.");coordinates.setPadding(24,12,24,8);root.addView(coordinates);accept=new Button(this);accept.setText(readOnly?"Fechar mapa":"Usar este ponto");accept.setAllCaps(false);accept.setEnabled(readOnly||selected);root.addView(accept);accept.setOnClickListener(v->{if(readOnly){finish();return;}if(selected){setResult(RESULT_OK,new Intent().putExtra("lat",lat).putExtra("lon",lon));finish();}});
    Button retry=new Button(this);retry.setText("Recarregar mapa");retry.setAllCaps(false);retry.setOnClickListener(v->load());root.addView(retry);setContentView(root);load();
  }
  private void load(){accept.setEnabled(readOnly||selected);coordinates.setText(selected?String.format(Locale.US,"%.6f, %.6f",lat,lon):"Toque no mapa para selecionar o ponto exato.");map.loadUrl("https://meu-onibus.miguel-angelo09.chatgpt.site/map-picker?lat=-3.89372&lon=-38.68215");}
  private void paintRoute(){
    String raw=getIntent().getStringExtra("routePath");if(raw==null||map==null||!br.com.meuonibus.core.MapUrlPolicy.allowed(map.getUrl()))return;
    try{org.json.JSONArray path=new org.json.JSONArray(raw);br.com.meuonibus.core.ItineraryData.points(path);map.evaluateJavascript("(function(){if(!window.busMap||!window.L)return;if(window.busItinerary)busMap.removeLayer(window.busItinerary);window.busItinerary=L.polyline("+path+",{color:'#0b5547',weight:5}).addTo(busMap);if("+readOnly+")busMap.fitBounds(window.busItinerary.getBounds(),{padding:[30,30],maxZoom:17});})();",null);}catch(org.json.JSONException invalid){}
  }
  private void requestGps(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},11);return;}startGps();}
  @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==11){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)startGps();else coordinates.setText("Permissão de localização negada. Você pode selecionar a parada tocando no mapa.");}}
  private void startGps(){stopGps();manager=(LocationManager)getSystemService(LOCATION_SERVICE);gps.setEnabled(false);gps.setText("Localizando…");listener=new LocationListener(){public void onLocationChanged(Location fix){userLocation=fix;showGps();stopGps();}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int state,Bundle b){}};try{boolean enabled=false;for(String provider:manager.getProviders(true)){if(provider.equals(LocationManager.GPS_PROVIDER)&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)continue;if(provider.equals(LocationManager.GPS_PROVIDER)||provider.equals(LocationManager.NETWORK_PROVIDER)){manager.requestLocationUpdates(provider,0,0,listener,Looper.getMainLooper());enabled=true;}}if(!enabled){stopGps();coordinates.setText("Ative a localização do Android para ver sua posição.");return;}handler.postDelayed(timeout,15000);}catch(SecurityException e){stopGps();coordinates.setText("A localização não está autorizada.");}}
  private void showGps(){if(userLocation==null||map==null||!br.com.meuonibus.core.MapUrlPolicy.allowed(map.getUrl()))return;String script=String.format(Locale.US,"if(window.showMyLocation)window.showMyLocation(%.8f,%.8f,%.2f);",userLocation.getLatitude(),userLocation.getLongitude(),userLocation.getAccuracy());map.evaluateJavascript(script,null);coordinates.setText("Sua localização aparece em azul. Toque no mapa para selecionar a parada.");}
  private void stopGps(){handler.removeCallbacks(timeout);if(manager!=null&&listener!=null){manager.removeUpdates(listener);listener=null;}if(gps!=null){gps.setEnabled(true);gps.setText("⌖ Minha localização");}}
  @Override protected void onSaveInstanceState(Bundle state){state.putDouble("map_lat",lat);state.putDouble("map_lon",lon);state.putBoolean("map_selected",selected);super.onSaveInstanceState(state);}
  @Override protected void onPause(){super.onPause();stopGps();}
  @Override protected void onDestroy(){stopGps();if(map!=null){map.removeJavascriptInterface("BusMap");map.destroy();}super.onDestroy();}
}

