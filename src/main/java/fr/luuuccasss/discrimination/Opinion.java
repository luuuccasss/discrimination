package fr.luuuccasss.discrimination;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

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

    public static Prejudice prejudice(Villager villager) {
        Prejudiced prejudiced = (Prejudiced) villager;
        if (prejudiced.discrimination$getPrejudice() == null) {
            prejudiced.discrimination$setPrejudice(Prejudice.roll(villager.getRandom()));
        }
        return prejudiced.discrimination$getPrejudice();
    }

    public static Prejudice.Verdict of(Villager villager, ServerPlayer player) {
        Prejudice prejudice = prejudice(villager);
        Prejudice.Verdict verdict = prejudice.judge(SocialClass.of(player), villager.getVillagerData().type().unwrapKey().orElse(null),
            ((Profiled) player).discrimination$getOrigin());
        if (!verdict.hostile()) return verdict;
        int familiarity = ((Prejudiced) villager).discrimination$getFamiliarity(player.getUUID());
        if (familiarity >= ACCEPTED_AT) return Prejudice.Verdict.NEUTRAL;
        double softened = verdict.malus() > 0 ? verdict.malus() * (1 - familiarity / (double) ACCEPTED_AT) : verdict.malus();
        boolean refuse = verdict.refuse() && familiarity < REFUSE_UNTIL;
        boolean lock = (verdict.lockBest() || verdict.refuse()) && familiarity < LOCK_UNTIL;
        return new Prejudice.Verdict(softened, lock, refuse, verdict.remark());
    }
}
