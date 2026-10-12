package br.com.meuonibus.core;

import java.net.URI;

/** The bridge and locally injected coordinates must remain on the owned HTTPS map page. */
public final class MapUrlPolicy {
  private MapUrlPolicy(){}
  public static boolean allowed(String value){
    try{URI uri=new URI(value);return "https".equals(uri.getScheme())&&"meu-onibus.miguel-angelo09.chatgpt.site".equals(uri.getHost())&&uri.getUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443)&&"/map-picker".equals(uri.getRawPath());}catch(Exception error){return false;}
  }
}
