package dev.primeants.gametest;

import com.google.gson.JsonObject;

/** Comparison operands are observed once, before the GameTest assertion can abort a scenario. */
final class ComparisonEvidence {
    private static boolean log(Object a,Object b,String op,String expression,boolean result){
        var row=new JsonObject();row.addProperty("expression",expression);row.addProperty("operator",op);
        row.addProperty("left",String.valueOf(a));row.addProperty("right",String.valueOf(b));row.addProperty("result",result);
        ReconciliationEvidence.log("t04_comparison","before_assertion",row);return result;
    }
    static boolean longs(long a,long b,String op,String e){return log(a,b,op,e,switch(op){case "=="->a==b;case "!="->a!=b;case "<"->a<b;case "<="->a<=b;case ">"->a>b;case ">="->a>=b;default->throw new IllegalArgumentException(op);});}
    static boolean doubles(double a,double b,String op,String e){return log(a,b,op,e,switch(op){case "=="->a==b;case "!="->a!=b;case "<"->a<b;case "<="->a<=b;case ">"->a>b;case ">="->a>=b;default->throw new IllegalArgumentException(op);});}
    static boolean floats(float a,float b,String op,String e){return log(a,b,op,e,switch(op){case "=="->a==b;case "!="->a!=b;case "<"->a<b;case "<="->a<=b;case ">"->a>b;case ">="->a>=b;default->throw new IllegalArgumentException(op);});}
    static boolean bools(boolean a,boolean b,String op,String e){return log(a,b,op,e,op.equals("==")?a==b:a!=b);}
    static boolean objects(Object a,Object b,String op,String e){return log(a,b,op,e,op.equals("==")?a==b:a!=b);}
    static boolean equal(Object a,Object b,String e){return log(a,b,"equals",e,a.equals(b));}
    static boolean flag(boolean value,String e){return log(value,true,"predicate",e,value);}
}
