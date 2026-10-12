package br.com.meuonibus;

import android.app.*;import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.widget.Toast;
import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import java.util.concurrent.*;

public final class AppUpdates {
  private final Activity activity;private final Handler handler=new Handler(Looper.getMainLooper());private final ExecutorService executor=Executors.newSingleThreadExecutor();
  private boolean checking,downloading,waitingPermission;private volatile boolean closed;private long lastCheck,offered;
  private UpdatePolicy.Release pending;private ProgressDialog progress;
  public AppUpdates(Activity activity){this.activity=activity;}
  private long version(PackageInfo info){return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}
  private PackageInfo installed()throws PackageManager.NameNotFoundException{return activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);}
  private void ui(Runnable action){handler.post(()->{if(!closed&&!activity.isFinishing()&&!activity.isDestroyed())action.run();});}
  public void check(boolean manual){
    if(closed||checking||downloading||(!manual&&lastCheck!=0&&SystemClock.elapsedRealtime()-lastCheck<60000))return;
    checking=true;lastCheck=SystemClock.elapsedRealtime();if(manual)Toast.makeText(activity,"Verificando a versão mais recente…",Toast.LENGTH_SHORT).show();
    executor.execute(()->{try{HttpURLConnection connection=(HttpURLConnection)new URL("https://"+UpdatePolicy.HOST+"/api/app-release").openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("Accept","application/json");
      JSONObject info;try{if(connection.getResponseCode()!=200)throw new IOException("Não foi possível verificar a atualização.");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1){bytes.write(buffer,0,n);if(bytes.size()>12000)throw new IOException("Informação inválida.");}}info=new JSONObject(bytes.toString("UTF-8"));}finally{connection.disconnect();}
      UpdatePolicy.Release release=new UpdatePolicy.Release(info.getString("packageName"),info.getLong("versionCode"),info.getString("versionName"),info.getString("apkUrl"),info.getString("sha256"),info.getLong("bytes"),info.optString("notes",""));PackageInfo current=installed();
      ui(()->{checking=false;if(release.newerThan(version(current))){if(manual||offered!=release.code){offered=release.code;offer(release,current.versionName);}}else if(manual)new AlertDialog.Builder(activity).setTitle("Seu app está atualizado").setMessage("Você está usando a versão "+current.versionName+".").setPositiveButton("Entendi",null).show();});
    }catch(Exception e){ui(()->{checking=false;if(manual)error("Não foi possível verificar agora. Confira sua internet e tente novamente.");});}});
  }
  private void offer(UpdatePolicy.Release release,String current){new AlertDialog.Builder(activity).setTitle("Nova versão do Meu Ônibus").setMessage("Você está usando a versão "+current+". A versão "+release.name+" está disponível.\n\n"+release.notes+"\n\nBaixe a atualização e confirme a instalação no Android. Seus dados do app serão mantidos.").setNegativeButton("Agora não",null).setPositiveButton("Baixar e atualizar",(dialog,which)->download(release)).show();}
  private String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte value:bytes)out.append(String.format(Locale.US,"%02x",value&255));return out.toString();}
  private void verifyArchive(File file,UpdatePolicy.Release release)throws Exception {
    int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;PackageManager pm=activity.getPackageManager();PackageInfo archive=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags),current=pm.getPackageInfo(activity.getPackageName(),flags);
    if(archive==null||!activity.getPackageName().equals(archive.packageName)||version(archive)!=release.code||version(archive)<=version(current))throw new IOException("O arquivo não corresponde à atualização deste app.");
    Signature[] oldSignatures=Build.VERSION.SDK_INT>=28&&current.signingInfo!=null?current.signingInfo.getApkContentsSigners():current.signatures,newSignatures=Build.VERSION.SDK_INT>=28&&archive.signingInfo!=null?archive.signingInfo.getApkContentsSigners():archive.signatures;
    if(oldSignatures==null||newSignatures==null||oldSignatures.length==0||oldSignatures.length!=newSignatures.length)throw new IOException("Assinatura da atualização inválida.");
    for(Signature signature:oldSignatures){boolean found=false;for(Signature candidate:newSignatures)if(Arrays.equals(signature.toByteArray(),candidate.toByteArray()))found=true;if(!found)throw new IOException("A atualização foi assinada por outro desenvolvedor.");}
  }
  public void download(UpdatePolicy.Release release){
    if(downloading||closed)return;downloading=true;progress=new ProgressDialog(activity);progress.setTitle("Atualizando Meu Ônibus");progress.setMessage("Baixando a versão "+release.name+"…");progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);progress.setMax(100);progress.setCancelable(false);progress.show();
    executor.execute(()->{File target=ApkProvider.file(activity),temp=new File(target.getParentFile(),"download-"+UUID.randomUUID()+".apk");try{
      if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Sem espaço para a atualização.");
      HttpURLConnection connection=(HttpURLConnection)new URL(release.url).openConnection();connection.setConnectTimeout(8000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("Accept","application/vnd.android.package-archive");MessageDigest digest=MessageDigest.getInstance("SHA-256");long count=0;
      try{if(connection.getResponseCode()!=200)throw new IOException("Não foi possível baixar o APK.");try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(temp)){byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1){if(closed||Thread.currentThread().isInterrupted())throw new IOException("Download interrompido.");count+=n;if(count>release.bytes)throw new IOException("Tamanho da atualização inválido.");digest.update(buffer,0,n);out.write(buffer,0,n);int percent=(int)(count*100/release.bytes);ui(()->{if(progress!=null)progress.setProgress(percent);});}}}finally{connection.disconnect();}
      if(!release.matches(hex(digest.digest()),count))throw new IOException("O APK baixado não passou na verificação. Tente novamente.");verifyArchive(temp,release);
      if(target.exists()&&!target.delete())throw new IOException("Não foi possível preparar a instalação.");if(!temp.renameTo(target))throw new IOException("Não foi possível preparar a instalação.");
      ui(()->{downloading=false;dismissProgress();pending=release;installOrRequest();});
    }catch(Exception e){temp.delete();ui(()->{downloading=false;dismissProgress();error(e.getMessage()==null?"Não foi possível atualizar.":e.getMessage());});}});
  }
  private void installOrRequest(){
    if(pending==null)return;
    if(!activity.getPackageManager().canRequestPackageInstalls()){
      new AlertDialog.Builder(activity).setTitle("Permitir a atualização").setMessage("O APK já foi baixado e verificado.\n\nNa próxima tela, permita que o Meu Ônibus instale aplicativos. Depois volte para confirmar a atualização no instalador do Android.").setNegativeButton("Agora não",null).setPositiveButton("Abrir configuração",(dialog,which)->{try{waitingPermission=true;activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));}catch(Exception e){waitingPermission=false;error("Abra as configurações do Android e permita instalações pelo Meu Ônibus.");}}).show();return;
    }
    try{verifyArchive(ApkProvider.file(activity),pending);Uri uri=ApkProvider.uri(activity);Intent install=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);install.setClipData(ClipData.newRawUri("Atualização Meu Ônibus",uri));waitingPermission=false;activity.startActivity(install);}catch(Exception e){error("Não foi possível abrir a instalação. "+e.getMessage());}
  }
  public void resumed(){if(waitingPermission){waitingPermission=false;if(activity.getPackageManager().canRequestPackageInstalls()){installOrRequest();return;}}check(false);}
  private void dismissProgress(){if(progress!=null){progress.dismiss();progress=null;}}
  private void error(String message){new AlertDialog.Builder(activity).setTitle("Atualização do aplicativo").setMessage(message).setPositiveButton("Entendi",null).show();}
  public void close(){closed=true;executor.shutdownNow();dismissProgress();handler.removeCallbacksAndMessages(null);}
}

