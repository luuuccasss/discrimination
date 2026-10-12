package fr.luuuccasss.discrimination;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;

/** Ce qui se passe quand un joueur traverse un village : regards, messes basses, et le golem qui le suit. */
public final class Gossip {
    private static final int VILLAGER_RADIUS = 7;
    private static final int GOLEM_RADIUS = 20;
    private static final long COMMENT_DELAY = 140;
    private static final long GOLEM_MESSAGE_DELAY = 20 * 45;

    private static final Map<UUID, Long> LAST_COMMENT = new HashMap<>();
    private static final Map<UUID, Long> LAST_GOLEM = new HashMap<>();

    private static final Map<Prejudice, String[]> MUTTERINGS = Map.of(
        Prejudice.SNOB, new String[]{"Un villageois te toise de haut en bas.", "« Mais qui a laissé entrer ce clochard ? »",
            "Un villageois serre sa bourse contre lui en te croisant."},
        Prejudice.ENVIEUX, new String[]{"« Regardez-moi ce bourgeois. »", "Un villageois fixe ton armure d'un œil mauvais.",
            "« Il se croit tout permis avec son diamant. »"},
        Prejudice.CHAUVIN, new String[]{"« Encore un étranger… »", "Deux villageois chuchotent en te regardant.",
            "« On est plus chez nous. »", "Un villageois ferme ses volets sur ton passage."});

    private Gossip() {
    }

    /** Appelé toutes les 10 ticks pour chaque joueur. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        if (player.isSpectator() || player.isInvisible()) return;
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(VILLAGER_RADIUS),
            v -> v.isAlive() && !v.isBaby() && !v.isSleeping() && !v.isTrading());
        RandomSource random = level.getRandom();
        for (Villager villager : villagers) {
            Prejudice.Verdict verdict = Opinion.of(villager, player);
            if (verdict.hostile()) {
                villager.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true), 60);
                if (random.nextInt(8) == 0) {
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER, villager.getX(), villager.getEyeY() + 0.4, villager.getZ(), 1, 0, 0, 0, 0);
                }
                if (random.nextInt(6) == 0) comment(level, player, villager);
            } else if (verdict.malus() < 0 && random.nextInt(10) == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getEyeY() + 0.4, villager.getZ(), 3, 0.3, 0.2, 0.3, 0);
            }
        }
        golemCheck(level, player);
    }

    private static void comment(ServerLevel level, ServerPlayer player, Villager villager) {
        long now = level.getGameTime();
        Long last = LAST_COMMENT.get(player.getUUID());
        if (last != null && now - last < COMMENT_DELAY) return;
        String[] lines = MUTTERINGS.get(Opinion.prejudice(villager));
        if (lines == null) return;
        LAST_COMMENT.put(player.getUUID(), now);
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1f, 0.8f);
        player.sendSystemMessage(Component.literal(lines[level.getRandom().nextInt(lines.length)])
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    /** Le villageois adulte le plus proche, ou null. */
    public static Villager closestVillager(ServerLevel level, ServerPlayer player, double radius) {
        return level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(radius), v -> v.isAlive() && !v.isBaby())
            .stream().min(Comparator.comparingDouble(v -> v.distanceToSqr(player))).orElse(null);
    }

    /**
     * Les golems d'un village suivent de près les étrangers à ce village, comme pour un contrôle d'identité.
     * Les joueurs qui y sont habitués (assez d'échanges avec les villageois) sont laissés tranquilles.
     */
    private static void golemCheck(ServerLevel level, ServerPlayer player) {
        ResourceKey<VillagerType> origin = ((Profiled) player).discrimination$getOrigin();
        if (origin == null) return;
        Villager local = closestVillager(level, player, 32);
        if (local == null || origin.equals(local.getVillagerData().type().unwrapKey().orElse(null))) return;
        if (((Prejudiced) local).discrimination$getFamiliarity(player.getUUID()) >= Opinion.ACCEPTED_AT) return;
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(GOLEM_RADIUS),
            g -> g.isAlive() && !g.isPlayerCreated() && g.getTarget() == null)) {
            golem.getLookControl().setLookAt(player);
            if (golem.distanceToSqr(player) > 9) golem.getNavigation().moveTo(player, 0.7);
            else golem.getNavigation().stop();
            long now = level.getGameTime();
            Long last = LAST_GOLEM.get(player.getUUID());
            if (last == null || now - last > GOLEM_MESSAGE_DELAY) {
                LAST_GOLEM.put(player.getUUID(), now);
                player.sendSystemMessage(Component.literal("Le golem du village te suit de près. Contrôle d'identité.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    public static void forget(ServerPlayer player) {
        LAST_COMMENT.remove(player.getUUID());
        LAST_GOLEM.remove(player.getUUID());
    }
}
