package dev.primeants.gametest.mixin;
import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.founding.NestPlan;
import dev.primeants.gametest.MinerCausalProbe;
import dev.primeants.worker.WorkerTasks;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(WorkerTasks.class)
public abstract class MinerDecisionMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Inject(method="tick",at=@At("HEAD")) private void tickHead(ServerLevel l,CallbackInfo ci){if(MinerCausalProbe.observes(worker))MinerCausalProbe.current=worker;}
    @Inject(method="tick",at=@At("RETURN")) private void tickTail(ServerLevel l,CallbackInfo ci){MinerCausalProbe.state(worker);MinerCausalProbe.current=null;}
    @Inject(method="materials",at=@At("HEAD")) private void materialHead(ServerLevel l,CallbackInfo ci){MinerCausalProbe.begin(worker,"materials");}
    @Inject(method="materials",at=@At("RETURN")) private void materialTail(ServerLevel l,CallbackInfo ci){MinerCausalProbe.end(worker,"materials");}
    @Inject(method="search",at=@At("HEAD")) private void searchHead(ServerLevel l,CallbackInfo ci){MinerCausalProbe.begin(worker,"search");}
    @Inject(method="search",at=@At("RETURN")) private void searchTail(ServerLevel l,CallbackInfo ci){MinerCausalProbe.end(worker,"search");}
    @Inject(method="pickup",at=@At("HEAD")) private void pickupHead(ServerLevel l,CallbackInfo ci){MinerCausalProbe.begin(worker,"pickup");}
    @Inject(method="pickup",at=@At("RETURN")) private void pickupTail(ServerLevel l,CallbackInfo ci){MinerCausalProbe.end(worker,"pickup");}
    @Inject(method="collectableFood",at=@At("HEAD")) private void foodHead(ServerLevel l,CallbackInfoReturnable<ItemEntity> ci){MinerCausalProbe.begin(worker,"collectableFood_second_check");}
    @Inject(method="collectableFood",at=@At("RETURN")) private void foodTail(ServerLevel l,CallbackInfoReturnable<ItemEntity> ci){if(!MinerCausalProbe.observes(worker))return;var i=ci.getReturnValue();MinerCausalProbe.event(worker,"actual_food_preemption_result",i==null?null:MinerCausalProbe.item(i));MinerCausalProbe.end(worker,"collectableFood_second_check");}
    @Inject(method="withinSearch",at=@At("RETURN")) private void domain(BlockPos p,CallbackInfoReturnable<Boolean> ci){if(MinerCausalProbe.active())MinerCausalProbe.event(worker,"actual_withinSearch",p.toShortString()+":"+ci.getReturnValue());}
    @Inject(method="authorized",at=@At("RETURN")) private void auth(ServerLevel l,CallbackInfoReturnable<Boolean> ci){if(MinerCausalProbe.active())MinerCausalProbe.event(worker,"actual_authorized",ci.getReturnValue());}
    @Inject(method="canForage",at=@At("RETURN")) private void forage(NestPlan p,CallbackInfoReturnable<Boolean> ci){MinerCausalProbe.event(worker,"actual_canForage",ci.getReturnValue());}
    @Inject(method="caregiver",at=@At("RETURN")) private void care(ServerLevel l,NestPlan p,CallbackInfoReturnable<Boolean> ci){MinerCausalProbe.event(worker,"actual_caregiver",ci.getReturnValue());}
    @Inject(method="storable",at=@At("RETURN")) private void room(ServerLevel l,ItemStack s,CallbackInfoReturnable<Boolean> ci){if(MinerCausalProbe.active())MinerCausalProbe.event(worker,"actual_food_storable",s+":"+ci.getReturnValue());}
    @Redirect(method={"materials","collectableFood"},at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V"))
    private <T extends Entity> void query(ServerLevel l,EntityTypeTest<Entity,T> kind,AABB box,Predicate<? super T> predicate,List<? super T> result,int limit){
        if(!MinerCausalProbe.observes(worker)){l.getEntities(kind,box,predicate,result,limit);return;}
        MinerCausalProbe.event(worker,"actual_query_entry",Map.of("bounds",box.toString(),"limit",limit));
        l.getEntities(kind,box,i->{boolean accepted;MinerCausalProbe.predicateCall=true;try{accepted=predicate.test(i);}finally{MinerCausalProbe.predicateCall=false;}MinerCausalProbe.event(worker,"actual_query_predicate",Map.of("item",i instanceof ItemEntity item?MinerCausalProbe.item(item):i.toString(),"accepted",accepted));return accepted;},result,limit);
        MinerCausalProbe.event(worker,"actual_query_return",Map.of("size",result.size(),"atLimit_not_proof_of_omission",result.size()==limit,"order",result.stream().map(v->v instanceof ItemEntity item?MinerCausalProbe.item(item):v.toString()).toList()));
    }
    @Redirect(method={"materials","collectableFood"},at=@At(value="INVOKE",target="Ljava/util/ArrayList;sort(Ljava/util/Comparator;)V"))
    private void sort(ArrayList<ItemEntity> list,Comparator<? super ItemEntity> compare){list.sort(compare);if(MinerCausalProbe.observes(worker))MinerCausalProbe.event(worker,"actual_sorted_candidates",list.stream().map(MinerCausalProbe::item).toList());}
}
