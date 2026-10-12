package br.com.meuonibus;
public final class UpdatePolicyCheck {
  private static final String URL="https://meu-onibus.miguel-angelo09.chatgpt.site/downloads/Meu-Onibus.apk",HASH="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
  private static UpdatePolicy.Release release(long code,String url,String hash,long bytes){return new UpdatePolicy.Release("br.com.meuonibus",code,"0.5.0",url,hash,bytes,"Novidades");}
  private static void invalid(Runnable action,String message){boolean rejected=false;try{action.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,message);}
  public static void main(String[] args){
    UpdatePolicy.Release release=release(6,URL,HASH,1000);
    check(release.newerThan(5),"A version 5 user must receive version 6");check(!release.newerThan(6),"No repeated prompt for current version");check(!release.newerThan(7),"No downgrade prompt");
    check(release.matches(HASH,1000),"Correct digest and size");check(!release.matches("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",1000),"Reject tampered APK digest");check(!release.matches(HASH,999),"Reject short download");check(!release.matches(HASH,1001),"Reject oversized download");
    invalid(()->release(0,URL,HASH,1000),"Zero version code");release(1,URL,HASH,1000);
    invalid(()->release(6,URL,HASH,0),"Empty APK");release(6,URL,HASH,1);release(6,URL,HASH,10485760);invalid(()->release(6,URL,HASH,10485761),"APK beyond size limit");
    invalid(()->release(6,URL.replace("https:","http:"),HASH,1000),"HTTP forbidden");invalid(()->release(6,URL.replace("meu-onibus.miguel-angelo09.chatgpt.site","other.example"),HASH,1000),"Another host forbidden");invalid(()->release(6,URL.replace("/downloads/Meu-Onibus.apk","/downloads/admin.apk"),HASH,1000),"Another APK path forbidden");invalid(()->release(6,URL+"?redirect=elsewhere",HASH,1000),"Query forbidden");invalid(()->release(6,URL+"#file",HASH,1000),"Fragment forbidden");invalid(()->release(6,URL.replace("https://","https://user@"),HASH,1000),"Credentials forbidden");invalid(()->release(6,URL.replace(".site/",".site:443/"),HASH,1000),"Unexpected port forbidden");
    invalid(()->release(6,URL,"aaaa",1000),"Malformed checksum");invalid(()->new UpdatePolicy.Release("br.com.meuonibus.admin",6,"0.5.0",URL,HASH,1000,""),"Wrong package forbidden");invalid(()->new UpdatePolicy.Release("br.com.meuonibus",6,"beta",URL,HASH,1000,""),"Invalid version name");
    System.out.println("PASS: new-version notification, equal/older versions, checksum, size limits and trusted APK URL");
  }
}
