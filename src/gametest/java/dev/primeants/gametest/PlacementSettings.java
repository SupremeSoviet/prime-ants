package dev.primeants.gametest;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import java.util.*;
import net.minecraft.world.level.ChunkPos;

/** Development-only declared generation settings, separate from retained origin fixtures. */
public final class PlacementSettings implements ModInitializer {
    public static TicketType TICKET;
    public static final Set<ChunkPos> NATIVE_FULL=new HashSet<>();
    public static final Set<String> ALL_FULL=new HashSet<>();
    @Override public void onInitialize() {
        String dimensions="prime_ants_test:placement_soil,prime_ants_test:placement_desert,prime_ants_test:placement_stone,prime_ants_test:placement_retry,prime_ants_test:placement_history,prime_ants_test:placement_negative";
        if(System.getProperty("prime_ants.placementSeed")!=null) dimensions+=",prime_ants_test:placement_native";
        System.setProperty("prime_ants.developmentPlacementDimensions",dimensions);
        TICKET=Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.fromNamespaceAndPath("prime_ants_test","placement"),new TicketType(0,14));
        ServerChunkEvents.CHUNK_LOAD.register((l,c,g)->{
            if(System.getProperty("prime_ants.placementSeed")!=null && g) {
                ALL_FULL.add(l.dimension().identifier()+":"+c.getPos().x()+":"+c.getPos().z());
                if(ALL_FULL.size()>100)throw new IllegalStateException("Total diagnostic FULL budget exceeded");
            }
            if(l.dimension().identifier().toString().equals("prime_ants_test:placement_native") && g) {
                var p=c.getPos();NATIVE_FULL.add(p);
                if(p.x() < -1 || p.x()>8 || p.z() < -1 || p.z()>8 || NATIVE_FULL.size()>100)
                    throw new IllegalStateException("Native placement FULL budget/region exceeded: "+p);
            }
        });
    }
}
