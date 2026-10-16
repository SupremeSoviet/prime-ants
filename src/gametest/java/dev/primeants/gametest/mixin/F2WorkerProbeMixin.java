package dev.primeants.gametest.mixin;

import dev.primeants.entity.LasiusNigerEntity;
import dev.primeants.gametest.F2CarryProbe;
import dev.primeants.worker.WorkerTasks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(WorkerTasks.class)
public abstract class F2WorkerProbeMixin {
    @Shadow @Final private LasiusNigerEntity worker;
    @Shadow private int searchTicks,phaseTicks,cooldown,droppedCursor;
    @Inject(method="tick",at=@At("HEAD"))
    private void before(ServerLevel l,CallbackInfo ci){F2CarryProbe.current=F2CarryProbe.observes(worker)?worker:null;F2CarryProbe.state(worker,"tick_head");}
    @Inject(method="tick",at=@At("RETURN"))
    private void after(ServerLevel l,CallbackInfo ci){F2CarryProbe.state(worker,"tick_return");if(F2CarryProbe.observes(worker))F2CarryProbe.event(worker,"counters",searchTicks+":"+phaseTicks+":"+cooldown+":"+droppedCursor);F2CarryProbe.current=null;}
    @Inject(method="materials",at=@At("HEAD"))
    private void materials(ServerLevel l,CallbackInfo ci){F2CarryProbe.event(worker,"materials_call",null);}
    @Inject(method="search",at=@At("HEAD"))
    private void search(ServerLevel l,CallbackInfo ci){F2CarryProbe.event(worker,"search_call",null);}
    @Inject(method="pickup",at=@At("HEAD"))
    private void pickup(ServerLevel l,CallbackInfo ci){F2CarryProbe.event(worker,"pickup_call",null);}
    @Inject(method="deposit",at=@At("HEAD"))
    private void deposit(ServerLevel l,CallbackInfo ci){F2CarryProbe.event(worker,"deposit_call",null);}
    @Inject(method="authorized",at=@At("RETURN"))
    private void authorized(ServerLevel l,CallbackInfoReturnable<Boolean> ci){F2CarryProbe.event(worker,"authorized_return",ci.getReturnValue());}
    @Inject(method="adultMeal",at=@At("RETURN"))
    private void meal(ServerLevel l,CallbackInfoReturnable<Boolean> ci){F2CarryProbe.event(worker,"meal_return",ci.getReturnValue());}
    @Inject(method="storable",at=@At("RETURN"))
    private void storable(ServerLevel l,ItemStack s,CallbackInfoReturnable<Boolean> ci){F2CarryProbe.event(worker,"storable_return",s+":"+ci.getReturnValue());}
    @Inject(method="harvestRoom",at=@At("RETURN"))
    private void harvest(ServerLevel l,boolean prey,CallbackInfoReturnable<Boolean> ci){F2CarryProbe.event(worker,"native_admission_return",prey+":"+ci.getReturnValue());}
    @Inject(method="collectableFood",at=@At("RETURN"))
    private void food(ServerLevel l,CallbackInfoReturnable<ItemEntity> ci){var i=ci.getReturnValue();F2CarryProbe.event(worker,"food_preemption_return",i==null?null:i.getUUID()+":"+i.getItem());}
}
