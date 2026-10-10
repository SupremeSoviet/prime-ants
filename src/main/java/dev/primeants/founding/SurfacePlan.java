package dev.primeants.founding;

import com.google.gson.*;
import dev.primeants.colony.ColonyStage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Semantic surface IR, separate from the spoil capacity. Pure compilation, never world clearing. */
public final class SurfacePlan {
    public record Cell(int forward,int side,int layer,String material,String component) {
        public String column(){return forward+","+side;}
        public String key(){return column()+","+layer;}
    }
    public record Plan(String description,ColonyStage stage,List<Cell> cells) {
        public int cost(){return cells.size();}
    }
    private static final Map<ColonyStage,Plan> BUNDLED=new EnumMap<>(ColonyStage.class);
    static {for(var s:List.of(ColonyStage.YOUNG,ColonyStage.MATURE,ColonyStage.GREAT)){
        try(var in=SurfacePlan.class.getResourceAsStream("/data/prime_ants/surface_plans/"+s.serializedName()+".json")){
            if(in==null)throw new IllegalStateException("Missing bundled surface plan "+s);
            BUNDLED.put(s,compile(new String(in.readAllBytes(),StandardCharsets.UTF_8)));
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }}
    public static Plan bundled(ColonyStage s){return BUNDLED.get(s);}
    private static int n(JsonObject o,String k){return o.get(k).getAsInt();}
    private static void column(Map<String,Cell> cells,int f,int s,int height,String purpose){
        for(int y=0;y<height;y++)add(cells,new Cell(f,s,y,"soil",purpose));
    }
    private static void add(Map<String,Cell> cells,Cell c){
        if(c.forward()>=0||c.forward()<-NestMound.BACK||Math.abs(c.side())>NestMound.SIDE||c.layer()<0||c.layer()>=NestMound.LAYERS
            ||c.side()==0&&c.layer()<2||c.forward()==-2&&c.side()==0)throw new IllegalArgumentException("Protected route or footprint: "+c);
        if(cells.putIfAbsent(c.key(),c)!=null)throw new IllegalArgumentException("Overlapping semantic components: "+c);
    }
    public static Plan compile(String json){
        var root=JsonParser.parseString(json).getAsJsonObject();
        if(n(root,"schemaVersion")!=1||!root.get("palette").getAsString().equals("earth"))throw new IllegalArgumentException("Surface schema/palette");
        var stage=ColonyStage.byName(root.get("stage").getAsString());
        if(stage==ColonyStage.FOUNDING||!root.get("mound").getAsString().equals(stage==ColonyStage.YOUNG?"young":"mature"))throw new IllegalArgumentException("Surface mound envelope");
        var cells=new LinkedHashMap<String,Cell>();
        if(stage!=ColonyStage.YOUNG){
            var entrance=root.getAsJsonObject("entrance");int f=n(entrance,"forward"),h=n(entrance,"height");
            if(n(entrance,"width")!=3||h!=3||f!=-1)throw new IllegalArgumentException("Bounded supported entrance primitive");
            for(int s:new int[]{-1,1}){column(cells,f-1,s,1,"entrance_step");column(cells,f,s,h,"arch_pillar");}
            add(cells,new Cell(f,0,h-1,"soil","arch_lintel"));
            var crest=root.getAsJsonObject("moundCrests");int cf=n(crest,"forward"),cs=n(crest,"side"),ch=n(crest,"height");
            if(cf!=-4||cs!=3||ch!=4||n(crest,"count")!=1)throw new IllegalArgumentException("Bounded mound crest primitive");
            column(cells,cf-3,cs,1,"mound_step");column(cells,cf-2,cs,2,"mound_step");column(cells,cf-1,cs,3,"mound_step");column(cells,cf,cs,ch,"mound_crest");
            if(stage==ColonyStage.GREAT){
                var fort=root.getAsJsonObject("fortification");int end=n(fort,"span"),rh=n(fort,"rampartHeight"),ph=n(fort,"postHeight");
                if(end!=4||rh!=2||ph!=5||n(fort,"posts")!=2)throw new IllegalArgumentException("Bounded fortification primitive");
                for(int sign:new int[]{-1,1}){
                    for(int s=2;s<end;s++)column(cells,f,sign*s,rh,"rampart");
                    column(cells,f,sign*end,ph,"watch_post");
                    for(int back=1;back<=4;back++)column(cells,f-back,sign*end,5-back,"post_step");
                    var key=f+","+sign+",0";var old=cells.get(key);cells.put(key,new Cell(old.forward(),old.side(),old.layer(),"gate","gate_jamb"));
                }
            }
        }
        if(cells.size()>64)throw new IllegalArgumentException("Surface edit budget");
        var queue=new ArrayList<>(cells.values());
        queue.sort(Comparator.comparingInt(Cell::layer).thenComparing(Comparator.comparingInt(Cell::forward).reversed()).thenComparingInt(c->Math.abs(c.side())).thenComparingInt(Cell::side));
        var seen=new HashSet<String>();
        // The centre lintel must follow both same-height jambs, even under the ordinary layer order.
        for(int i=0;i<queue.size();i++)if(queue.get(i).component().equals("arch_lintel")){var c=queue.remove(i);queue.add(c);break;}
        for(var c:queue){
            if(c.layer()>0&&!seen.contains(c.forward()+","+c.side()+","+(c.layer()-1))){
                if(!c.component().equals("arch_lintel")||!seen.contains(c.forward()+",-1,"+c.layer())||!seen.contains(c.forward()+",1,"+c.layer()))throw new IllegalArgumentException("Unsupported queue "+c);
            }seen.add(c.key());
        }
        return new Plan(root.toString(),stage,List.copyOf(queue));
    }
    private SurfacePlan(){}
}
