package br.com.meuonibus.core;

public final class ItineraryPolicyCheck {
  private static void check(boolean result,String why){if(!result)throw new AssertionError(why);}
  public static void main(String[] args){
    check(ItineraryPolicy.usableAccuracy(true,99.9),"GPS below 100 m uncertainty");
    check(ItineraryPolicy.usableAccuracy(true,100),"GPS at 100 m uncertainty");
    check(!ItineraryPolicy.usableAccuracy(true,100.1),"GPS over 100 m uncertainty");
    check(!ItineraryPolicy.usableAccuracy(true,400),"Coarse location cannot claim a 13 m distance");
    check(!ItineraryPolicy.usableAccuracy(false,10),"Unknown GPS accuracy");
    check(!ItineraryPolicy.usableAccuracy(true,Double.NaN),"Invalid GPS accuracy");
    check(!ItineraryPolicy.usableAccuracy(true,-1),"Negative GPS accuracy");
    check(ItineraryPolicy.provenance(true,3,"admin-traced","Conferido em viagem"),"Manual itinerary with evidence");
    check(ItineraryPolicy.provenance(true,3,"official","https://operator.invalid/route"),"Official itinerary with reference");
    check(!ItineraryPolicy.provenance(false,3,"official","Reference"),"Unreviewed itinerary");
    check(!ItineraryPolicy.provenance(true,2,"official","Reference"),"Old driving geometry");
    check(!ItineraryPolicy.provenance(true,3,"driving","Reference"),"Driving engine cannot prove a bus route");
    check(!ItineraryPolicy.provenance(true,3,"admin-traced","  "),"Missing evidence");
    double[] a={0,0},b={0,.002};double[][] path={{0,0},{0,.001},{0,.002}};
    check(ItineraryPolicy.path(path,a,b),"Continuous marked itinerary");
    check(!ItineraryPolicy.path(path,b,a),"Do not reverse the direction silently");
    check(!ItineraryPolicy.path(new double[][]{{0,0},{0,.003}},a,new double[]{0,.003}),"Long straight shortcut is not a marked route");
    check(ItineraryPolicy.path(new double[][]{{0,0},{0,.00224}},a,new double[]{0,.00224}),"Leg below 250 m");
    check(!ItineraryPolicy.path(new double[][]{{0,0},{0,.00226}},a,new double[]{0,.00226}),"Leg above 250 m");
    check(ItineraryPolicy.path(new double[][]{{.00044,0},{0,.001},{0,.002}},a,b),"Start within 50 m");
    check(!ItineraryPolicy.path(new double[][]{{.00046,0},{0,.001},{0,.002}},a,b),"Start over 50 m");
    check(!ItineraryPolicy.path(path,a,new double[]{.00046,.002}),"End over 50 m");
    check(!ItineraryPolicy.path(new double[][]{{0,0},{91,0}},a,b),"Invalid latitude");
    check(!ItineraryPolicy.path(new double[][]{{0,0},{Double.NaN,0}},a,b),"Non-finite position");
    check(!ItineraryPolicy.path(path,null,b),"Missing confirmed origin");
    check(!ItineraryPolicy.path(new double[][]{{0,0},{0,0}},a,a),"Degenerate itinerary");
    System.out.println("PASS: confirmed provenance, direction, endpoints and continuous bus itinerary");
  }
}
