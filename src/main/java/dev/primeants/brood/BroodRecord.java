package dev.primeants.brood;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A physical pile slot, never an adult population counter. */
public final class BroodRecord {
    private final UUID id, queen;
    private final int slot;
    BroodStage stage = BroodStage.EGG;
    long progress, nourishment;
    boolean founding=true;
    public static final long DEFAULT_NEGLECT=24_000, DEFAULT_COCOON_WAIT=24_000;
    private long neglectGrace=DEFAULT_NEGLECT,waitingBound=DEFAULT_COCOON_WAIT;
    long neglectTicks,waitingTicks;
    String neglectReason="";
    public static long selectedDuration(String key,long fallback){long n=Long.parseLong(System.getProperty(key,Long.toString(fallback)));if(n<1)throw new IllegalArgumentException("Brood duration must be positive: "+key);return n;}
    public long neglectGrace(){return neglectGrace;} public long waitingBound(){return waitingBound;}
    public long neglectTicks(){return neglectTicks;} public long waitingTicks(){return waitingTicks;}
    /** Why care last failed, or empty while cared for. */
    public String neglectReason(){return neglectReason;}
    public BroodRecord(UUID id,UUID queen,int slot,long neglect,long waiting){this(id,queen,slot);if(neglect<1||waiting<1)throw new IllegalArgumentException("Invalid brood birth policy");neglectGrace=neglect;waitingBound=waiting;}
    private final dev.primeants.worker.Nutrition nutrition=new dev.primeants.worker.Nutrition();
    public boolean founding(){return founding;}
    public dev.primeants.worker.Nutrition nutrition(){return nutrition;}
    public BroodRecord(UUID id, UUID queen, int slot) { this.id = id; this.queen = queen; this.slot = slot; }
    public UUID id() { return id; }
    public UUID queenId() { return queen; }
    public int slot() { return slot; }
    public BroodStage stage() { return stage; }
    public long progress() { return progress; }
    public long nourishment() { return nourishment; }
    public UUID workerId() { return UUID.nameUUIDFromBytes(("prime_ants:worker:" + id).getBytes(StandardCharsets.UTF_8)); }
    void save(ValueOutput out) {
        out.putString("Id", id.toString()); out.putString("Queen", queen.toString()); out.putInt("Slot", slot);
        out.putString("Stage", stage.name()); out.putLong("Progress", progress); out.putLong("Nourishment", nourishment);
        out.putLong("NeglectGrace",neglectGrace);out.putLong("WaitingBound",waitingBound);out.putLong("NeglectTicks",neglectTicks);out.putLong("WaitingTicks",waitingTicks);out.putString("NeglectReason",neglectReason);
        out.putBoolean("Founding",founding);nutrition.save(out.child("Nutrition"));
    }
    static BroodRecord load(ValueInput in) {
        BroodRecord r = new BroodRecord(UUID.fromString(in.getStringOr("Id", "")), UUID.fromString(in.getStringOr("Queen", "")), in.getIntOr("Slot", -1));
        r.stage = BroodStage.valueOf(in.getStringOr("Stage", ""));
        r.progress = in.getLongOr("Progress", -1); r.nourishment = in.getLongOr("Nourishment", -1);
        r.neglectGrace=in.getLongOr("NeglectGrace",DEFAULT_NEGLECT);r.waitingBound=in.getLongOr("WaitingBound",DEFAULT_COCOON_WAIT);
        r.neglectTicks=in.getLongOr("NeglectTicks",0);r.waitingTicks=in.getLongOr("WaitingTicks",0);r.neglectReason=in.getStringOr("NeglectReason","");
        if(r.neglectGrace<1||r.waitingBound<1||r.neglectTicks<0||r.waitingTicks<0)throw new IllegalArgumentException("Invalid persisted brood survival");
        r.founding=in.getBooleanOr("Founding",true);r.nutrition.load(in.childOrEmpty("Nutrition"),dev.primeants.worker.Nutrition.LARVA_SUGAR,dev.primeants.worker.Nutrition.LARVA_PROTEIN);
        if (r.stage == BroodStage.EMPTY || r.slot < 0 || r.slot >= BroodCapacity.MAX_SLOTS || r.progress < 0
                || r.nourishment < 0 || r.nourishment > BroodPile.LARVA_COST
                || r.stage == BroodStage.COCOON && r.nourishment != BroodPile.LARVA_COST) throw new IllegalArgumentException("Invalid brood record");
        if(!r.founding&&(r.nourishment!=r.nutrition.spentSugar()+r.nutrition.spentProtein()||r.nutrition.gainedSugar()>dev.primeants.worker.Nutrition.LARVA_SUGAR||r.nutrition.gainedProtein()>dev.primeants.worker.Nutrition.LARVA_PROTEIN))throw new IllegalArgumentException("Invalid fed larval accounting");
        return r;
    }
}
