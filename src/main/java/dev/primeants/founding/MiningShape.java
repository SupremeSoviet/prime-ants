package dev.primeants.founding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** First bounded gallery: beyond the store passage, leaving all room walls/supports intact. No world reads. */
public final class MiningShape {
    public static final int RADIUS=16, MIN_DEPTH=1, MAX_DEPTH=6, MAX_EDITS=64;
    public static final Map<String,String> UNITS=Map.of(
        "minecraft:stone","minecraft:cobblestone", "minecraft:clay","minecraft:clay_ball",
        "minecraft:gravel","minecraft:gravel", "minecraft:sand","minecraft:sand",
        "minecraft:coal_ore","minecraft:coal", "minecraft:copper_ore","minecraft:raw_copper",
        "minecraft:iron_ore","minecraft:raw_iron");
    private MiningShape(){}
    public static boolean within(int x,int dy,int z){
        return (long)x*x+(long)z*z<=RADIUS*RADIUS && dy<=-MIN_DEPTH && dy>=-MAX_DEPTH;
    }
    public static List<NestBlueprint.Cell> cells(){
        var out=new ArrayList<NestBlueprint.Cell>();
        for(int f=8;f<=15;f++)for(int y=-2;y<=-1;y++)out.add(new NestBlueprint.Cell(f,0,y));
        return List.copyOf(out);
    }
}
