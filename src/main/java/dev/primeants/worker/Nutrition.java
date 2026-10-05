package dev.primeants.worker;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Recipient-owned ingested stores. Receipts describe terminal consumption and cannot be spent as food. */
public final class Nutrition {
    public static final long APPLE_SUGAR=4000, BERRY_SUGAR=2000, CHICKEN_PROTEIN=8000;
    public static final long QUEEN_SUGAR_CAPACITY=8000, QUEEN_PROTEIN_CAPACITY=16000;
    public static final long EGG_SUGAR=1000, EGG_PROTEIN=2000;
    public static final long LARVA_SUGAR=4000, LARVA_PROTEIN=8000;
    private long sugar,protein,apples,berries,chickens,spentSugar,spentProtein;
    public long sugar(){return sugar;} public long protein(){return protein;}
    public long apples(){return apples;} public long berries(){return berries;} public long chickens(){return chickens;}
    public long consumedUnits(){return apples+berries+chickens;}
    public long gainedSugar(){return apples*APPLE_SUGAR+berries*BERRY_SUGAR;}
    public long gainedProtein(){return chickens*CHICKEN_PROTEIN;}
    public long spentSugar(){return spentSugar;} public long spentProtein(){return spentProtein;}
    public static long sugarYield(ItemStack s){return s.is(Items.APPLE)?APPLE_SUGAR:s.is(Items.SWEET_BERRIES)?BERRY_SUGAR:0;}
    public static long proteinYield(ItemStack s){return s.is(Items.CHICKEN)?CHICKEN_PROTEIN:0;}
    public boolean accepts(ItemStack s,long sugarCap,long proteinCap) {
        return WorkerTasks.food(s)&&s.getCount()==1&&sugar+sugarYield(s)<=sugarCap&&protein+proteinYield(s)<=proteinCap;
    }
    /** Only a revalidated physical feeding action calls this, then clears the caregiver's equipment. */
    public boolean ingest(ItemStack s,long sugarCap,long proteinCap) {
        if(!accepts(s,sugarCap,proteinCap))return false;
        sugar+=sugarYield(s);protein+=proteinYield(s);
        if(s.is(Items.APPLE))apples++;else if(s.is(Items.SWEET_BERRIES))berries++;else chickens++;
        return true;
    }
    public boolean spend(long s,long p){if(s<0||p<0||s>sugar||p>protein)return false;sugar-=s;protein-=p;spentSugar+=s;spentProtein+=p;return true;}
    public void save(ValueOutput o){o.putLong("Sugar",sugar);o.putLong("Protein",protein);o.putLong("Apples",apples);o.putLong("Berries",berries);o.putLong("Chickens",chickens);o.putLong("SpentSugar",spentSugar);o.putLong("SpentProtein",spentProtein);}
    public void load(ValueInput i,long sCap,long pCap){
        sugar=i.getLongOr("Sugar",0);protein=i.getLongOr("Protein",0);apples=i.getLongOr("Apples",0);berries=i.getLongOr("Berries",0);chickens=i.getLongOr("Chickens",0);spentSugar=i.getLongOr("SpentSugar",0);spentProtein=i.getLongOr("SpentProtein",0);
        if(sugar<0||protein<0||sugar>sCap||protein>pCap||apples<0||berries<0||chickens<0||spentSugar<0||spentProtein<0||apples>Long.MAX_VALUE/APPLE_SUGAR/4||berries>Long.MAX_VALUE/BERRY_SUGAR/4||chickens>Long.MAX_VALUE/CHICKEN_PROTEIN/4||sugar+spentSugar!=gainedSugar()||protein+spentProtein!=gainedProtein())throw new IllegalArgumentException("Invalid finite ingested nutrition");
    }
}
