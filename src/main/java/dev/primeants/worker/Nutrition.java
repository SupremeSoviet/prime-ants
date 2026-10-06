package dev.primeants.worker;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Recipient-owned ingested stores. Receipts describe terminal consumption and cannot be spent as food. */
public final class Nutrition {
    public static final long APPLE_SUGAR=4000, BERRY_SUGAR=2000, CHICKEN_PROTEIN=8000;
    public static final long NECTAR_SUGAR=1000, NECTAR_V2_SUGAR=4000, PREY_PROTEIN=8000, FLESH_PROTEIN=4000;
    public static final long QUEEN_SUGAR_CAPACITY=8000, QUEEN_PROTEIN_CAPACITY=16000;
    public static final long EGG_SUGAR=1000, EGG_PROTEIN=2000;
    public static final long LARVA_SUGAR=4000, LARVA_PROTEIN=8000;
    private long sugar,protein,apples,berries,chickens,nectar,nectarV2,prey,flesh,spentSugar,spentProtein,receivedSugar,givenSugar;
    public long receivedSugar(){return receivedSugar;} public long givenSugar(){return givenSugar;}
    /** Only the controller's revalidated mouth action calls this. No ingestion or income receipt. */
    boolean shareWith(Nutrition recipient,long amount,long reserve,long capacity){
        if(recipient==this||amount<=0||reserve<0||sugar-reserve<amount||recipient.sugar>capacity-amount
            ||givenSugar>Long.MAX_VALUE-amount||recipient.receivedSugar>Long.MAX_VALUE-amount)return false;
        sugar-=amount;givenSugar+=amount;recipient.sugar+=amount;recipient.receivedSugar+=amount;return true;
    }
    public long sugar(){return sugar;} public long protein(){return protein;}
    public long apples(){return apples;} public long berries(){return berries;} public long chickens(){return chickens;}
    public long nectar(){return nectar+nectarV2;} public long nectarV2(){return nectarV2;} public long prey(){return prey;} public long flesh(){return flesh;}
    public long consumedUnits(){return apples+berries+chickens+nectar+nectarV2+prey+flesh;}
    public long gainedSugar(){return apples*APPLE_SUGAR+berries*BERRY_SUGAR+nectar*NECTAR_SUGAR+nectarV2*NECTAR_V2_SUGAR;}
    public long gainedProtein(){return chickens*CHICKEN_PROTEIN+flesh*FLESH_PROTEIN+prey*PREY_PROTEIN;}
    public long spentSugar(){return spentSugar;} public long spentProtein(){return spentProtein;}
    public static long sugarYield(ItemStack s){return s.is(Items.APPLE)?APPLE_SUGAR:s.is(Items.SWEET_BERRIES)?BERRY_SUGAR:s.is(dev.primeants.item.AntItems.FLOWER_NECTAR)?NECTAR_SUGAR:s.is(dev.primeants.item.AntItems.FLOWER_NECTAR_V2)?NECTAR_V2_SUGAR:0;}
    public static long proteinYield(ItemStack s){return s.is(Items.CHICKEN)?CHICKEN_PROTEIN:s.is(Items.ROTTEN_FLESH)?FLESH_PROTEIN:s.is(dev.primeants.item.AntItems.SMALL_PREY)?PREY_PROTEIN:0;}
    public boolean accepts(ItemStack s,long sugarCap,long proteinCap) {
        return WorkerTasks.food(s)&&s.getCount()==1&&sugar+sugarYield(s)<=sugarCap&&protein+proteinYield(s)<=proteinCap;
    }
    /** Only a revalidated physical feeding action calls this, then clears the caregiver's equipment. */
    public boolean ingest(ItemStack s,long sugarCap,long proteinCap) {
        if(!accepts(s,sugarCap,proteinCap))return false;
        sugar+=sugarYield(s);protein+=proteinYield(s);
        if(s.is(Items.APPLE))apples++;else if(s.is(Items.SWEET_BERRIES))berries++;else if(s.is(Items.CHICKEN))chickens++;else if(s.is(dev.primeants.item.AntItems.FLOWER_NECTAR))nectar++;else if(s.is(dev.primeants.item.AntItems.FLOWER_NECTAR_V2))nectarV2++;else if(s.is(dev.primeants.item.AntItems.SMALL_PREY))prey++;else if(s.is(Items.ROTTEN_FLESH))flesh++;else throw new IllegalArgumentException("Unaccounted food");
        return true;
    }
    public boolean spend(long s,long p){if(s<0||p<0||s>sugar||p>protein)return false;sugar-=s;protein-=p;spentSugar+=s;spentProtein+=p;return true;}
    public void save(ValueOutput o){o.putLong("Sugar",sugar);o.putLong("Protein",protein);o.putLong("Apples",apples);o.putLong("Berries",berries);o.putLong("Chickens",chickens);o.putLong("Nectar",nectar);o.putLong("NectarV2",nectarV2);o.putLong("Prey",prey);o.putLong("Flesh",flesh);o.putLong("SpentSugar",spentSugar);o.putLong("SpentProtein",spentProtein);o.putLong("SugarReceived",receivedSugar);o.putLong("SugarGiven",givenSugar);}
    public void load(ValueInput i,long sCap,long pCap){
        sugar=i.getLongOr("Sugar",0);protein=i.getLongOr("Protein",0);apples=i.getLongOr("Apples",0);berries=i.getLongOr("Berries",0);chickens=i.getLongOr("Chickens",0);spentSugar=i.getLongOr("SpentSugar",0);spentProtein=i.getLongOr("SpentProtein",0);
        flesh=i.getLongOr("Flesh",0);if(flesh<0||flesh>Long.MAX_VALUE/FLESH_PROTEIN/4)throw new IllegalArgumentException("Invalid flesh receipts");
        nectarV2=i.getLongOr("NectarV2",0);prey=i.getLongOr("Prey",0);if(nectarV2<0||prey<0||nectarV2>Long.MAX_VALUE/NECTAR_V2_SUGAR/8||prey>Long.MAX_VALUE/PREY_PROTEIN/8)throw new IllegalArgumentException("Invalid versioned food receipts");
        nectar=i.getLongOr("Nectar",0);if(nectar<0||nectar>Long.MAX_VALUE/NECTAR_SUGAR/4)throw new IllegalArgumentException("Invalid nectar receipts");
        receivedSugar=i.getLongOr("SugarReceived",0);givenSugar=i.getLongOr("SugarGiven",0);
        if(sugar<0||protein<0||sugar>sCap||protein>pCap||apples<0||berries<0||chickens<0||spentSugar<0||spentProtein<0||receivedSugar<0||givenSugar<0||apples>Long.MAX_VALUE/APPLE_SUGAR/4||berries>Long.MAX_VALUE/BERRY_SUGAR/4||chickens>Long.MAX_VALUE/CHICKEN_PROTEIN/4)throw new IllegalArgumentException("Invalid finite ingested nutrition");
        try{if(Math.addExact(sugar,Math.addExact(spentSugar,givenSugar))!=Math.addExact(gainedSugar(),receivedSugar)||Math.addExact(protein,spentProtein)!=gainedProtein())throw new IllegalArgumentException("Unbalanced ingested nutrition");}catch(ArithmeticException e){throw new IllegalArgumentException("Overflowed nutrition",e);}
    }
}
