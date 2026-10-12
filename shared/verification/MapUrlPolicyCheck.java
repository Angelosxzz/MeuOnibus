package br.com.meuonibus.core;

public final class MapUrlPolicyCheck {
  public static void main(String[] args) {
    String base="https://meu-onibus.miguel-angelo09.chatgpt.site/map-picker";
    if(!MapUrlPolicy.allowed(base+"?lat=-3.88&lon=-38.62") || !MapUrlPolicy.allowed(base.replace(".site/", ".site:443/")))throw new AssertionError("Mapa oficial bloqueado");
    for(String url:new String[]{null,"",base.replace("https:","http:"),base.replace(".site/", ".site.evil.invalid/"),base.replace("https://", "https://user@"),base.replace(".site/", ".site:8443/"),base+"/other",base.replace("/map-picker", "/%6dap-picker"),"javascript:alert(1)","file:///map-picker"}) {
      if(MapUrlPolicy.allowed(url))throw new AssertionError("URL fora da política aceita: "+url);
    }
    System.out.println("PASS: mapa HTTPS, host, porta, credenciais e caminho restritos");
  }
}
