package fr.luuuccasss.discrimination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerType;

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
    public static void tick(ServerWorld world, ServerPlayerEntity player) {
        if (player.isSpectator() || player.isInvisible()) return;
        List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, player.getBoundingBox().expand(VILLAGER_RADIUS),
            v -> v.isAlive() && !v.isBaby() && !v.isSleeping() && !v.hasCustomer());
        Random random = world.random;
        for (VillagerEntity villager : villagers) {
            Prejudice.Verdict verdict = Opinion.of(villager, player);
            if (verdict.hostile()) {
                villager.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(player, true), 60);
                if (random.nextInt(8) == 0) {
                    world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, villager.getX(), villager.getEyeY() + 0.4, villager.getZ(), 1, 0, 0, 0, 0);
                }
                if (random.nextInt(6) == 0) comment(world, player, villager);
            } else if (verdict.malus() < 0 && random.nextInt(10) == 0) {
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getEyeY() + 0.4, villager.getZ(), 3, 0.3, 0.2, 0.3, 0);
            }
        }
        golemCheck(world, player);
    }

    private static void comment(ServerWorld world, ServerPlayerEntity player, VillagerEntity villager) {
        long now = world.getTime();
        Long last = LAST_COMMENT.get(player.getUuid());
        if (last != null && now - last < COMMENT_DELAY) return;
        String[] lines = MUTTERINGS.get(Opinion.prejudice(villager));
        if (lines == null) return;
        LAST_COMMENT.put(player.getUuid(), now);
        world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundCategory.NEUTRAL, 1f, 0.8f);
        player.sendMessage(Text.literal(lines[world.random.nextInt(lines.length)]).formatted(Formatting.GRAY, Formatting.ITALIC), true);
    }

    /**
     * Les golems d'un village suivent de près les étrangers à ce village, comme pour un contrôle d'identité.
     * Les joueurs qui y sont habitués (assez d'échanges avec les villageois) sont laissés tranquilles.
     */
    private static void golemCheck(ServerWorld world, ServerPlayerEntity player) {
        VillagerType origin = ((Profiled) player).discrimination$getOrigin();
        if (origin == null) return;
        VillagerEntity local = world.getClosestEntity(VillagerEntity.class, net.minecraft.entity.ai.TargetPredicate.DEFAULT, player,
            player.getX(), player.getY(), player.getZ(), player.getBoundingBox().expand(32));
        if (local == null || local.getVillagerData().getType() == origin) return;
        if (((Prejudiced) local).discrimination$getFamiliarity(player.getUuid()) >= Opinion.ACCEPTED_AT) return;
        for (IronGolemEntity golem : world.getEntitiesByClass(IronGolemEntity.class, player.getBoundingBox().expand(GOLEM_RADIUS),
            g -> g.isAlive() && !g.isPlayerCreated() && g.getTarget() == null)) {
            golem.getLookControl().lookAt(player);
            if (golem.squaredDistanceTo(player) > 9) golem.getNavigation().startMovingTo(player, 0.7);
            else golem.getNavigation().stop();
            long now = world.getTime();
            Long last = LAST_GOLEM.get(player.getUuid());
            if (last == null || now - last > GOLEM_MESSAGE_DELAY) {
                LAST_GOLEM.put(player.getUuid(), now);
                player.sendMessage(Text.literal("Le golem du village te suit de près. Contrôle d'identité.")
                    .formatted(Formatting.GRAY, Formatting.ITALIC), false);
            }
        }
    }

    public static void forget(ServerPlayerEntity player) {
        LAST_COMMENT.remove(player.getUuid());
        LAST_GOLEM.remove(player.getUuid());
    }
}
