package fr.luuuccasss.discrimination;

import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerType;

/** Le préjugé d'un villageois, tiré au hasard à sa première rencontre avec un joueur et gardé pour toujours. */
public enum Prejudice {
    /** Méprise les pauvres, déroule le tapis rouge aux riches. */
    SNOB("snob", Formatting.LIGHT_PURPLE, 25),
    /** Déteste les riches. */
    ENVIEUX("envieux", Formatting.GREEN, 20),
    /** Se méfie de tout ce qui ne vient pas de son biome. */
    CHAUVIN("chauvin", Formatting.BLUE, 35),
    /** Aucun préjugé. */
    TOLERANT("tolérant", Formatting.WHITE, 20);

    public final String label;
    public final Formatting color;
    private final int weight;

    Prejudice(String label, Formatting color, int weight) {
        this.label = label;
        this.color = color;
        this.weight = weight;
    }

    public static Prejudice roll(Random random) {
        int total = 0;
        for (Prejudice p : values()) total += p.weight;
        int n = random.nextInt(total);
        for (Prejudice p : values()) {
            n -= p.weight;
            if (n < 0) return p;
        }
        return TOLERANT;
    }

    /**
     * Ce que ce préjugé pense d'un joueur, avant que l'habitude ne l'adoucisse.
     *
     * @param villagerType le biome du villageois
     * @param origin       le biome d'origine du joueur
     */
    public Verdict judge(SocialClass socialClass, VillagerType villagerType, VillagerType origin) {
        boolean foreigner = origin != null && origin != villagerType;
        return switch (this) {
            case SNOB -> switch (socialClass) {
                case MISEREUX -> Verdict.refuse("« Je ne commerce pas avec les va-nu-pieds. »");
                case MODESTE -> new Verdict(0.5, true, false, "« Ces articles-là ne sont pas pour les gens comme vous. »");
                case AISE -> Verdict.NEUTRAL;
                case RICHE -> new Verdict(-0.2, false, false, "« Quel honneur ! Prix d'ami, bien sûr. »");
            };
            case ENVIEUX -> switch (socialClass) {
                case MISEREUX -> new Verdict(-0.2, false, false, "« Toi au moins t'es pas un bourgeois. »");
                case MODESTE -> Verdict.NEUTRAL;
                case AISE -> new Verdict(0.3, false, false, "« Monsieur peut payer, hein. »");
                case RICHE -> new Verdict(0.8, true, false, "« Avec ton armure en diamant, tu peux bien payer le double. »");
            };
            case CHAUVIN -> {
                if (origin == null) yield Verdict.NEUTRAL;
                if (!foreigner) yield new Verdict(-0.2, false, false, "« Ah, un gars du pays ! »");
                if (socialClass == SocialClass.MISEREUX) {
                    yield Verdict.refuse("« Retourne " + Origin.home(origin) + ". »");
                }
                yield new Verdict(0.4, false, false, "« Un étranger " + Origin.from(origin) + "… Ce sera plus cher. »");
            }
            case TOLERANT -> Verdict.NEUTRAL;
        };
    }

    /**
     * @param malus    majoration des prix (négatif : remise)
     * @param lockBest les meilleurs échanges deviennent hors de prix
     * @param refuse   refuse tout échange
     * @param remark   ce qu'il dit au joueur
     */
    public record Verdict(double malus, boolean lockBest, boolean refuse, String remark) {
        public static final Verdict NEUTRAL = new Verdict(0, false, false, null);

        static Verdict refuse(String remark) {
            return new Verdict(0, false, true, remark);
        }

        public boolean hostile() {
            return refuse || lockBest || malus > 0;
        }
    }
}
