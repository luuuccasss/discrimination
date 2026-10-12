package fr.luuuccasss.discrimination;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** Ce qu'un villageois pense d'un joueur : son préjugé, adouci par l'habitude. */
public final class Opinion {
    /** Échanges ou cadeaux nécessaires pour que le préjugé disparaisse complètement. */
    public static final int ACCEPTED_AT = 10;
    /** En dessous, un préjugé qui refuse continue de refuser. */
    private static final int REFUSE_UNTIL = 3;
    /** En dessous, les meilleurs échanges restent hors de prix. */
    private static final int LOCK_UNTIL = 6;

    private Opinion() {
    }

    public static Prejudice prejudice(VillagerEntity villager) {
        Prejudiced prejudiced = (Prejudiced) villager;
        if (prejudiced.discrimination$getPrejudice() == null) {
            prejudiced.discrimination$setPrejudice(Prejudice.roll(villager.getRandom()));
        }
        return prejudiced.discrimination$getPrejudice();
    }

    public static Prejudice.Verdict of(VillagerEntity villager, ServerPlayerEntity player) {
        Prejudice prejudice = prejudice(villager);
        Prejudice.Verdict verdict = prejudice.judge(SocialClass.of(player), villager.getVillagerData().getType(),
            ((Profiled) player).discrimination$getOrigin());
        if (!verdict.hostile()) return verdict;
        int familiarity = ((Prejudiced) villager).discrimination$getFamiliarity(player.getUuid());
        if (familiarity >= ACCEPTED_AT) return Prejudice.Verdict.NEUTRAL;
        double softened = verdict.malus() > 0 ? verdict.malus() * (1 - familiarity / (double) ACCEPTED_AT) : verdict.malus();
        boolean refuse = verdict.refuse() && familiarity < REFUSE_UNTIL;
        boolean lock = (verdict.lockBest() || verdict.refuse()) && familiarity < LOCK_UNTIL;
        return new Prejudice.Verdict(softened, lock, refuse, verdict.remark());
    }
}
