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
    }
    static BroodRecord load(ValueInput in) {
        BroodRecord r = new BroodRecord(UUID.fromString(in.getStringOr("Id", "")), UUID.fromString(in.getStringOr("Queen", "")), in.getIntOr("Slot", -1));
        r.stage = BroodStage.valueOf(in.getStringOr("Stage", ""));
        r.progress = in.getLongOr("Progress", -1); r.nourishment = in.getLongOr("Nourishment", -1);
        if (r.stage == BroodStage.EMPTY || r.slot < 0 || r.slot >= BroodPile.CAPACITY || r.progress < 0
                || r.nourishment < 0 || r.nourishment > BroodPile.LARVA_COST
                || r.stage == BroodStage.COCOON && r.nourishment != BroodPile.LARVA_COST) throw new IllegalArgumentException("Invalid brood record");
        return r;
    }
}
