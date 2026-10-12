package br.com.meuonibus.core;

/** A routing engine's driving path is never proof of the streets used by a bus. */
public final class ItineraryPolicy {
  private ItineraryPolicy(){}
  public static boolean usableAccuracy(boolean known,double metres){return known&&Double.isFinite(metres)&&metres>=0&&metres<=100;}
  public static boolean provenance(boolean confirmed,int version,String source,String reference){
    return confirmed&&version==3&&("admin-traced".equals(source)||"official".equals(source))&&reference!=null&&!reference.trim().isEmpty()&&reference.length()<=200;
  }
  public static double metres(double[] a,double[] b){
    double lat=Math.toRadians(b[0]-a[0]),lon=Math.toRadians(b[1]-a[1]);
    double h=Math.sin(lat/2)*Math.sin(lat/2)+Math.cos(Math.toRadians(a[0]))*Math.cos(Math.toRadians(b[0]))*Math.sin(lon/2)*Math.sin(lon/2);
    return 6371000*2*Math.atan2(Math.sqrt(Math.min(1,h)),Math.sqrt(Math.max(0,1-h)));
  }
  private static boolean point(double[] p){return p!=null&&p.length==2&&Double.isFinite(p[0])&&Double.isFinite(p[1])&&Math.abs(p[0])<=90&&Math.abs(p[1])<=180;}
  public static boolean path(double[][] points,double[] from,double[] to){
    if(points==null||points.length<2||points.length>1000||!point(from)||!point(to))return false;
    double length=0;
    for(int i=0;i<points.length;i++){if(!point(points[i]))return false;if(i>0){double leg=metres(points[i-1],points[i]);if(leg>250)return false;length+=leg;}}
    return length>=1&&metres(points[0],from)<=50&&metres(points[points.length-1],to)<=50;
  }
}
