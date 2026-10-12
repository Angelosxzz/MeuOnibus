package br.com.meuonibus;
import java.net.URI;
public final class UpdatePolicy {
  public static final String HOST="meu-onibus.miguel-angelo09.chatgpt.site";
  public static final class Release {
    public final long code,bytes;public final String name,url,sha256,notes;
    public Release(String packageName,long code,String name,String url,String sha256,long bytes,String notes){
      if(!"br.com.meuonibus".equals(packageName)||code<=0||code>Integer.MAX_VALUE||name==null||!name.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||sha256==null||!sha256.matches("[0-9a-f]{64}")||bytes<=0||bytes>10*1024*1024||notes==null||notes.length()>2000)throw new IllegalArgumentException("Informação de atualização inválida.");
      try{URI uri=new URI(url);if(!"https".equals(uri.getScheme())||!HOST.equals(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1||!"/downloads/Meu-Onibus.apk".equals(uri.getPath())||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException("Endereço de atualização inválido.");}catch(Exception e){throw new IllegalArgumentException("Endereço de atualização inválido.");}
      this.code=code;this.name=name;this.url=url;this.sha256=sha256;this.bytes=bytes;this.notes=notes;
    }
    public boolean newerThan(long installed){return code>installed;}
    public boolean matches(String digest,long actualBytes){return sha256.equals(digest)&&bytes==actualBytes;}
  }
}
